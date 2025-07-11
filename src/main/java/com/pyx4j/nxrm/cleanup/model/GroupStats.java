package com.pyx4j.nxrm.cleanup.model;

/**
 * Stats for a specific group (e.g., Maven groupId, npm scope).
 */
public class GroupStats {

    private long componentCount;

    private long sizeBytes;

    private long remainingComponentCount;

    private long remainingSizeBytes;

    public GroupStats() {
        this.componentCount = 0;
        this.sizeBytes = 0;
        this.remainingComponentCount = 0;
        this.remainingSizeBytes = 0;
    }

    public void addComponents(long componentCount, long sizeBytes) {
        this.componentCount += componentCount;
        this.sizeBytes += sizeBytes;
    }

    public long getComponentCount() {
        return componentCount;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void addRemaining(long componentCount, long sizeBytes) {
        this.remainingComponentCount += componentCount;
        this.remainingSizeBytes += sizeBytes;
    }

    public long getRemainingComponentCount() {
        return remainingComponentCount;
    }

    public long getRemainingSizeBytes() {
        return remainingSizeBytes;
    }
}
