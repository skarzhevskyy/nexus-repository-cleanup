package com.pyx4j.nxrm.cleanup.model;

abstract class ReportSection {

    boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
