package com.pyx4j.nxrm.cleanup;

import java.util.concurrent.Callable;

import com.pyx4j.nxrm.cleanup.model.SortBy;
import picocli.CommandLine;

@CommandLine.Command(name = "nexus-repository-cleanup.jar",
        description = "Nexus Repository Cleanup Tool",
        versionProvider = ManifestVersionProvider.class,
        sortOptions = false, mixinStandardHelpOptions = true)
public class NxCleanupCommandArgs implements Callable<Integer> {

    @CommandLine.Option(
            names = {"--rules"},
            description = "The cleanup rules",
            required = true)
    public String rulesFile;

    @CommandLine.Option(
            names = {"--dry-run"},
            description = "Skip components removal, only report what is going to be removed")
    public boolean dryRun;

    @CommandLine.Option(
            names = {"--url"},
            description = "Nexus Repository Manager URL", required = true,
            defaultValue = "${NEXUS_URL}")
    public String nexusServerUrl;

    @CommandLine.Option(
            names = {"--username"},
            description = "Nexus Repository Manager username",
            defaultValue = "${NEXUS_USERNAME}")
    public String nexusUsername;

    @CommandLine.Option(
            names = {"--password"},
            description = "Nexus Repository Manager password",
            defaultValue = "${NEXUS_PASSWORD}")
    public String nexusPassword;

    @CommandLine.Option(
            names = {"--token"},
            description = "Nexus Repository Manager Token",
            defaultValue = "${NEXUS_TOKEN}")
    public String nexusToken;

    @CommandLine.Option(
            names = {"--proxy"},
            description = "Proxy server URL (e.g., proxy.example.com:8081 or http://proxy.example.com:8081)")
    public String proxyUrl;

    @CommandLine.Option(
            names = {"--report-repositories-summary"},
            description = "Report repositories summary")
    public boolean reportRepositoriesSummary;

    @CommandLine.Option(
            names = {"--report-top-groups"},
            description = "Report top-groups")
    public boolean reportTopGroups;

    @CommandLine.Option(
            names = {"--repo-sort"},
            description = "Sort repositories by: ${COMPLETION-CANDIDATES} (default: components)",
            converter = SortBy.CaseInsensitiveEnumConverter.class)
    public SortBy repositoriesSortBy = SortBy.COMPONENTS;

    @CommandLine.Option(
            names = {"--top-groups"},
            description = "Report to show only the top N groups (default: 10)")
    public int topGroups = 10;

    @CommandLine.Option(
            names = {"--group-sort"},
            description = "Sort groups by: ${COMPLETION-CANDIDATES} (default: components)",
            converter = SortBy.CaseInsensitiveEnumConverter.class)
    public SortBy groupSort = SortBy.COMPONENTS;

    @CommandLine.Option(
            names = {"--report-output-file"},
            description = "Save report to a file (e.g., report.json, report.csv)")
    public String reportOutputFile;

    @CommandLine.Option(
            names = {"--output-component"},
            description = "Save all filtered components to a file (e.g., components.json, components.csv)")
    public String outputComponentFile;


    public Integer call() throws Exception {
        return new NxCleanupJob(this).execute();
    }

}
