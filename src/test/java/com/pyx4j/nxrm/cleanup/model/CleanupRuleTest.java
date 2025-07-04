package com.pyx4j.nxrm.cleanup.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CleanupRuleTest {

    @Test
    void cleanupRule_withValidFields_shouldCreateCorrectly() {
        CleanupRule.CleanupFilters filters = new CleanupRule.CleanupFilters();
        filters.setRepositories(List.of("my-repo"));
        filters.setUpdated("30d");

        CleanupRule rule = new CleanupRule("test-rule", "Test description", true, "delete", filters);

        assertThat(rule.getName()).isEqualTo("test-rule");
        assertThat(rule.getDescription()).isEqualTo("Test description");
        assertThat(rule.isEnabled()).isTrue();
        assertThat(rule.getAction()).isEqualTo("delete");
        assertThat(rule.getFilters()).isEqualTo(filters);
    }

    @Test
    void cleanupRule_withDefaultConstructor_shouldAllowSettingFields() {
        CleanupRule rule = new CleanupRule();
        CleanupRule.CleanupFilters filters = new CleanupRule.CleanupFilters();
        filters.setNames(List.of("spring-*"));

        rule.setName("my-rule");
        rule.setDescription("My description");
        rule.setEnabled(false);
        rule.setAction("keep");
        rule.setFilters(filters);

        assertThat(rule.getName()).isEqualTo("my-rule");
        assertThat(rule.getDescription()).isEqualTo("My description");
        assertThat(rule.isEnabled()).isFalse();
        assertThat(rule.getAction()).isEqualTo("keep");
        assertThat(rule.getFilters()).isEqualTo(filters);
    }

    @Test
    void cleanupRule_equals_shouldCompareAllFields() {
        CleanupRule.CleanupFilters filters1 = new CleanupRule.CleanupFilters();
        filters1.setRepositories(List.of("repo1"));
        
        CleanupRule.CleanupFilters filters2 = new CleanupRule.CleanupFilters();
        filters2.setRepositories(List.of("repo1"));

        CleanupRule rule1 = new CleanupRule("test", "desc", true, "delete", filters1);
        CleanupRule rule2 = new CleanupRule("test", "desc", true, "delete", filters2);

        assertThat(rule1).isEqualTo(rule2);
        assertThat(rule1.hashCode()).isEqualTo(rule2.hashCode());
    }

    @Test
    void cleanupRule_toString_shouldIncludeAllFields() {
        CleanupRule.CleanupFilters filters = new CleanupRule.CleanupFilters();
        filters.setGroups(List.of("com.example"));
        
        CleanupRule rule = new CleanupRule("test-rule", "Description", false, "keep", filters);
        String str = rule.toString();

        assertThat(str).contains("test-rule");
        assertThat(str).contains("Description");
        assertThat(str).contains("false");
        assertThat(str).contains("keep");
        assertThat(str).contains("filters=");
    }

    @Test
    void cleanupFilters_hasAtLeastOneFilter_shouldReturnTrueWhenFilterPresent() {
        CleanupRule.CleanupFilters filters = new CleanupRule.CleanupFilters();
        
        assertThat(filters.hasAtLeastOneFilter()).isFalse();

        filters.setRepositories(List.of("repo1"));
        assertThat(filters.hasAtLeastOneFilter()).isTrue();

        filters.setRepositories(null);
        filters.setFormats(List.of("maven2"));
        assertThat(filters.hasAtLeastOneFilter()).isTrue();

        filters.setFormats(null);
        filters.setGroups(List.of("com.example"));
        assertThat(filters.hasAtLeastOneFilter()).isTrue();

        filters.setGroups(null);
        filters.setNames(List.of("spring-*"));
        assertThat(filters.hasAtLeastOneFilter()).isTrue();

        filters.setNames(null);
        filters.setVersions(List.of("1.*"));
        assertThat(filters.hasAtLeastOneFilter()).isTrue();

        filters.setVersions(null);
        filters.setUpdated("30d");
        assertThat(filters.hasAtLeastOneFilter()).isTrue();

        filters.setUpdated(null);
        filters.setDownloaded("never");
        assertThat(filters.hasAtLeastOneFilter()).isTrue();
    }

    @Test
    void cleanupFilters_hasAtLeastOneFilter_shouldReturnFalseWhenEmptyLists() {
        CleanupRule.CleanupFilters filters = new CleanupRule.CleanupFilters();
        filters.setRepositories(List.of());
        filters.setFormats(List.of());
        filters.setGroups(List.of());
        filters.setNames(List.of());
        filters.setVersions(List.of());

        assertThat(filters.hasAtLeastOneFilter()).isFalse();
    }

    @Test
    void cleanupFilters_equals_shouldCompareAllFields() {
        CleanupRule.CleanupFilters filters1 = new CleanupRule.CleanupFilters();
        filters1.setRepositories(List.of("repo1"));
        filters1.setUpdated("30d");
        filters1.setDownloaded("never");

        CleanupRule.CleanupFilters filters2 = new CleanupRule.CleanupFilters();
        filters2.setRepositories(List.of("repo1"));
        filters2.setUpdated("30d");
        filters2.setDownloaded("never");

        assertThat(filters1).isEqualTo(filters2);
        assertThat(filters1.hashCode()).isEqualTo(filters2.hashCode());
    }

    @Test
    void cleanupFilters_toString_shouldIncludeAllFields() {
        CleanupRule.CleanupFilters filters = new CleanupRule.CleanupFilters();
        filters.setRepositories(List.of("repo1", "repo2"));
        filters.setFormats(List.of("maven2"));
        filters.setGroups(List.of("com.example"));
        filters.setNames(List.of("spring-*"));
        filters.setVersions(List.of("1.*"));
        filters.setUpdated("30 days");
        filters.setDownloaded("never");

        String str = filters.toString();

        assertThat(str).contains("repositories=[repo1, repo2]");
        assertThat(str).contains("formats=[maven2]");
        assertThat(str).contains("groups=[com.example]");
        assertThat(str).contains("names=[spring-*]");
        assertThat(str).contains("versions=[1.*]");
        assertThat(str).contains("updated='30 days'");
        assertThat(str).contains("downloaded='never'");
    }
}