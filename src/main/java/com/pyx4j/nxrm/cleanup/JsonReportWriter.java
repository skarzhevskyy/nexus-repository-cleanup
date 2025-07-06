package com.pyx4j.nxrm.cleanup;

import java.io.IOException;
import java.io.Writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pyx4j.nxrm.cleanup.model.GroupsSummary;
import com.pyx4j.nxrm.cleanup.model.RepositoryComponentsSummary;
import com.pyx4j.nxrm.cleanup.model.SortBy;
import org.sonatype.nexus.model.ComponentXO;

public class JsonReportWriter implements ReportWriter {

    private final ObjectMapper objectMapper;
    private final Writer writer;

    public JsonReportWriter(Writer writer) {
        this.writer = writer;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void writeRepositoryComponentsSummary(RepositoryComponentsSummary summary, SortBy sortBy) throws IOException {
        objectMapper.writeValue(writer, summary);
    }

    @Override
    public void writeGroupsSummary(GroupsSummary summary, SortBy sortBy, int topGroups) throws IOException {
        objectMapper.writeValue(writer, summary);
    }

    @Override
    public void writeComponent(ComponentXO component) throws IOException {
        objectMapper.writeValue(writer, component);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
