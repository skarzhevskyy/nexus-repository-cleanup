package com.pyx4j.nxrm.cleanup.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

import com.pyx4j.nxrm.cleanup.CleanupRuleParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CleanupRuleParserTest {

    private final CleanupRuleParser parser = new CleanupRuleParser();

    @Test
    void parseFromString_withValidSingleRule_shouldParseCorrectly() throws IOException {
        String yaml = """
                rules:
                  - name: "cleanup-old-components"
                    description: "Remove old components"
                    enabled: true
                    action: delete
                    filters:
                      repositories:
                        - "maven-releases"
                      formats:
                        - "maven2"
                      updated: "30d"
                """;

        CleanupRuleSet ruleSet = parser.parseFromString(yaml);

        assertThat(ruleSet.getRules()).hasSize(1);
        CleanupRule rule = ruleSet.getRules().get(0);
        assertThat(rule.getName()).isEqualTo("cleanup-old-components");
        assertThat(rule.getDescription()).isEqualTo("Remove old components");
        assertThat(rule.isEnabled()).isTrue();
        assertThat(rule.getAction()).isEqualTo("delete");
        
        CleanupRule.CleanupFilters filters = rule.getFilters();
        assertThat(filters.getRepositories()).containsExactly("maven-releases");
        assertThat(filters.getFormats()).containsExactly("maven2");
        assertThat(filters.getUpdated()).isEqualTo("30d");
    }

    @Test
    void parseFromString_withMultipleRules_shouldParseCorrectly() throws IOException {
        String yaml = """
                rules:
                  - name: "cleanup-snapshots"
                    enabled: true
                    action: delete
                    filters:
                      repositories:
                        - "maven-snapshots"
                      updated: "7 days"
                  - name: "keep-recent-releases"
                    enabled: true
                    action: keep
                    filters:
                      repositories:
                        - "maven-releases"
                      updated: "90 Days ago"
                """;

        CleanupRuleSet ruleSet = parser.parseFromString(yaml);

        assertThat(ruleSet.getRules()).hasSize(2);
        
        CleanupRule rule1 = ruleSet.getRules().get(0);
        assertThat(rule1.getName()).isEqualTo("cleanup-snapshots");
        assertThat(rule1.getAction()).isEqualTo("delete");
        assertThat(rule1.getFilters().getUpdated()).isEqualTo("7 days");
        
        CleanupRule rule2 = ruleSet.getRules().get(1);
        assertThat(rule2.getName()).isEqualTo("keep-recent-releases");
        assertThat(rule2.getAction()).isEqualTo("keep");
        assertThat(rule2.getFilters().getUpdated()).isEqualTo("90 Days ago");
    }

    @Test
    void parseFromString_withAllFilterTypes_shouldParseCorrectly() throws IOException {
        String yaml = """
                rules:
                  - name: "comprehensive-rule"
                    filters:
                      repositories:
                        - "*-releases"
                        - "*-snapshots"
                      formats:
                        - "maven2"
                        - "npm"
                      groups:
                        - "com.example.*"
                        - "org.springframework.*"
                      names:
                        - "spring-*"
                        - "*-test"
                      versions:
                        - "1.*"
                        - "*-SNAPSHOT"
                      updated: "30 days"
                      downloaded: "never"
                """;

        CleanupRuleSet ruleSet = parser.parseFromString(yaml);

        assertThat(ruleSet.getRules()).hasSize(1);
        CleanupRule rule = ruleSet.getRules().get(0);
        CleanupRule.CleanupFilters filters = rule.getFilters();
        
        assertThat(filters.getRepositories()).containsExactly("*-releases", "*-snapshots");
        assertThat(filters.getFormats()).containsExactly("maven2", "npm");
        assertThat(filters.getGroups()).containsExactly("com.example.*", "org.springframework.*");
        assertThat(filters.getNames()).containsExactly("spring-*", "*-test");
        assertThat(filters.getVersions()).containsExactly("1.*", "*-SNAPSHOT");
        assertThat(filters.getUpdated()).isEqualTo("30 days");
        assertThat(filters.getDownloaded()).isEqualTo("never");
    }

    @Test
    void parseFromString_withNeverDownloaded_shouldParseCorrectly() throws IOException {
        String yaml = """
                rules:
                  - name: "never-downloaded-rule"
                    filters:
                      repositories:
                        - "maven-releases"
                      downloaded: "never"
                """;

        CleanupRuleSet ruleSet = parser.parseFromString(yaml);
        CleanupRule rule = ruleSet.getRules().get(0);
        
        assertThat(rule.getFilters().getDownloaded()).isEqualTo("never");
    }

    @Test
    void parseFromString_withAbsoluteDate_shouldParseCorrectly() throws IOException {
        String yaml = """
                rules:
                  - name: "absolute-date-rule"
                    filters:
                      repositories:
                        - "maven-releases"
                      updated: "2025-03-01"
                """;

        CleanupRuleSet ruleSet = parser.parseFromString(yaml);
        CleanupRule rule = ruleSet.getRules().get(0);
        
        assertThat(rule.getFilters().getUpdated()).isEqualTo("2025-03-01");
    }

    @Test
    void parseFromFile_withValidFile_shouldParseCorrectly(@TempDir Path tempDir) throws IOException {
        String yaml = """
                rules:
                  - name: "file-rule"
                    filters:
                      repositories:
                        - "test-repo"
                """;

        Path yamlFile = tempDir.resolve("test-rules.yml");
        Files.writeString(yamlFile, yaml);

        CleanupRuleSet ruleSet = parser.parseFromFile(yamlFile);

        assertThat(ruleSet.getRules()).hasSize(1);
        assertThat(ruleSet.getRules().get(0).getName()).isEqualTo("file-rule");
    }

    @Test
    void parseFromStream_withValidStream_shouldParseCorrectly() throws IOException {
        String yaml = """
                rules:
                  - name: "stream-rule"
                    filters:
                      groups:
                        - "com.example"
                """;

        try (InputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))) {
            CleanupRuleSet ruleSet = parser.parseFromStream(stream);

            assertThat(ruleSet.getRules()).hasSize(1);
            assertThat(ruleSet.getRules().get(0).getName()).isEqualTo("stream-rule");
        }
    }

    // Negative test cases

    @Test
    void parseFromString_withEmptyRules_shouldThrowException() {
        String yaml = """
                rules: []
                """;

        assertThatThrownBy(() -> parser.parseFromString(yaml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rule set must contain at least one rule");
    }

    @Test
    void parseFromString_withUnknownFields_shouldThrowException() {
        String yaml = """
                other: value
                """;

        assertThatThrownBy(() -> parser.parseFromString(yaml))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Unrecognized field \"other\"");
    }

    @Test
    void parseFromString_withMissingName_shouldThrowException() {
        String yaml = """
                rules:
                  - description: "Missing name"
                    filters:
                      repositories:
                        - "test-repo"
                """;

        assertThatThrownBy(() -> parser.parseFromString(yaml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rule must have a non-empty name");
    }

    @Test
    void parseFromString_withEmptyName_shouldThrowException() {
        String yaml = """
                rules:
                  - name: ""
                    filters:
                      repositories:
                        - "test-repo"
                """;

        assertThatThrownBy(() -> parser.parseFromString(yaml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rule must have a non-empty name");
    }

    @Test
    void parseFromString_withDuplicateNames_shouldThrowException() {
        String yaml = """
                rules:
                  - name: "duplicate-name"
                    filters:
                      repositories:
                        - "repo1"
                  - name: "duplicate-name"
                    filters:
                      repositories:
                        - "repo2"
                """;

        assertThatThrownBy(() -> parser.parseFromString(yaml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate rule name found: 'duplicate-name'");
    }

    @Test
    void parseFromString_withNoFilters_shouldThrowException() {
        String yaml = """
                rules:
                  - name: "no-filters-rule"
                """;

        assertThatThrownBy(() -> parser.parseFromString(yaml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no-filters-rule")
                .hasMessageContaining("must have filters");
    }

    @Test
    void parseFromString_withEmptyFilters_shouldThrowException() {
        String yaml = """
                rules:
                  - name: "empty-filters-rule"
                    filters: {}
                """;

        assertThatThrownBy(() -> parser.parseFromString(yaml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty-filters-rule")
                .hasMessageContaining("must have at least one filter specified");
    }

    @Test
    void parseFromString_withInvalidAction_shouldThrowException() {
        String yaml = """
                rules:
                  - name: "invalid-action-rule"
                    action: "invalid"
                    filters:
                      repositories:
                        - "test-repo"
                """;

        assertThatThrownBy(() -> parser.parseFromString(yaml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid action 'invalid'")
                .hasMessageContaining("Must be one of: [delete, keep]");
    }

    @Test
    void parseFromString_withInvalidDateFormat_shouldThrowException() {
        String yaml = """
                rules:
                  - name: "invalid-date-rule"
                    filters:
                      repositories:
                        - "test-repo"
                      updated: "invalid-date"
                """;

        assertThatThrownBy(() -> parser.parseFromString(yaml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid updated filter in rule 'invalid-date-rule'")
                .hasMessageContaining("Invalid date format: 'invalid-date'");
    }

    @Test
    void parseFromString_withInvalidDownloadedFormat_shouldThrowException() {
        String yaml = """
                rules:
                  - name: "invalid-downloaded-rule"
                    filters:
                      repositories:
                        - "test-repo"
                      downloaded: "invalid-downloaded"
                """;

        assertThatThrownBy(() -> parser.parseFromString(yaml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid downloaded filter in rule 'invalid-downloaded-rule'")
                .hasMessageContaining("Invalid date format: 'invalid-downloaded'");
    }

    @Test
    void isNeverDownloaded_withNever_shouldReturnTrue() {
        assertThat(CleanupRuleParser.isNeverDownloaded("never")).isTrue();
        assertThat(CleanupRuleParser.isNeverDownloaded("NEVER")).isTrue();
        assertThat(CleanupRuleParser.isNeverDownloaded("Never")).isTrue();
        assertThat(CleanupRuleParser.isNeverDownloaded("  never  ")).isTrue();
    }

    @Test
    void isNeverDownloaded_withOtherValues_shouldReturnFalse() {
        assertThat(CleanupRuleParser.isNeverDownloaded("30d")).isFalse();
        assertThat(CleanupRuleParser.isNeverDownloaded("30 days")).isFalse();
        assertThat(CleanupRuleParser.isNeverDownloaded("2024-01-01")).isFalse();
        assertThat(CleanupRuleParser.isNeverDownloaded(null)).isFalse();
        assertThat(CleanupRuleParser.isNeverDownloaded("")).isFalse();
    }

    @Test
    void parseDownloadedFilter_withValidDate_shouldReturnOffsetDateTime() {
        OffsetDateTime result = CleanupRuleParser.parseDownloadedFilter("30d");
        assertThat(result).isNotNull();
        
        result = CleanupRuleParser.parseDownloadedFilter("30 days");
        assertThat(result).isNotNull();
        
        result = CleanupRuleParser.parseDownloadedFilter("2024-01-01");
        assertThat(result).isNotNull();
    }

    @Test
    void parseDownloadedFilter_withNever_shouldThrowException() {
        assertThatThrownBy(() -> CleanupRuleParser.parseDownloadedFilter("never"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Use isNeverDownloaded() to check for 'never' value");
    }

    @Test
    void parseDownloadedFilter_withInvalidFormat_shouldThrowException() {
        assertThatThrownBy(() -> CleanupRuleParser.parseDownloadedFilter("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid date format: 'invalid'");
    }
}