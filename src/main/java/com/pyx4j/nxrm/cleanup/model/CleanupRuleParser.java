package com.pyx4j.nxrm.cleanup.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.pyx4j.nxrm.cleanup.DateFilterParser;
import org.jspecify.annotations.NonNull;

/**
 * Parser and validator for cleanup rules defined in YAML format.
 * Supports loading and validating cleanup rule sets from YAML files.
 */
public final class CleanupRuleParser {

    private static final Set<String> VALID_ACTIONS = Set.of("delete", "keep");
    private static final String NEVER = "never";

    private final ObjectMapper yamlMapper;

    public CleanupRuleParser() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * Parses a cleanup rule set from a YAML file.
     *
     * @param yamlFile The path to the YAML file
     * @return The parsed and validated cleanup rule set
     * @throws IOException              if the file cannot be read
     * @throws IllegalArgumentException if the YAML content is invalid
     */
    @NonNull
    public CleanupRuleSet parseFromFile(@NonNull Path yamlFile) throws IOException {
        Objects.requireNonNull(yamlFile, "YAML file path cannot be null");
        
        try (InputStream inputStream = Files.newInputStream(yamlFile)) {
            return parseFromStream(inputStream);
        }
    }

    /**
     * Parses a cleanup rule set from a YAML input stream.
     *
     * @param inputStream The input stream containing YAML content
     * @return The parsed and validated cleanup rule set
     * @throws IOException              if the stream cannot be read
     * @throws IllegalArgumentException if the YAML content is invalid
     */
    @NonNull
    public CleanupRuleSet parseFromStream(@NonNull InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "Input stream cannot be null");
        
        CleanupRuleSet ruleSet = yamlMapper.readValue(inputStream, CleanupRuleSet.class);
        validate(ruleSet);
        return ruleSet;
    }

    /**
     * Parses a cleanup rule set from a YAML string.
     *
     * @param yamlContent The YAML content as a string
     * @return The parsed and validated cleanup rule set
     * @throws IOException              if the YAML cannot be parsed
     * @throws IllegalArgumentException if the YAML content is invalid
     */
    @NonNull
    public CleanupRuleSet parseFromString(@NonNull String yamlContent) throws IOException {
        Objects.requireNonNull(yamlContent, "YAML content cannot be null");
        
        CleanupRuleSet ruleSet = yamlMapper.readValue(yamlContent, CleanupRuleSet.class);
        validate(ruleSet);
        return ruleSet;
    }

    /**
     * Validates a cleanup rule set for consistency and correctness.
     *
     * @param ruleSet The rule set to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validate(@NonNull CleanupRuleSet ruleSet) {
        Objects.requireNonNull(ruleSet, "Rule set cannot be null");
        
        List<CleanupRule> rules = ruleSet.getRules();
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("Rule set must contain at least one rule");
        }

        Set<String> ruleNames = new HashSet<>();
        
        for (CleanupRule rule : rules) {
            validateRule(rule);
            
            // Check for duplicate rule names
            String ruleName = rule.getName();
            if (ruleNames.contains(ruleName)) {
                throw new IllegalArgumentException("Duplicate rule name found: '" + ruleName + "'");
            }
            ruleNames.add(ruleName);
        }
    }

    /**
     * Validates a single cleanup rule.
     *
     * @param rule The rule to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateRule(@NonNull CleanupRule rule) {
        Objects.requireNonNull(rule, "Rule cannot be null");
        
        // Validate rule name
        if (rule.getName() == null || rule.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Rule must have a non-empty name");
        }

        // Validate action
        String action = rule.getAction();
        if (action == null || !VALID_ACTIONS.contains(action.toLowerCase())) {
            throw new IllegalArgumentException("Invalid action '" + action + 
                "'. Must be one of: " + VALID_ACTIONS);
        }

        // Validate filters
        CleanupRule.CleanupFilters filters = rule.getFilters();
        if (filters == null) {
            throw new IllegalArgumentException("Rule '" + rule.getName() + "' must have filters");
        }

        if (!filters.hasAtLeastOneFilter()) {
            throw new IllegalArgumentException("Rule '" + rule.getName() + 
                "' must have at least one filter specified");
        }

        // Validate date filters
        validateDateFilter(filters.getUpdated(), "updated", rule.getName());
        validateDownloadedFilter(filters.getDownloaded(), rule.getName());
    }

    /**
     * Validates a date filter (updated).
     *
     * @param dateFilter The date filter value
     * @param filterType The type of filter for error messages
     * @param ruleName   The name of the rule being validated
     * @throws IllegalArgumentException if the date filter is invalid
     */
    private void validateDateFilter(String dateFilter, String filterType, String ruleName) {
        if (dateFilter == null) {
            return; // Optional filter
        }

        try {
            DateFilterParser.parseDate(dateFilter);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + filterType + " filter in rule '" + 
                ruleName + "': " + e.getMessage(), e);
        }
    }

    /**
     * Validates a downloaded filter (supports "never" in addition to date formats).
     *
     * @param downloadedFilter The downloaded filter value
     * @param ruleName         The name of the rule being validated
     * @throws IllegalArgumentException if the downloaded filter is invalid
     */
    private void validateDownloadedFilter(String downloadedFilter, String ruleName) {
        if (downloadedFilter == null) {
            return; // Optional filter
        }

        // Special case: "never" is allowed for downloaded filter
        if (NEVER.equalsIgnoreCase(downloadedFilter.trim())) {
            return;
        }

        // Otherwise, validate as a date filter
        validateDateFilter(downloadedFilter, "downloaded", ruleName);
    }

    /**
     * Parses the downloaded filter value, returning null for "never" and a date for other values.
     *
     * @param downloadedFilter The downloaded filter value
     * @return OffsetDateTime for date values, null for "never"
     * @throws IllegalArgumentException if the filter format is invalid
     */
    @NonNull
    public static OffsetDateTime parseDownloadedFilter(@NonNull String downloadedFilter) {
        Objects.requireNonNull(downloadedFilter, "Downloaded filter cannot be null");
        
        String trimmed = downloadedFilter.trim();
        if (NEVER.equalsIgnoreCase(trimmed)) {
            throw new IllegalArgumentException("Use isNeverDownloaded() to check for 'never' value");
        }
        
        return DateFilterParser.parseDate(trimmed);
    }

    /**
     * Checks if the downloaded filter represents "never downloaded".
     *
     * @param downloadedFilter The downloaded filter value
     * @return true if the filter represents "never downloaded"
     */
    public static boolean isNeverDownloaded(String downloadedFilter) {
        return downloadedFilter != null && NEVER.equalsIgnoreCase(downloadedFilter.trim());
    }
}