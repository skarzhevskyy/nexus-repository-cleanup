package com.pyx4j.nxrm.cleanup;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Predicate;

import com.google.common.base.Strings;
import com.pyx4j.nxrm.cleanup.model.CleanupRule;
import com.pyx4j.nxrm.cleanup.model.CleanupRuleSet;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.sonatype.nexus.model.AssetXO;
import org.sonatype.nexus.model.ComponentXO;

/**
 * Utility class for creating component filters based on criteria defined in rules.
 */
final class ComponentFilter {

    private final Predicate<ComponentXO> componentFilter;
    private final List<String> repositoryPatterns;

    ComponentFilter(@NonNull CleanupRuleSet ruleSet) {
        this.repositoryPatterns = extractRepositoryPatterns(ruleSet);
        this.componentFilter = createFilter(ruleSet);
    }

    public Predicate<ComponentXO> getComponentFilter() {
        return componentFilter;
    }

    @NonNull
    private Predicate<ComponentXO> createFilter(@NonNull CleanupRuleSet ruleSet) {
        List<ParsedRule> enabledRules = parseRules(ruleSet);

        return component -> {
            if (component == null || component.getAssets() == null || component.getAssets().isEmpty()) {
                return false;
            }

            // Split rules by action
            List<ParsedRule> deleteRules = enabledRules.stream()
                    .filter(rule -> "delete".equals(rule.action))
                    .toList();
            List<ParsedRule> keepRules = enabledRules.stream()
                    .filter(rule -> "keep".equals(rule.action))
                    .toList();

            // Check if any keep rule matches - if so, component should not be deleted
            boolean matchesKeepRule = keepRules.stream()
                    .anyMatch(rule -> matchesRule(component, rule));
            if (matchesKeepRule) {
                return false;
            }

            // Check if any delete rule matches
            return deleteRules.stream()
                    .anyMatch(rule -> matchesRule(component, rule));
        };
    }

    /**
     * Parses the rules from a rule set, creating ParsedRule objects with precompiled patterns and dates.
     */
    @NonNull
    private List<ParsedRule> parseRules(@NonNull CleanupRuleSet ruleSet) {
        return ruleSet.getRules().stream()
                .filter(CleanupRule::isEnabled)
                .map(this::parseRule)
                .toList();
    }

    /**
     * Parses a single rule into a ParsedRule with precompiled patterns and dates.
     */
    @NonNull
    private ParsedRule parseRule(@NonNull CleanupRule rule) {
        CleanupRule.CleanupFilters filters = rule.getFilters();

        // Parse date filters
        OffsetDateTime updatedBefore = null;
        if (filters.getUpdated() != null) {
            updatedBefore = DateFilterParser.parseDate(filters.getUpdated());
        }

        OffsetDateTime downloadedBefore = null;
        boolean isNeverDownloaded = false;
        if (filters.getDownloaded() != null) {
            if (CleanupRuleParser.isNeverDownloaded(filters.getDownloaded())) {
                isNeverDownloaded = true;
            } else {
                downloadedBefore = CleanupRuleParser.parseDownloadedFilter(filters.getDownloaded());
            }
        }

        return new ParsedRule(
                rule.getName(),
                rule.getAction(),
                filters.getRepositories(),
                filters.getFormats(),
                filters.getGroups(),
                filters.getNames(),
                filters.getVersions(),
                updatedBefore,
                downloadedBefore,
                isNeverDownloaded
        );
    }

    /**
     * Checks if a repository name matches the provided repository patterns.
     *
     * @param repositoryName The repository name to test
     * @return true if the repository name matches any of the patterns, or if no patterns are provided
     */
    public boolean matchesRepositoryFilter(@Nullable String repositoryName) {
        if (repositoryPatterns == null || repositoryPatterns.isEmpty()) {
            return true; // No patterns means match all repositories
        }

        if (Strings.isNullOrEmpty(repositoryName)) {
            return false; // Cannot match patterns with null/empty repository name
        }

        return matchesAnyPattern(repositoryName, repositoryPatterns);
    }

    /**
     * Extracts all repository patterns from enabled rules in the rule set.
     *
     * @param ruleSet The cleanup rule set
     * @return List of repository patterns from all enabled rules, or empty list if none
     */
    @NonNull
    private List<String> extractRepositoryPatterns(@NonNull CleanupRuleSet ruleSet) {
        return ruleSet.getRules().stream()
                .filter(CleanupRule::isEnabled)
                .map(CleanupRule::getFilters)
                .map(CleanupRule.CleanupFilters::getRepositories)
                .filter(repos -> repos != null && !repos.isEmpty())
                .flatMap(List::stream)
                .distinct()
                .toList();
    }

    /**
     * Checks if a component matches the provided component-level filters.
     *
     * @param component    The component to test
     * @param repositories List of repository patterns (OR logic)
     * @param groups       List of group patterns (OR logic)
     * @param names        List of name patterns (OR logic)
     * @return true if the component matches all provided filters (AND logic between filter types)
     */
    private static boolean matchesComponentFilters(@NonNull ComponentXO component,
                                                   @Nullable List<String> repositories,
                                                   @Nullable List<String> groups,
                                                   @Nullable List<String> names) {
        // Repository filter
        if (repositories != null && !repositories.isEmpty()) {
            if (!matchesAnyPattern(component.getRepository(), repositories)) {
                return false;
            }
        }

        // Group filter
        if (groups != null && !groups.isEmpty()) {
            if (!matchesAnyPattern(component.getGroup(), groups)) {
                return false;
            }
        }

        // Name filter
        if (names != null && !names.isEmpty()) {
            return matchesAnyPattern(component.getName(), names);
        }

        return true;
    }

    /**
     * Checks if a value matches any of the provided wildcard patterns.
     *
     * @param value    The value to test (can be null)
     * @param patterns List of wildcard patterns
     * @return true if the value matches any pattern, false if value is null or no patterns match
     */
    private static boolean matchesAnyPattern(@Nullable String value, @NonNull List<String> patterns) {
        if (Strings.isNullOrEmpty(value)) {
            return false;
        }

        return patterns.stream().anyMatch(pattern -> matchesWildcardPattern(value, pattern));
    }

    /**
     * Tests if a value matches a wildcard pattern.
     * Supports '*' (any characters) and '?' (single character) wildcards.
     *
     * @param value   The value to test
     * @param pattern The wildcard pattern
     * @return true if the value matches the pattern
     */
    private static boolean matchesWildcardPattern(@NonNull String value, @NonNull String pattern) {
        if (Strings.isNullOrEmpty(pattern)) {
            return Strings.isNullOrEmpty(value);
        }

        // Convert wildcard pattern to regex
        // Escape special regex characters except * and ?
        String regex = pattern
                .replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("+", "\\+")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("|", "\\|")
                // Now handle wildcards
                .replace("*", ".*")
                .replace("?", ".");

        return value.matches(regex);
    }

    /**
     * Checks if a component matches a parsed rule.
     */
    private boolean matchesRule(@NonNull ComponentXO component, @NonNull ParsedRule rule) {
        // Check component-level filters
        if (!matchesComponentFilters(component, rule.repositories, rule.groups, rule.names)) {
            return false;
        }

        // Check format filter
        if (rule.formats != null && !rule.formats.isEmpty()) {
            if (!matchesAnyPattern(component.getFormat(), rule.formats)) {
                return false;
            }
        }

        // Check version filter
        if (rule.versions != null && !rule.versions.isEmpty()) {
            if (!matchesAnyPattern(component.getVersion(), rule.versions)) {
                return false;
            }
        }

        // Check asset-level date filters - ALL assets must match ALL criteria
        List<AssetXO> assets = component.getAssets();
        if (assets == null || assets.isEmpty()) {
            return false;
        }

        // Check updated filter (blobCreated) - all assets must be created before the cutoff
        if (rule.updatedBefore != null) {
            boolean allAssetsMatch = assets.stream()
                    .allMatch(asset -> asset.getBlobCreated() != null &&
                            asset.getBlobCreated().isBefore(rule.updatedBefore));
            if (!allAssetsMatch) {
                return false;
            }
        }

        // Check downloaded filter
        if (rule.isNeverDownloaded) {
            // All assets must have never been downloaded (lastDownloaded == null)
            boolean allNeverDownloaded = assets.stream()
                    .allMatch(asset -> asset.getLastDownloaded() == null);
            return allNeverDownloaded;
        } else if (rule.downloadedBefore != null) {
            // All assets must be downloaded before the cutoff (or never downloaded)
            boolean allAssetsMatch = assets.stream()
                    .allMatch(asset -> asset.getLastDownloaded() == null ||
                            asset.getLastDownloaded().isBefore(rule.downloadedBefore));
            return allAssetsMatch;
        }

        return true;
    }

    /**
     * Parsed rule with precompiled patterns and dates for efficient matching.
     */
    private record ParsedRule(
            @NonNull String name,
            @NonNull String action,
            @Nullable List<String> repositories,
            @Nullable List<String> formats,
            @Nullable List<String> groups,
            @Nullable List<String> names,
            @Nullable List<String> versions,
            @Nullable OffsetDateTime updatedBefore,
            @Nullable OffsetDateTime downloadedBefore,
            boolean isNeverDownloaded
    ) {
    }
}
