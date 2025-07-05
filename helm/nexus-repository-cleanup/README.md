# Nexus Repository Cleanup Helm Chart

This Helm chart deploys the Nexus Repository Cleanup application as a Kubernetes CronJob to automatically clean up artifacts in your Sonatype Nexus Repository Manager.

## Prerequisites

- Kubernetes cluster version >= 1.30
- Helm 3.0+
- Access to a Nexus Repository Manager instance
- Nexus user account with appropriate cleanup permissions

## Installation

### Quick Start

1. Add the Helm repository (coming soon - after GitHub Actions implementation):
```bash
helm repo add nexus-cleanup https://ghcr.io/skarzhevskyy/charts
helm repo update
```

2. Install the chart:
```bash
helm install my-nexus-cleanup nexus-cleanup/nexus-repository-cleanup \
  --set nexusRepositoryCleanup.nexusUrl=https://nexus.example.com \
  --set nexusRepositoryCleanup.credentialsSecretName=nexus-credentials
```

### Installation from Source

1. Clone the repository:
```bash
git clone https://github.com/skarzhevskyy/nexus-repository-cleanup.git
cd nexus-repository-cleanup/helm/nexus-repository-cleanup
```

2. Create credentials secret:
```bash
kubectl create secret generic nexus-credentials \
  --from-literal=username=your-nexus-username \
  --from-literal=password=your-nexus-password
```

3. Install with custom values:
```bash
helm install my-nexus-cleanup . -f my-values.yaml
```

## Configuration

### Basic Configuration

The following table lists the configurable parameters and their default values:

| Parameter | Description | Default |
|-----------|-------------|---------|
| `image.registry` | Container registry | `ghcr.io` |
| `image.repository` | Image repository | `skarzhevskyy/nexus-repository-cleanup` |
| `image.tag` | Image tag | `""` (uses chart appVersion) |
| `image.pullPolicy` | Image pull policy | `IfNotPresent` |
| `cronjob.schedule` | Cron schedule for cleanup job | `0 1 * * *` (daily at 1 AM) |
| `cronjob.timeZone` | Timezone for cron schedule | `""` |
| `cronjob.suspend` | Suspend cron job execution | `false` |
| `nexusRepositoryCleanup.nexusUrl` | Nexus Repository Manager URL | `""` |
| `nexusRepositoryCleanup.credentialsSecretName` | Name of secret containing Nexus credentials | `""` |
| `nexusRepositoryCleanup.otherArguments` | Additional CLI arguments | `--report-top-groups --report-repositories-summary` |

### Cleanup Rules Configuration

You can configure cleanup rules in two ways:

#### Option 1: Inline Rules (Recommended)

Define rules directly in `values.yaml`:

```yaml
nexusRepositoryCleanup:
  nexusUrl: "https://nexus.example.com"
  credentialsSecretName: "nexus-credentials"
  rules:
    - name: "cleanup-old-snapshots"
      description: "Remove SNAPSHOT versions older than 30 days"
      enabled: true
      action: delete
      filters:
        repositories:
          - "maven-snapshots"
        formats:
          - "maven2"
        versions:
          - "*-SNAPSHOT"
        updated: "30 days"
    - name: "cleanup-never-downloaded"
      description: "Remove artifacts never downloaded and older than 90 days"
      enabled: true
      action: delete
      filters:
        repositories:
          - "maven-releases"
        formats:
          - "maven2"
        updated: "90 days"
        downloaded: "never"
```

#### Option 2: External ConfigMap

Create your own ConfigMap and reference it:

```yaml
nexusRepositoryCleanup:
  nexusUrl: "https://nexus.example.com"
  credentialsSecretName: "nexus-credentials"
  rulesRef: "my-cleanup-rules-configmap"
```

### Security Configuration

The chart implements security best practices by default:

```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  seccompProfile:
    type: RuntimeDefault

containerSecurityContext:
  runAsNonRoot: true
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true
  capabilities:
    drop: ["ALL"]
```

### Resource Configuration

Set resource limits and requests:

```yaml
resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 100m
    memory: 128Mi
```

### Environment Variables

Add custom environment variables:

```yaml
env:
  - name: CUSTOM_VAR
    value: "custom-value"
  - name: SECRET_VAR
    valueFrom:
      secretKeyRef:
        name: my-secret
        key: secret-key

envFrom:
  - configMapRef:
      name: my-config
  - secretRef:
      name: my-secret
```

## Examples

### Complete Production Configuration

```yaml
# values.yaml
image:
  registry: ghcr.io
  repository: skarzhevskyy/nexus-repository-cleanup
  tag: "latest"
  pullPolicy: IfNotPresent

cronjob:
  schedule: "0 2 * * *"  # 2 AM daily
  timeZone: "America/New_York"
  suspend: false

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 100m
    memory: 128Mi

nexusRepositoryCleanup:
  nexusUrl: "https://nexus.company.com"
  credentialsSecretName: "nexus-prod-credentials"
  otherArguments: "--report-top-groups --report-repositories-summary --dry-run"
  rules:
    - name: "cleanup-old-snapshots"
      description: "Remove SNAPSHOT artifacts older than 7 days"
      enabled: true
      action: delete
      filters:
        repositories:
          - "maven-snapshots"
          - "npm-snapshots"
        formats:
          - "maven2"
          - "npm"
        versions:
          - "*-SNAPSHOT"
        updated: "7 days"
    
    - name: "cleanup-never-downloaded-releases"
      description: "Remove release artifacts never downloaded and older than 180 days"
      enabled: true
      action: delete
      filters:
        repositories:
          - "maven-releases"
          - "npm-releases"
        formats:
          - "maven2"
          - "npm"
        updated: "180 days"
        downloaded: "never"

serviceAccount:
  create: true
  name: ""

pod:
  annotations:
    prometheus.io/scrape: "false"
  labels:
    environment: "production"

nodeSelector:
  kubernetes.io/arch: amd64

tolerations:
  - key: "dedicated"
    operator: "Equal"
    value: "nexus-cleanup"
    effect: "NoSchedule"
```

### Testing with Dry Run

For testing purposes, always start with `--dry-run`:

```yaml
nexusRepositoryCleanup:
  nexusUrl: "https://nexus-test.example.com"
  credentialsSecretName: "nexus-test-credentials"
  otherArguments: "--report-top-groups --report-repositories-summary --dry-run"
  rules:
    # ... your rules
```

## Troubleshooting

### Check CronJob Status

```bash
kubectl get cronjob
kubectl describe cronjob my-nexus-cleanup
```

### View Job History

```bash
kubectl get jobs -l app.kubernetes.io/instance=my-nexus-cleanup
```

### Check Logs

```bash
# Latest job logs
kubectl logs -l app.kubernetes.io/instance=my-nexus-cleanup --tail=100

# Specific job logs
kubectl logs job/my-nexus-cleanup-12345678
```

### Manual Job Execution

```bash
kubectl create job --from=cronjob/my-nexus-cleanup my-nexus-cleanup-manual
```

### Common Issues

1. **Authentication Errors**: Verify your credentials secret contains valid `username` and `password` or `token`
2. **No Rules Configured**: Ensure either `rules` or `rulesRef` is set
3. **Permission Denied**: Check that the Nexus user has appropriate cleanup permissions
4. **Connectivity Issues**: Verify network policies allow access to Nexus from the cluster

## Security Considerations

- Always use secrets for credentials, never plain text values
- Consider using service mesh or network policies to restrict egress traffic
- Regularly rotate Nexus credentials
- Use `--dry-run` mode first to validate cleanup rules
- Monitor job execution and review logs regularly
- Consider using a dedicated service account with minimal required permissions

## Development

### Testing the Chart

```bash
# Lint the chart
helm lint .

# Dry run to validate templates
helm install test-release . --dry-run --debug

# Template output
helm template test-release . --debug
```

### Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](../../LICENSE) file for details.