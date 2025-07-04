package com.pyx4j.nxrm.cleanup.model;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents a single cleanup rule with its properties and filters.
 * Each rule can target components by repository, format, group, name and version patterns,
 * and age/download filters.
 */
public final class CleanupRule {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("enabled")
    private boolean enabled = true;

    @JsonProperty("action")
    private String action = "delete";

    @JsonProperty("filters")
    private CleanupFilters filters;

    /**
     * Default constructor for Jackson deserialization.
     */
    public CleanupRule() {
    }

    /**
     * Constructor for creating a cleanup rule.
     *
     * @param name        The unique name of the rule
     * @param description Optional description of the rule
     * @param enabled     Whether the rule is enabled
     * @param action      The action to perform (delete or keep)
     * @param filters     The filters to apply
     */
    public CleanupRule(@NonNull String name, @Nullable String description, boolean enabled,
                       @NonNull String action, @NonNull CleanupFilters filters) {
        this.name = Objects.requireNonNull(name, "Rule name cannot be null");
        this.description = description;
        this.enabled = enabled;
        this.action = Objects.requireNonNull(action, "Action cannot be null");
        this.filters = Objects.requireNonNull(filters, "Filters cannot be null");
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = Objects.requireNonNull(name, "Rule name cannot be null");
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @NonNull
    public String getAction() {
        return action;
    }

    public void setAction(@NonNull String action) {
        this.action = Objects.requireNonNull(action, "Action cannot be null");
    }

    @NonNull
    public CleanupFilters getFilters() {
        return filters;
    }

    public void setFilters(@NonNull CleanupFilters filters) {
        this.filters = Objects.requireNonNull(filters, "Filters cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CleanupRule that = (CleanupRule) o;
        return enabled == that.enabled &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(action, that.action) &&
                Objects.equals(filters, that.filters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, enabled, action, filters);
    }

    @Override
    public String toString() {
        return "CleanupRule{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", enabled=" + enabled +
                ", action='" + action + '\'' +
                ", filters=" + filters +
                '}';
    }

    /**
     * Represents the filters that can be applied to a cleanup rule.
     */
    public static final class CleanupFilters {

        @JsonProperty("repositories")
        private List<String> repositories;

        @JsonProperty("formats")
        private List<String> formats;

        @JsonProperty("groups")
        private List<String> groups;

        @JsonProperty("names")
        private List<String> names;

        @JsonProperty("versions")
        private List<String> versions;

        @JsonProperty("updated")
        private String updated;

        @JsonProperty("downloaded")
        private String downloaded;

        /**
         * Default constructor for Jackson deserialization.
         */
        public CleanupFilters() {
        }

        @Nullable
        public List<String> getRepositories() {
            return repositories;
        }

        public void setRepositories(@Nullable List<String> repositories) {
            this.repositories = repositories;
        }

        @Nullable
        public List<String> getFormats() {
            return formats;
        }

        public void setFormats(@Nullable List<String> formats) {
            this.formats = formats;
        }

        @Nullable
        public List<String> getGroups() {
            return groups;
        }

        public void setGroups(@Nullable List<String> groups) {
            this.groups = groups;
        }

        @Nullable
        public List<String> getNames() {
            return names;
        }

        public void setNames(@Nullable List<String> names) {
            this.names = names;
        }

        @Nullable
        public List<String> getVersions() {
            return versions;
        }

        public void setVersions(@Nullable List<String> versions) {
            this.versions = versions;
        }

        @Nullable
        public String getUpdated() {
            return updated;
        }

        public void setUpdated(@Nullable String updated) {
            this.updated = updated;
        }

        @Nullable
        public String getDownloaded() {
            return downloaded;
        }

        public void setDownloaded(@Nullable String downloaded) {
            this.downloaded = downloaded;
        }

        /**
         * Checks if at least one filter is specified.
         *
         * @return true if at least one filter is present
         */
        public boolean hasAtLeastOneFilter() {
            return (repositories != null && !repositories.isEmpty()) ||
                    (formats != null && !formats.isEmpty()) ||
                    (groups != null && !groups.isEmpty()) ||
                    (names != null && !names.isEmpty()) ||
                    (versions != null && !versions.isEmpty()) ||
                    updated != null ||
                    downloaded != null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CleanupFilters that = (CleanupFilters) o;
            return Objects.equals(repositories, that.repositories) &&
                    Objects.equals(formats, that.formats) &&
                    Objects.equals(groups, that.groups) &&
                    Objects.equals(names, that.names) &&
                    Objects.equals(versions, that.versions) &&
                    Objects.equals(updated, that.updated) &&
                    Objects.equals(downloaded, that.downloaded);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repositories, formats, groups, names, versions, updated, downloaded);
        }

        @Override
        public String toString() {
            return "CleanupFilters{" +
                    "repositories=" + repositories +
                    ", formats=" + formats +
                    ", groups=" + groups +
                    ", names=" + names +
                    ", versions=" + versions +
                    ", updated='" + updated + '\'' +
                    ", downloaded='" + downloaded + '\'' +
                    '}';
        }
    }
}
