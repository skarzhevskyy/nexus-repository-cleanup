package com.pyx4j.nxrm.cleanup.model;

/**
 * Stats for a specific repository.
 */
public class RepositoryStats {

    private final String format;

    private long componentCount;

    private long sizeBytes;

    private long remainingComponentCount;

    private long remainingSizeBytes;

    public RepositoryStats(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
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
