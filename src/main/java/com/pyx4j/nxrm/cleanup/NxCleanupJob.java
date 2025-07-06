package com.pyx4j.nxrm.cleanup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.pyx4j.nxrm.cleanup.model.CleanupRuleSet;
import com.pyx4j.nxrm.cleanup.model.GroupsSummary;
import com.pyx4j.nxrm.cleanup.model.RepositoryComponentsSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.ApiClient;
import org.sonatype.nexus.api.ComponentsApi;
import org.sonatype.nexus.api.RepositoryManagementApi;
import org.sonatype.nexus.model.AbstractApiRepository;
import org.sonatype.nexus.model.ComponentXO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class NxCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(NxCleanupJob.class);

    private final NxCleanupCommandArgs args;

    private final ApiClient apiClient;

    private final ComponentFilter componentFilter;

    private final RepositoryComponentsSummary repositoryComponentsSummary;

    private final GroupsSummary groupsSummary;

    private final ReportWriter componentWriter;

    public NxCleanupJob(NxCleanupCommandArgs args) {
        // Create our summary objects based on report type
        repositoryComponentsSummary = new RepositoryComponentsSummary();
        repositoryComponentsSummary.setEnabled(args.reportRepositoriesSummary);
        groupsSummary = new GroupsSummary();
        groupsSummary.setEnabled(args.reportTopGroups);

        this.args = args;

        apiClient = createApiClient(args);

        CleanupRuleSet ruleSet;
        try {
            ruleSet = new CleanupRuleParser().parseFromFile(Path.of(args.rulesFile));
        } catch (IOException e) {
            log.error("Failed to parse cleanup rules from file: {}", args.rulesFile, e);
            throw new IllegalArgumentException("Invalid cleanup rules file: " + args.rulesFile, e);
        }
        // Create component filter based on rules
        componentFilter = new ComponentFilter(ruleSet);
        try {
            componentWriter = ReportWriterFactory.create(args.outputComponentFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ApiClient createApiClient(NxCleanupCommandArgs args) {
        Objects.requireNonNull(args, "Command arguments cannot be null");
        Objects.requireNonNull(args.nexusServerUrl, "Nexus server URL cannot be null");

        log.debug("Initializing scan of nexus server: {}", args.nexusServerUrl);

        if (args.dryRun) {
            System.out.printf("Nexus Repository Cleanup Job (Dry Run) starting — Scanning server: %s (no deletions will be performed)%n", args.nexusServerUrl);
        } else {
            System.out.printf("Nexus Repository Cleanup Job starting — Scanning server: %s%n", args.nexusServerUrl);
        }

        // There is no authentication configured in swagger, so apiClient.setUsername(args.nexusUsername) can't be used here
        String authorizationHeader;
        if (args.nexusToken != null && !args.nexusToken.isEmpty()) {
            authorizationHeader = "Bearer " + args.nexusToken;
        } else {
            authorizationHeader = "Basic " + Base64.getEncoder().encodeToString((args.nexusUsername + ":" + args.nexusPassword).getBytes(StandardCharsets.UTF_8));
        }

        // Configure proxy settings
        ProxySelector.ProxyConfig proxyConfig = ProxySelector.selectProxy(args.nexusServerUrl, args.proxyUrl);

        WebClient webClient = ProxySelector.configureProxy(WebClient.builder(), proxyConfig)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();

        // Initialize API clients
        ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(args.nexusServerUrl + "/service/rest");

        return apiClient;
    }

    public int execute() {

        // Use CountDownLatch to control flow in the main thread
        AtomicInteger resultCode = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        RepositoryManagementApi repoApi = new RepositoryManagementApi(apiClient);

        // Build the reactive pipeline
        repoApi.getRepositories()
                .doOnNext(repository -> log.debug("Found {} repository of type {}", repository.getName(), repository.getType()))
                .filter(repository -> !repository.getType().equals(AbstractApiRepository.TypeEnum.GROUP)) // Exclude group repositories
                .filter(repository -> componentFilter.matchesRepositoryFilter(repository.getName())) // Filter repositories early
                .doOnNext(repository -> log.trace("Processing repository: {}", repository.getName()))
                .flatMap(repository -> processRepositoryComponents(repository))
                .collectList()
                .doOnSuccess(allRepos -> {
                    try {
                        writeReports();
                        resultCode.set(0);
                    } catch (IOException e) {
                        log.error("Error writing report file", e);
                        resultCode.set(1);
                    }
                })
                .doOnError(ex -> {
                    log.error("Cleanup error", ex);
                    resultCode.set(1);
                })
                .doFinally(signal -> latch.countDown())
                .subscribe();

        // Wait for completion
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("Cleanup interrupted", e);
            Thread.currentThread().interrupt();
            return 1;
        }

        return resultCode.get();
    }

    private Mono<Void> processRepositoryComponents(AbstractApiRepository repository) {
        ComponentsApi componentsApi = new ComponentsApi(apiClient);
        final String repoName = Objects.requireNonNull(repository.getName(), "Repository name cannot be null");

        return Mono.just("")  // Start with empty string to trigger initial fetch
                .expand(token -> {
                    // Convert empty string to null for the API call
                    String actualToken = token.isEmpty() ? null : token;
                    log.debug("Fetching components page for repository {} with token: {}", repoName, actualToken);

                    return componentsApi.getComponents(repoName, actualToken)
                            .flatMap(page -> {
                                if (page != null && page.getItems() != null) {
                                    List<ComponentXO> allComponents = page.getItems();
                                    // Apply filter to components
                                    List<ComponentXO> filteredComponents = allComponents.stream()
                                            .filter(componentFilter.getComponentFilter())
                                            .toList();

                                    log.debug("Repository {} page has {} components (filtered from {}) for processing",
                                            repoName, filteredComponents.size(), allComponents.size());

                                    // Process filtered components for this page
                                    return processFilteredComponents(componentsApi, repository, allComponents, filteredComponents)
                                            .then(Mono.fromCallable(() -> {
                                                String nextToken = page.getContinuationToken();
                                                return (nextToken != null && !nextToken.isEmpty()) ? nextToken : null;
                                            }))
                                            .cast(String.class);  // Ensure type consistency
                                } else {
                                    log.debug("Repository {} page has no components", repoName);
                                    return Mono.empty();
                                }
                            })
                            .onErrorResume(error -> {
                                log.warn("Error processing page for repository {} with token {}: {}",
                                        repoName, actualToken, error.getMessage());
                                return Mono.empty(); // Stop pagination on error
                            });
                })
                .then();
    }

    private Mono<Void> processFilteredComponents(ComponentsApi componentsApi, AbstractApiRepository repository, List<ComponentXO> allComponents, List<ComponentXO> filteredComponents) {
        if (allComponents.isEmpty()) {
            return Mono.empty();
        }

        final String repoName = repository.getName();

        List<ComponentXO> componentsToRemove = filteredComponents;
        List<ComponentXO> remainingComponents = allComponents.stream()
                .filter(c -> !componentsToRemove.contains(c))
                .toList();

        long removedSize = calculateTotalSize(componentsToRemove);
        long remainingSize = calculateTotalSize(remainingComponents);

        log.trace("Processing {} filtered components in repository {} with total size of {} bytes",
                componentsToRemove.size(), repoName, removedSize);

        addToReports(repository, componentsToRemove, remainingComponents);

        if (args.dryRun) {
            log.debug("DRY RUN: Would delete {} components from repository {}", componentsToRemove.size(), repoName);
            return Mono.empty();
        } else {
            // Delete components single-threaded execution and add to reports only if deletion is successful
            return Flux.fromIterable(componentsToRemove)
                    .concatMap(component -> deleteComponent(componentsApi, component, repository))
                    .then();
        }
    }

    private Mono<Void> deleteComponent(ComponentsApi componentsApi, ComponentXO component, AbstractApiRepository repository) {
        final String componentId = component.getId();
        final String repoName = repository.getName();

        log.trace("Attempting to delete component {} from repository {}", componentId, repoName);

        return componentsApi.deleteComponent(componentId)
                .doOnSuccess(unused -> {
                    log.debug("Successfully deleted component {} from repository {}", componentId, repoName);
                })
                .doOnError(error -> {
                    log.error("Failed to delete component {} from repository {}: {}",
                            componentId, repoName, error.getMessage());
                })
                .onErrorResume(error -> {
                    // Continue processing other components even if one fails
                    log.warn("Skipping component {} due to deletion error", componentId);
                    return Mono.empty();
                });
    }

    private void addToReports(AbstractApiRepository repository, List<ComponentXO> componentsToRemove, List<ComponentXO> remainingComponents) {
        Objects.requireNonNull(repository, "Repository cannot be null");

        final String repoName = repository.getName();
        final String repoFormat = repository.getFormat();

        long removedCount = componentsToRemove.size();
        long removedSize = calculateTotalSize(componentsToRemove);
        long remainingCount = remainingComponents.size();
        long remainingSize = calculateTotalSize(remainingComponents);

        log.trace("Adding to reports for repository {}: {} removed, {} remaining", repoName, removedCount, remainingCount);

        // Update repository summary if enabled
        if (repositoryComponentsSummary.isEnabled()) {
            repositoryComponentsSummary.addRepositoryStats(repoName, repoFormat, removedCount, removedSize, remainingCount, remainingSize);
        }

        // Update groups summary if enabled
        if (groupsSummary.isEnabled()) {
            // Group components by their group name
            var removedByGroup = componentsToRemove.stream().filter(c -> c.getGroup() != null).collect(Collectors.groupingBy(ComponentXO::getGroup));
            var remainingByGroup = remainingComponents.stream().filter(c -> c.getGroup() != null).collect(Collectors.groupingBy(ComponentXO::getGroup));
            var allGroups = Stream.concat(removedByGroup.keySet().stream(), remainingByGroup.keySet().stream()).collect(Collectors.toSet());

            for (String groupName : allGroups) {
                List<ComponentXO> removedInGroup = removedByGroup.getOrDefault(groupName, Collections.emptyList());
                List<ComponentXO> remainingInGroup = remainingByGroup.getOrDefault(groupName, Collections.emptyList());
                groupsSummary.addGroupStats(groupName,
                        removedInGroup.size(), calculateTotalSize(removedInGroup),
                        remainingInGroup.size(), calculateTotalSize(remainingInGroup));
            }
        }

        if (componentWriter != null) {
            try {
                for (ComponentXO component : componentsToRemove) {
                    componentWriter.writeComponent(component);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void writeReports() throws IOException {
        boolean hasPreviousOutput = false;
        if (repositoryComponentsSummary.isEnabled()) {
            NxReportConsole.printSummary(repositoryComponentsSummary, args.repositoriesSortBy, args.dryRun);
            hasPreviousOutput = true;
        }
        if (groupsSummary.isEnabled()) {
            if (hasPreviousOutput) {
                System.out.println(); // Add blank line between reports
            }
            NxReportConsole.printGroupsSummary(groupsSummary, args.groupSort, args.topGroups, args.dryRun);
        }

        try (ReportWriter reportWriter = ReportWriterFactory.create(args.reportOutputFile)) {
            if (reportWriter != null) {
                if (repositoryComponentsSummary.isEnabled()) {
                    reportWriter.writeRepositoryComponentsSummary(repositoryComponentsSummary, args.repositoriesSortBy);
                }
                if (groupsSummary.isEnabled()) {
                    reportWriter.writeGroupsSummary(groupsSummary, args.groupSort, args.topGroups);
                }
            }
        } finally {
            if (componentWriter != null) {
                componentWriter.close();
            }
        }
    }

    /**
     * Calculates the total size of all components in bytes.
     *
     * @param components List of components to calculate size for
     * @return Total size in bytes
     */
    private static long calculateTotalSize(List<ComponentXO> components) {
        if (components == null || components.isEmpty()) {
            return 0;
        }

        return components.stream()
                .mapToLong(NxCleanupJob::calculateComponentSize)
                .sum();
    }

    /**
     * Calculates the total size of a single component in bytes.
     *
     * @param component Component to calculate size for
     * @return Total size in bytes
     */
    private static long calculateComponentSize(ComponentXO component) {
        if (component == null || component.getAssets() == null) {
            return 0;
        }

        return component.getAssets().stream()
                .filter(asset -> asset.getFileSize() != null)
                .mapToLong(asset -> asset.getFileSize() != null ? asset.getFileSize() : 0)
                .sum();
    }

}
