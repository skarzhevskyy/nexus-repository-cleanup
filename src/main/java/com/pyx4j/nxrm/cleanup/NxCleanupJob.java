package com.pyx4j.nxrm.cleanup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

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

    NxCleanupCommandArgs args;

    private final ApiClient apiClient;

    private final Predicate<ComponentXO> componentFilter;

    private final RepositoryComponentsSummary repositoryComponentsSummary;

    private final GroupsSummary groupsSummary;

    private final List<ComponentXO> allFilteredComponents = new ArrayList<>();

    public NxCleanupJob(NxCleanupCommandArgs args) {
        // Create our summary objects based on report type
        repositoryComponentsSummary = new RepositoryComponentsSummary();
        repositoryComponentsSummary.setEnabled(args.reportRepositoriesSummary);
        groupsSummary = new GroupsSummary();
        groupsSummary.setEnabled(args.reportTopGroups);

        // Create component filter based on command line arguments
        componentFilter = ComponentFilter.createFilter(args);
        this.args = args;

        apiClient = createApiClient(args);
    }

    private static ApiClient createApiClient(NxCleanupCommandArgs args) {
        Objects.requireNonNull(args, "Command arguments cannot be null");
        Objects.requireNonNull(args.nexusServerUrl, "Nexus server URL cannot be null");

        log.info("Initializing report generation for Nexus server: {}", args.nexusServerUrl);

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
                .filter(repository -> ComponentFilter.matchesRepositoryFilter(repository.getName(), args.repositories)) // Filter repositories early
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
        return processPaginatedComponents(componentsApi, repository, null);
    }

    private Mono<Void> processPaginatedComponents(ComponentsApi componentsApi, AbstractApiRepository repository, String continuationToken) {
        final String repoName = Objects.requireNonNull(repository.getName(), "Repository name cannot be null");
        log.debug("Fetching components page for repository {} with token: {}", repoName, continuationToken);

        return componentsApi.getComponents(repoName, continuationToken)
                .flatMap(page -> {
                    if (page != null && page.getItems() != null) {
                        // Apply filter to components
                        List<ComponentXO> filteredComponents = page.getItems().stream()
                                .filter(componentFilter)
                                .toList();

                        log.debug("Repository {} page has {} components (filtered from {}) for processing",
                                repoName, filteredComponents.size(), page.getItems().size());

                        // Process each filtered component
                        return processFilteredComponents(componentsApi, repository, filteredComponents)
                                .then(Mono.defer(() -> {
                                    // If we have a continuation token, process next page
                                    String nextContinuationToken = page.getContinuationToken();
                                    if (nextContinuationToken != null && !nextContinuationToken.isEmpty()) {
                                        return processPaginatedComponents(componentsApi, repository, nextContinuationToken);
                                    }
                                    return Mono.empty();
                                }));
                    } else {
                        log.debug("Repository {} page has no components", repoName);
                    }

                    return Mono.empty();
                });
    }

    private Mono<Void> processFilteredComponents(ComponentsApi componentsApi, AbstractApiRepository repository, List<ComponentXO> filteredComponents) {
        if (filteredComponents.isEmpty()) {
            return Mono.empty();
        }

        final String repoName = repository.getName();
        long totalSize = calculateTotalSize(filteredComponents);

        log.trace("Processing {} filtered components in repository {} with total size of {} bytes",
                filteredComponents.size(), repoName, totalSize);

        if (args.dryRun) {
            log.debug("DRY RUN: Would delete {} components from repository {}", filteredComponents.size(), repoName);
            // In dry run mode, add all components to reports
            filteredComponents.forEach(component -> addToReports(component, repository));
            return Mono.empty();
        } else {
            // Delete components single-threaded execution and add to reports only if deletion is successful
            return Flux.fromIterable(filteredComponents)
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
                    addToReports(component, repository);
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

    private void addToReports(ComponentXO component, AbstractApiRepository repository) {
        Objects.requireNonNull(component, "Component cannot be null");
        Objects.requireNonNull(repository, "Repository cannot be null");

        // Add component to the global list for component reporting
        allFilteredComponents.add(component);

        final String repoName = repository.getName();
        final String repoFormat = repository.getFormat();
        final long componentSize = calculateComponentSize(component);

        log.trace("Adding component {} to reports for repository {}", component.getId(), repoName);

        // Update repository summary if enabled
        if (repositoryComponentsSummary.isEnabled()) {
            repositoryComponentsSummary.addRepositoryStats(repoName, repoFormat, 1, componentSize);
        }

        // Update groups summary if enabled and component has a group
        if (groupsSummary.isEnabled() && component.getGroup() != null) {
            String groupName = component.getGroup();
            groupsSummary.addGroupStats(groupName, 1, componentSize);
        }
    }

    private void writeReports() throws IOException {
        try (ReportWriter reportWriter = ReportWriterFactory.create(args.reportOutputFile);
             ReportWriter componentWriter = ReportWriterFactory.create(args.outputComponentFile)) {

            if (reportWriter != null) {
                if (repositoryComponentsSummary.isEnabled()) {
                    reportWriter.writeRepositoryComponentsSummary(repositoryComponentsSummary, args.repositoriesSortBy);
                }
                if (groupsSummary.isEnabled()) {
                    reportWriter.writeGroupsSummary(groupsSummary, args.groupSort, args.topGroups);
                }
            } else {
                boolean hasPreviousOutput = false;
                if (repositoryComponentsSummary.isEnabled()) {
                    NxReportConsole.printSummary(repositoryComponentsSummary, args.repositoriesSortBy);
                    hasPreviousOutput = true;
                }
                if (groupsSummary.isEnabled()) {
                    if (hasPreviousOutput) {
                        System.out.println(); // Add blank line between reports
                    }
                    NxReportConsole.printGroupsSummary(groupsSummary, args.groupSort, args.topGroups);
                    hasPreviousOutput = true;
                }
            }

            if (componentWriter != null) {
                componentWriter.writeComponents(allFilteredComponents);
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
