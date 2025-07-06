package com.pyx4j.nxrm.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.util.List;

import com.pyx4j.nxrm.cleanup.model.GroupsSummary;
import com.pyx4j.nxrm.cleanup.model.RepositoryComponentsSummary;
import com.pyx4j.nxrm.cleanup.model.SortBy;
import org.junit.jupiter.api.Test;
import org.sonatype.nexus.model.AssetXO;
import org.sonatype.nexus.model.ComponentXO;

/**
 * Unit tests for NxReportConsole functionality.
 */
class NxReportConsoleTest {

    @Test
    void printSummary_withShortRepositoryNames_shouldFormatCorrectly() {
        RepositoryComponentsSummary summary = new RepositoryComponentsSummary();
        summary.addRepositoryStats("maven-central", "maven2", 100, 1024000, 50, 512000);
        summary.addRepositoryStats("npm-proxy", "npm", 50, 512000, 25, 256000);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printSummary(summary, SortBy.COMPONENTS, printStream, true);

        String output = outputStream.toString();
        assertThat(output.replaceAll("\\s+", " "))
                .contains("Repository Report Summary (Dry Run)")
                .contains("Repository Format Removed # Removed Size Remaining # Remaining Size")
                .contains("maven-central")
                .contains("npm-proxy")
                .contains("TOTAL");

        // Check that repositories are sorted by components (maven-central should come first with 100 components)
        int mavenIndex = output.indexOf("maven-central");
        int npmIndex = output.indexOf("npm-proxy");
        assertThat(mavenIndex).isLessThan(npmIndex);
    }

    @Test
    void printSummary_withLongRepositoryNames_shouldAdjustFormatting() {
        RepositoryComponentsSummary summary = new RepositoryComponentsSummary();
        summary.addRepositoryStats("very-long-repository-name-that-exceeds-thirty-characters", "maven2", 100, 1024000, 50, 512000);
        summary.addRepositoryStats("short", "npm", 50, 512000, 25, 256000);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printSummary(summary, SortBy.NAME, printStream, false);

        String output = outputStream.toString();
        assertThat(output).contains("Repository Report Summary (Removal)");
        assertThat(output).contains("very-long-repository-name-that-exceeds-thirty-characters");
        assertThat(output).contains("short");

        // Verify that the format doesn't break with long names
        String[] lines = output.split("\n");
        boolean foundLongName = false;
        for (String line : lines) {
            if (line.contains("very-long-repository-name-that-exceeds-thirty-characters")) {
                foundLongName = true;
                // The line should be properly formatted (columns should be separated correctly)
                assertThat(line).matches(".*very-long-repository-name-that-exceeds-thirty-characters\\s+maven2\\s+\\d+\\s+.*?");
                break;
            }
        }
        assertThat(foundLongName).as("Long repository name should be found in output").isTrue();
    }

    @Test
    void printSummary_sortByName_shouldSortAlphabetically() {
        RepositoryComponentsSummary summary = new RepositoryComponentsSummary();
        summary.addRepositoryStats("zebra-repo", "maven2", 10, 1000, 5, 500);
        summary.addRepositoryStats("alpha-repo", "npm", 20, 2000, 10, 1000);
        summary.addRepositoryStats("beta-repo", "docker", 30, 3000, 15, 1500);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printSummary(summary, SortBy.NAME, printStream, true);

        String output = outputStream.toString();
        int alphaIndex = output.indexOf("alpha-repo");
        int betaIndex = output.indexOf("beta-repo");
        int zebraIndex = output.indexOf("zebra-repo");

        assertThat(alphaIndex).isLessThan(betaIndex);
        assertThat(betaIndex).isLessThan(zebraIndex);
    }

    @Test
    void printSummary_sortBySize_shouldSortByDescendingSize() {
        RepositoryComponentsSummary summary = new RepositoryComponentsSummary();
        summary.addRepositoryStats("small-repo", "maven2", 10, 1000, 5, 500);
        summary.addRepositoryStats("large-repo", "npm", 20, 10000, 10, 5000);
        summary.addRepositoryStats("medium-repo", "docker", 30, 5000, 15, 2500);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printSummary(summary, SortBy.SIZE, printStream, true);

        String output = outputStream.toString();
        int largeIndex = output.indexOf("large-repo");
        int mediumIndex = output.indexOf("medium-repo");
        int smallIndex = output.indexOf("small-repo");

        // Should be sorted largest to smallest
        assertThat(largeIndex).isLessThan(mediumIndex);
        assertThat(mediumIndex).isLessThan(smallIndex);
    }

    @Test
    void printSummary_sortByComponents_shouldSortByDescendingComponentCount() {
        RepositoryComponentsSummary summary = new RepositoryComponentsSummary();
        summary.addRepositoryStats("few-components", "maven2", 10, 1000, 5, 500);
        summary.addRepositoryStats("many-components", "npm", 100, 2000, 50, 1000);
        summary.addRepositoryStats("some-components", "docker", 50, 3000, 25, 1500);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printSummary(summary, SortBy.COMPONENTS, printStream, true);

        String output = outputStream.toString();
        int manyIndex = output.indexOf("many-components");
        int someIndex = output.indexOf("some-components");
        int fewIndex = output.indexOf("few-components");

        // Should be sorted most to least components
        assertThat(manyIndex).isLessThan(someIndex);
        assertThat(someIndex).isLessThan(fewIndex);
    }

    @Test
    void printSummary_shouldDisplayTotalCorrectly() {
        RepositoryComponentsSummary summary = new RepositoryComponentsSummary();
        summary.addRepositoryStats("repo1", "maven2", 100, 1024000, 50, 512000);
        summary.addRepositoryStats("repo2", "npm", 50, 512000, 25, 256000);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printSummary(summary, SortBy.NAME, printStream, true);

        String output = outputStream.toString();
        assertThat(output).contains("TOTAL");
        assertThat(output).contains("150"); // Total components
        // Check that size formatting is present (should be like "1.46 MB" for total)
        assertThat(output).contains("1.46 MB");
    }

    @Test
    void printGroupsSummary_withShortGroupNames_shouldFormatCorrectly() {
        GroupsSummary summary = new GroupsSummary();
        summary.addGroupStats("org.springframework", 1200, 1800000000L, 600, 900000000L); // 1.8 GB
        summary.addGroupStats("com.example", 950, 1200000000L, 475, 600000000L); // 1.2 GB

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printGroupsSummary(summary, SortBy.COMPONENTS, 10, printStream, false);

        String output = outputStream.toString();
        assertThat(output.replaceAll("\\s+", " "))
                .contains("Top Consuming Groups (by Components, Removal)")
                .contains("Group Removed # Removed Size Remaining # Remaining Size")
                .contains("org.springframework")
                .contains("com.example")
                .contains("1200")
                .contains("950");

        // Check that groups are sorted by components (org.springframework should come first with 1200 components)
        int springIndex = output.indexOf("org.springframework");
        int exampleIndex = output.indexOf("com.example");
        assertThat(springIndex).isLessThan(exampleIndex);
    }

    @Test
    void printGroupsSummary_sortBySize_shouldSortCorrectly() {
        GroupsSummary summary = new GroupsSummary();
        summary.addGroupStats("org.springframework", 800, 2000000000L, 400, 1000000000L); // 2.0 GB
        summary.addGroupStats("com.example", 1200, 1000000000L, 600, 500000000L); // 1.0 GB

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printGroupsSummary(summary, SortBy.SIZE, 10, printStream, true);

        String output = outputStream.toString();
        assertThat(output).contains("Top Consuming Groups (by Size, Dry Run)");

        // Check that groups are sorted by size (org.springframework should come first with 2.0 GB)
        int springIndex = output.indexOf("org.springframework");
        int exampleIndex = output.indexOf("com.example");
        assertThat(springIndex).isLessThan(exampleIndex);
    }

    @Test
    void printGroupsSummary_withTopGroups_shouldLimitOutput() {
        GroupsSummary summary = new GroupsSummary();
        summary.addGroupStats("org.springframework", 1000, 1000000000L, 500, 500000000L);
        summary.addGroupStats("com.example", 900, 900000000L, 450, 450000000L);
        summary.addGroupStats("org.apache", 800, 800000000L, 400, 400000000L);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printGroupsSummary(summary, SortBy.COMPONENTS, 2, printStream, true);

        String output = outputStream.toString();
        assertThat(output)
                .contains("org.springframework")
                .contains("com.example")
                .doesNotContain("org.apache"); // Should be limited to top 2
    }

    @Test
    void printGroupsSummary_withLongGroupNames_shouldAdjustFormatting() {
        GroupsSummary summary = new GroupsSummary();
        summary.addGroupStats("very-long-group-name-that-exceeds-thirty-characters.deeply.nested", 100, 1024000, 50, 512000);
        summary.addGroupStats("short", 50, 512000, 25, 256000);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        NxReportConsole.printGroupsSummary(summary, SortBy.NAME, 10, printStream, true);

        String output = outputStream.toString();
        String[] lines = output.split("\n");

        boolean foundLongName = false;
        for (String line : lines) {
            if (line.contains("very-long-group-name-that-exceeds-thirty-characters.deeply.nested")) {
                foundLongName = true;
                // The line should be properly formatted (columns should be separated correctly)
                assertThat(line).matches(".*very-long-group-name-that-exceeds-thirty-characters\\.deeply\\.nested\\s+\\d+\\s+.*?");
                break;
            }
        }
        assertThat(foundLongName).as("Long group name should be found in output").isTrue();
    }

    private ComponentXO createComponentWithAsset(OffsetDateTime blobCreated) {
        ComponentXO component = new ComponentXO();
        AssetXO asset = new AssetXO();
        asset.setBlobCreated(blobCreated);
        asset.setFileSize(1024L);
        component.setAssets(List.of(asset));
        return component;
    }
}
