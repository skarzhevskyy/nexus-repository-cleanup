package com.pyx4j.nxrm.cleanup;

import java.util.List;
import java.util.function.Predicate;

import com.google.common.base.Strings;
import com.pyx4j.nxrm.cleanup.model.CleanupRuleSet;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.sonatype.nexus.model.ComponentXO;

/**
 * Utility class for creating component filters based on criteria defined in rules.
 */
final class ComponentFilter {

    private final Predicate<ComponentXO> componentFilter;

    ComponentFilter(@NonNull CleanupRuleSet ruleSet) {
        componentFilter = createFilter(ruleSet);
    }

    public Predicate<ComponentXO> getComponentFilter() {
        return componentFilter;
    }

    @NonNull
    private Predicate<ComponentXO> createFilter(@NonNull CleanupRuleSet ruleSet) {
        return component -> {
            if (component == null || component.getAssets() == null || component.getAssets().isEmpty()) {
                return false;
            }
            return false;
        };
    }

    /**
     * Checks if a repository name matches the provided repository patterns.
     *
     * @param repositoryName The repository name to test
     * @return true if the repository name matches any of the patterns, or if no patterns are provided
     */
    public boolean matchesRepositoryFilter(@Nullable String repositoryName) {
        return true;
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
}
