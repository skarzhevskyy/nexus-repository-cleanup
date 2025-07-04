package com.pyx4j.nxrm.cleanup.model;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;

/**
 * Represents a collection of cleanup rules loaded from a YAML file.
 * Contains a list of rules that define cleanup policies.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public final class CleanupRuleSet {

    @JsonProperty("rules")
    private List<CleanupRule> rules;

    /**
     * Default constructor for Jackson deserialization.
     */
    public CleanupRuleSet() {
    }

    /**
     * Constructor for creating a cleanup rule set.
     *
     * @param rules The list of cleanup rules
     */
    public CleanupRuleSet(@NonNull List<CleanupRule> rules) {
        this.rules = Objects.requireNonNull(rules, "Rules list cannot be null");
    }

    @NonNull
    public List<CleanupRule> getRules() {
        return rules;
    }

    public void setRules(@NonNull List<CleanupRule> rules) {
        this.rules = Objects.requireNonNull(rules, "Rules list cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CleanupRuleSet that = (CleanupRuleSet) o;
        return Objects.equals(rules, that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rules);
    }

    @Override
    public String toString() {
        return "CleanupRuleSet{" +
                "rules=" + rules +
                '}';
    }
}
