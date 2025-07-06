package com.pyx4j.nxrm.cleanup.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Stores summary information about components grouped by their group field.
 */
public class GroupsSummary extends ReportSection {

    private final Map<String, GroupStats> groupStats;

    private long totalComponents;

    private long totalSizeBytes;

    private long totalRemainingComponents;

    private long totalRemainingSizeBytes;

    public GroupsSummary() {
        this.groupStats = new HashMap<>();
        this.totalComponents = 0;
        this.totalSizeBytes = 0;
        this.totalRemainingComponents = 0;
        this.totalRemainingSizeBytes = 0;
    }

    /**
     * Adds statistics for a group.
     *
     * @param groupName      The name of the group (e.g., Maven groupId, npm scope)
     * @param componentCount The number of components in the group
     * @param sizeBytes      The total size in bytes of all components in the group
     */
    public void addGroupStats(String groupName, long componentCount, long sizeBytes, long remainingComponentCount, long remainingSizeBytes) {
        Objects.requireNonNull(groupName, "Group name cannot be null");

        GroupStats stats = groupStats.computeIfAbsent(groupName, k -> new GroupStats());
        stats.addComponents(componentCount, sizeBytes);
        stats.addRemaining(remainingComponentCount, remainingSizeBytes);

        // Update totals
        totalComponents += componentCount;
        totalSizeBytes += sizeBytes;
        totalRemainingComponents += remainingComponentCount;
        totalRemainingSizeBytes += remainingSizeBytes;
    }

    /**
     * Gets an unmodifiable view of the group statistics.
     *
     * @return Map of group names to their statistics
     */
    public Map<String, GroupStats> getGroupStats() {
        return Collections.unmodifiableMap(groupStats);
    }

    /**
     * Gets the total number of components across all groups.
     *
     * @return Total component count
     */
    public long getTotalComponents() {
        return totalComponents;
    }

    /**
     * Gets the total size in bytes across all groups.
     *
     * @return Total size in bytes
     */
    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public long getTotalRemainingComponents() {
        return totalRemainingComponents;
    }

    public long getTotalRemainingSizeBytes() {
        return totalRemainingSizeBytes;
    }

}
