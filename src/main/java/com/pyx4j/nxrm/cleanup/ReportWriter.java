package com.pyx4j.nxrm.cleanup;

import java.io.IOException;

import com.pyx4j.nxrm.cleanup.model.GroupsSummary;
import com.pyx4j.nxrm.cleanup.model.RepositoryComponentsSummary;
import com.pyx4j.nxrm.cleanup.model.SortBy;
import org.sonatype.nexus.model.ComponentXO;

public interface ReportWriter extends AutoCloseable {

    void writeRepositoryComponentsSummary(RepositoryComponentsSummary summary, SortBy sortBy) throws IOException;

    void writeGroupsSummary(GroupsSummary summary, SortBy sortBy, int topGroups) throws IOException;

    void writeComponent(ComponentXO component) throws IOException;

    @Override
    void close() throws IOException;
}
