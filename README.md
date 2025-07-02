# nexus-repository-cleanup

A declarative, infrastructure-as-code tool for cleaning up artifacts in a Sonatype Nexus Repository (OSS or Pro)

## Motivation

Nexus Repository Manager’s built-in Cleanup Policies do not support filtering by “never downloaded” artifacts, and the upcoming Nexus versions may deprecate Groovy scripting. This project fills that gap by providing:

- **Declarative configuration**: Define cleanup rules in YAML.
- **Code-driven**: Manage policies alongside your CI/CD pipeline or Kubernetes Jobs.
- **REST API integration**: Works with Nexus OSS or Pro over HTTP(S).
- **Extensible filters**: Out-of-the-box support for “never downloaded,” age, size, and more.

## Features

- **“Never downloaded” filter**  
  Delete components that have never been requested by any client.
- **Age-based cleanup**  
  Remove artifacts older than a specified number of days.
- **Size-based cleanup**  
  Purge components exceeding a size threshold.
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

Create a `cleanup-config.yml` file. This file defines the cleanup policies and their filters.:

TBD
```yaml
policies:
  - name: never-downloaded
    description: Remove artifacts that have never been downloaded and older than 30 days
    repositories:
      - maven-releases
    filters:
      - downloaded: never
      - createdBefore: 30d

  - name: old-snapshots
    description: Remove Maven snapshots older than 30 days
    repositories:
      - maven-snapshots
    filters:
      - createdBefore: 30d 

  - name: large-packages
    description: Remove packages larger than 100 MB
    repositories:
      - docker-hosted
    filters:
      - createdBefore: 90d
      - artefactMaxSize: 100Mb
```

### Usage

```bash
# Run cleanup with dry-run (no deletions) from this source directory
./gradlew run --args="--config cleanup-config.yml --dry-run"

# Execute actual cleanup
./gradlew run --args="--config cleanup-config.yml --dry-run"
# or execute the donoaded JAR
java -jar nexus-repository-cleanup.jar --config cleanup-config.yml

# Using docker with environment variables
docker run --rm \
  -v "$(pwd)/config:/app/config" \
  -e NEXUS_URL=https://nexus.example.com \
  -e NEXUS_USERNAME=admin \
  -e NEXUS_PASSWORD=yourpassword \
  ghcr.io/skarzhevskyy/nexus-repository-cleanup:latest \
  --config /app/config/cleanup-config.yml
```

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m "Add your feature"`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

## License

This project is licensed under the [Apache License 2.0](LICENSE).
