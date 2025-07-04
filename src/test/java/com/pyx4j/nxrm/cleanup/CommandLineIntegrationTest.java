package com.pyx4j.nxrm.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import com.pyx4j.nxrm.cleanup.model.SortBy;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * Integration tests for command line arguments functionality.
 */
class CommandLineIntegrationTest {

    @Test
    void commandLineArgs_withSortOption_shouldParseCorrectly() {
        NxCleanupCommandArgs args = new NxCleanupCommandArgs();
        CommandLine cmd = new CommandLine(args);

        // Test parsing with sort option
        cmd.parseArgs("--rules", "/dev/null", "--url", "https://nexus.example.com", "--username", "user", "--password", "pass", "--repo-sort", "size");

        assertThat(args.nexusServerUrl).isEqualTo("https://nexus.example.com");
        assertThat(args.nexusUsername).isEqualTo("user");
        assertThat(args.nexusPassword).isEqualTo("pass");
        assertThat(args.repositoriesSortBy).isEqualTo(SortBy.SIZE);
    }

    @Test
    void commandLineArgs_withoutSortOption_shouldUseDefault() {
        NxCleanupCommandArgs args = new NxCleanupCommandArgs();
        CommandLine cmd = new CommandLine(args);

        // Test parsing without sort option (should use default)
        cmd.parseArgs("--rules", "/dev/null", "--url", "https://nexus.example.com", "--username", "user", "--password", "pass");

        assertThat(args.repositoriesSortBy).isEqualTo(SortBy.COMPONENTS); // Default value
    }

    @Test
    void sortBy_parsing_shouldWorkWithValidValues() {
        NxCleanupCommandArgs args = new NxCleanupCommandArgs();
        CommandLine cmd = new CommandLine(args);

        // Test that picocli can parse enum values correctly (case sensitive)
        cmd.parseArgs("--rules", "/dev/null", "--url", "https://nexus.example.com", "--repo-sort", "COMPONENTS");
        assertThat(args.repositoriesSortBy).isEqualTo(SortBy.COMPONENTS);

        cmd.parseArgs("--rules", "/dev/null", "--url", "https://nexus.example.com", "--repo-sort", "NAME");
        assertThat(args.repositoriesSortBy).isEqualTo(SortBy.NAME);

        cmd.parseArgs("--rules", "/dev/null", "--url", "https://nexus.example.com", "--repo-sort", "SIZE");
        assertThat(args.repositoriesSortBy).isEqualTo(SortBy.SIZE);
    }

}
