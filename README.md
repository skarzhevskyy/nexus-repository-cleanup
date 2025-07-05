# nexus-repository-cleanup

A declarative, infrastructure-as-code tool for cleaning up artifacts in a Sonatype Nexus Repository (OSS or Pro)

## Motivation

Nexus Repository Manager’s built-in Cleanup Policies do not support filtering by “never downloaded” artifacts, and the upcoming Nexus versions may deprecate Groovy scripting. This project fills that gap by providing:

- **Declarative configuration**: Define cleanup rules in YAML.
- **Code-driven**: Manage policies alongside your CI/CD pipeline or Kubernetes Jobs.
- **REST API integration**: Works with Nexus OSS or Pro over HTTP(S).
- **Extensible filters**: Out-of-the-box support for “never downloaded,” age, and more.

## Features

- **“Never downloaded” filter**  
  Delete components that have never been requested by any client.
- **Age-based cleanup**  
  Remove artifacts older than a specified number of days.
- **Repository scoping**  
  Target specific repositories or repository groups.
- **Dry-run mode**  
  Preview what would be deleted without making changes.
- **Audit reporting**  
  Generate a summary report of deleted components.

## Getting Started

### Prerequisites

- Java 17+ or Container runtime (depending on your preferred runtime)
- Network access to your Nexus Repository Manager
- User with **nx-admin** or equivalent REST-API permissions

### Configuration

Create a `cleanup-rules.yml` file that defines the cleanup policies and their filters. The YAML format supports defining multiple cleanup rules, each with customizable filters.

#### YAML Format

```yaml
rules:
  - name: "rule-name"                    # Required: Unique name for the rule
    description: "Optional description"   # Optional: Human-readable description
    enabled: true                        # Optional: Whether rule is enabled (default: true)
    action: delete                       # Optional: "delete" (default) or "keep"
    filters:                            # Required: At least one filter must be specified
      repositories:                     # Optional: Repository name patterns (supports wildcards)
        - "maven-*"
        - "npm-releases"
      formats:                          # Optional: Repository format filters
        - "maven2"
        - "npm"
      groups:                           # Optional: Component group patterns (supports wildcards)
        - "com.example.*"
        - "org.springframework.*"
      names:                            # Optional: Component name patterns (supports wildcards)
        - "spring-*"
        - "*-test"
      versions:                         # Optional: Version patterns (supports wildcards)
        - "1.*"
        - "*-SNAPSHOT"
      updated: "90 days"                # Optional: Components last updated before this time
      downloaded: "60 days"             # Optional: Components last downloaded before this time or "never"
```

#### Date Filter Formats

The `updated` and `downloaded` filters support multiple date formats:

- **Relative formats**: `"30d"`, `"30 days"`, `"30 Days"`, `"30 days ago"`
- **Absolute ISO dates**: `"2025-03-01"`, `"2025-03-01T00:00:00Z"`
- **Special values**: `downloaded: "never"` for components that have never been downloaded

#### Examples

##### Example 1: Standard Rule with Age and Download Filters

```yaml
rules:
  - name: "cleanup-stale-components"
    description: >
      Remove components last modified more than 90 days ago
      that have not been downloaded in the last 60 days
    enabled: true
    action: delete
    filters:
      repositories:
        - "maven-releases"
        - "maven-snapshots"
      formats:
        - "maven2"
      groups:
        - "com.example.*"
        - "org.springframework.*"
      names:
        - "spring-*"
        - "*-deprecated"
      versions:
        - "1.*"
        - "*-SNAPSHOT"
      updated: "90 days"
      downloaded: "60 days"
```

##### Example 2: Never Downloaded Components

```yaml
rules:
  - name: "cleanup-never-downloaded"
    description: >
      Remove components that have never been downloaded
      and are older than 30 days
    enabled: true
    action: delete
    filters:
      repositories:
        - "maven-releases"
      formats:
        - "maven2"
      updated: "30 days ago"
      downloaded: "never"
```

### Authentication

The tool supports multiple authentication methods:

#### Username and Password
```bash
# Command line arguments
./gradlew run --args="--rules cleanup-rules.yml --url https://nexus.example.com --username admin --password yourpassword"

# Environment variables
export NEXUS_USERNAME=admin
export NEXUS_PASSWORD=yourpassword
./gradlew run --args="--rules cleanup-rules.yml --url https://nexus.example.com"
```

#### Authentication Token
```bash
# Command line argument
./gradlew run --args="--rules cleanup-rules.yml --url https://nexus.example.com --token your-auth-token"

# Environment variable
export NEXUS_TOKEN=your-auth-token
./gradlew run --args="--rules cleanup-rules.yml --url https://nexus.example.com"
```

### Proxy Support

The tool supports proxy configuration through multiple methods:

```bash
# Command line proxy argument (highest priority)
./gradlew run --args="--rules cleanup-rules.yml --url https://nexus.example.com --proxy proxy.company.com:8080"
./gradlew run --args="--rules cleanup-rules.yml --url https://nexus.example.com --proxy http://user:pass@proxy.company.com:8080"

# Environment variables
export HTTP_PROXY=http://proxy.company.com:8080
export HTTPS_PROXY=http://proxy.company.com:8080
./gradlew run --args="--rules cleanup-rules.yml"

# Java system properties
./gradlew run -Dhttp.proxyHost=proxy.company.com -Dhttp.proxyPort=8080 --args="--rules cleanup-rules.yml"
```

### Environment Variables

The following environment variables are supported:

- `NEXUS_URL` - Nexus Repository Manager URL (required)
- `NEXUS_USERNAME` - Username for authentication
- `NEXUS_PASSWORD` - Password for authentication
- `NEXUS_TOKEN` - Authentication token (alternative to username/password)
- `HTTP_PROXY` - HTTP proxy URL
- `HTTPS_PROXY` - HTTPS proxy URL

### Report Generation

The tool provides comprehensive reporting capabilities:

#### Repository Summary Report
```bash
# Generate repository summary with component counts and sizes
./gradlew run --args="--rules cleanup-rules.yml --report-repositories-summary"

# Sort repositories by different criteria
./gradlew run --args="--rules cleanup-rules.yml --report-repositories-summary --repo-sort name"
./gradlew run --args="--rules cleanup-rules.yml --report-repositories-summary --repo-sort size"
./gradlew run --args="--rules cleanup-rules.yml --report-repositories-summary --repo-sort components"
```

#### Top Groups Report
```bash
# Generate top groups report
./gradlew run --args="--rules cleanup-rules.yml --report-top-groups"

# Customize number of top groups and sorting
./gradlew run --args="--rules cleanup-rules.yml --report-top-groups --top-groups 20 --group-sort size"
```

#### Save Reports to Files
```bash
# Save report to JSON or CSV file
./gradlew run --args="--rules cleanup-rules.yml --report-repositories-summary --report-output-file report.json"
./gradlew run --args="--rules cleanup-rules.yml --report-repositories-summary --report-output-file report.csv"

# Save filtered components list to file
./gradlew run --args="--rules cleanup-rules.yml --output-component components.json"
./gradlew run --args="--rules cleanup-rules.yml --output-component components.csv"
```

### Usage

```bash
# Run cleanup with dry-run (no deletions) from this source directory
./gradlew run --args="--rules cleanup-rules.yml --url https://nexus.example.com --username admin --password yourpassword --dry-run"

# Execute actual cleanup
./gradlew run --args="--rules cleanup-rules.yml --url https://nexus.example.com --username admin --password yourpassword"

# Using environment variables
export NEXUS_URL=https://nexus.example.com
export NEXUS_USERNAME=admin
export NEXUS_PASSWORD=yourpassword
./gradlew run --args="--rules cleanup-rules.yml"

# or execute the downloaded JAR
java -jar nexus-repository-cleanup.jar --rules cleanup-rules.yml --url https://nexus.example.com --username admin --password yourpassword

# Using docker with environment variables
docker run --rm \
  -v "$(pwd)/config:/app/config" \
  -e NEXUS_URL=https://nexus.example.com \
  -e NEXUS_USERNAME=admin \
  -e NEXUS_PASSWORD=yourpassword \
  ghcr.io/skarzhevskyy/nexus-repository-cleanup:latest \
  --rules /app/config/cleanup-rules.yml
```

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m "Add your feature"`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

## License

This project is licensed under the [Apache License 2.0](LICENSE).
