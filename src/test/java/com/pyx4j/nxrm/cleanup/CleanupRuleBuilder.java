package com.pyx4j.nxrm.cleanup;

import java.util.List;

import com.pyx4j.nxrm.cleanup.model.CleanupRule;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Builder pattern for creating CleanupRule instances in tests.
 */
public final class CleanupRuleBuilder {

    private String name = "test-rule";
    private String description;
    private boolean enabled = true;
    private String action = "delete";
    private final CleanupRule.CleanupFilters filters = new CleanupRule.CleanupFilters();

    private CleanupRuleBuilder() {
    }

    public static CleanupRuleBuilder builder() {
        return new CleanupRuleBuilder();
    }

    public CleanupRuleBuilder name(@NonNull String name) {
        this.name = name;
        return this;
    }

    public CleanupRuleBuilder description(@Nullable String description) {
        this.description = description;
        return this;
    }

    public CleanupRuleBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public CleanupRuleBuilder action(@NonNull String action) {
        this.action = action;
        return this;
    }

    public CleanupRuleBuilder repositories(@Nullable List<String> repositories) {
        this.filters.setRepositories(repositories);
        return this;
    }

    public CleanupRuleBuilder formats(@Nullable List<String> formats) {
        this.filters.setFormats(formats);
        return this;
    }

    public CleanupRuleBuilder groups(@Nullable List<String> groups) {
        this.filters.setGroups(groups);
        return this;
    }

    public CleanupRuleBuilder names(@Nullable List<String> names) {
        this.filters.setNames(names);
        return this;
    }

    public CleanupRuleBuilder versions(@Nullable List<String> versions) {
        this.filters.setVersions(versions);
        return this;
    }

    public CleanupRuleBuilder updated(@Nullable String updated) {
        this.filters.setUpdated(updated);
        return this;
    }

    public CleanupRuleBuilder downloaded(@Nullable String downloaded) {
        this.filters.setDownloaded(downloaded);
        return this;
    }

    public CleanupRule build() {
        return new CleanupRule(name, description, enabled, action, filters);
    }
}
