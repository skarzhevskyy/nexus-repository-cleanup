package com.pyx4j.nxrm.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;

import com.pyx4j.nxrm.cleanup.model.RepositoryComponentsSummary;
import com.pyx4j.nxrm.cleanup.model.SortBy;
import org.junit.jupiter.api.Test;
import org.sonatype.nexus.model.ComponentXO;

class ReportWriterTest {

    @Test
    void testJsonReportWriter() throws IOException {
        StringWriter stringWriter = new StringWriter();
        try (ReportWriter reportWriter = new JsonReportWriter(stringWriter)) {
            RepositoryComponentsSummary summary = new RepositoryComponentsSummary();
            summary.addRepositoryStats("test-repo", "maven2", 10, 1024);
            reportWriter.writeRepositoryComponentsSummary(summary, SortBy.NAME);
        }
        assertThat(stringWriter.toString()).contains("test-repo");
    }

    @Test
    void testCsvReportWriter() throws IOException {
        StringWriter stringWriter = new StringWriter();
        try (ReportWriter reportWriter = new CsvReportWriter(stringWriter)) {
            RepositoryComponentsSummary summary = new RepositoryComponentsSummary();
            summary.addRepositoryStats("test-repo", "maven2", 10, 1024);
            reportWriter.writeRepositoryComponentsSummary(summary, SortBy.NAME);
        }
        assertThat(stringWriter.toString()).contains("\"test-repo\",\"maven2\",\"10\",\"1024\"");
    }

    @Test
    void testJsonComponentWriter() throws IOException {
        StringWriter stringWriter = new StringWriter();
        try (ReportWriter reportWriter = new JsonReportWriter(stringWriter)) {
            ComponentXO component = new ComponentXO();
            component.setRepository("test-repo");
            component.setGroup("test-group");
            component.setName("test-name");
            component.setVersion("1.0");
            reportWriter.writeComponent(component);
        }
        assertThat(stringWriter.toString()).contains("test-repo");
    }

    @Test
    void testCsvComponentWriter() throws IOException {
        StringWriter stringWriter = new StringWriter();
        try (ReportWriter reportWriter = new CsvReportWriter(stringWriter)) {
            ComponentXO component = new ComponentXO();
            component.setRepository("test-repo");
            component.setGroup("test-group");
            component.setName("test-name");
            component.setVersion("1.0");
            reportWriter.writeComponent(component);
        }
        assertThat(stringWriter.toString()).contains("\"test-repo\",\"test-group\",\"test-name\",\"1.0\",\"0\"");
    }
}
