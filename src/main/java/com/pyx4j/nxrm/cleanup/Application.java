package com.pyx4j.nxrm.cleanup;

import picocli.CommandLine;

public class Application {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new NxCleanupCommandArgs()).execute(args);
        System.exit(exitCode);
    }

}
