package com.pyx4j.nxrm.cleanup.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CleanupRuleSetTest {

    @Test
    void cleanupRuleSet_withValidRules_shouldCreateCorrectly() {
        CleanupRule.CleanupFilters filters = new CleanupRule.CleanupFilters();
        filters.setRepositories(List.of("my-repo"));

        CleanupRule rule = new CleanupRule("test-rule", "Test description", true, "delete", filters);
        List<CleanupRule> rules = List.of(rule);

        CleanupRuleSet ruleSet = new CleanupRuleSet(rules);

        assertThat(ruleSet.getRules()).isEqualTo(rules);
        assertThat(ruleSet.getRules()).hasSize(1);
        assertThat(ruleSet.getRules().get(0)).isEqualTo(rule);
    }

    @Test
    void cleanupRuleSet_withDefaultConstructor_shouldAllowSettingRules() {
        CleanupRuleSet ruleSet = new CleanupRuleSet();

        CleanupRule.CleanupFilters filters = new CleanupRule.CleanupFilters();
        filters.setNames(List.of("spring-*"));
        CleanupRule rule = new CleanupRule("my-rule", null, true, "delete", filters);
        List<CleanupRule> rules = List.of(rule);

        ruleSet.setRules(rules);

        assertThat(ruleSet.getRules()).isEqualTo(rules);
    }

    @Test
    void cleanupRuleSet_equals_shouldCompareRules() {
        CleanupRule.CleanupFilters filters = new CleanupRule.CleanupFilters();
        filters.setRepositories(List.of("repo1"));
        CleanupRule rule = new CleanupRule("test", "desc", true, "delete", filters);

        CleanupRuleSet ruleSet1 = new CleanupRuleSet(List.of(rule));
        CleanupRuleSet ruleSet2 = new CleanupRuleSet(List.of(rule));

        assertThat(ruleSet1).isEqualTo(ruleSet2);
        assertThat(ruleSet1.hashCode()).isEqualTo(ruleSet2.hashCode());
    }

    @Test
    void cleanupRuleSet_toString_shouldIncludeRules() {
        CleanupRule.CleanupFilters filters = new CleanupRule.CleanupFilters();
        filters.setGroups(List.of("com.example"));
        CleanupRule rule = new CleanupRule("test-rule", "Description", false, "keep", filters);

        CleanupRuleSet ruleSet = new CleanupRuleSet(List.of(rule));
        String str = ruleSet.toString();

        assertThat(str).contains("CleanupRuleSet");
        assertThat(str).contains("rules=");
        assertThat(str).contains("test-rule");
    }
}
