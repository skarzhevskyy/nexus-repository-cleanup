#!/bin/bash
#
# Quick setup script for nexus-repository-cleanup Helm chart
# This script helps you set up the basic prerequisites for the chart
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
NAMESPACE="default"
SECRET_NAME="nexus-credentials"
RELEASE_NAME="nexus-cleanup"

print_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -n, --namespace NAMESPACE    Kubernetes namespace (default: default)"
    echo "  -s, --secret SECRET_NAME     Secret name for credentials (default: nexus-credentials)"
    echo "  -r, --release RELEASE_NAME   Helm release name (default: nexus-cleanup)"
    echo "  -u, --nexus-url URL          Nexus Repository Manager URL (required)"
    echo "  --username USERNAME          Nexus username (required)"
    echo "  --password PASSWORD          Nexus password (required)"
    echo "  --token TOKEN                Nexus token (alternative to username/password)"
    echo "  --values-file FILE           Custom values file (optional)"
    echo "  --dry-run                    Run in dry-run mode (recommended for first time)"
    echo "  -h, --help                   Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --nexus-url https://nexus.company.com --username admin --password secret123"
    echo "  $0 --nexus-url https://nexus.company.com --token nx_token_123456 --dry-run"
    echo "  $0 --nexus-url https://nexus.company.com --username admin --password secret123 --values-file my-values.yaml"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -s|--secret)
            SECRET_NAME="$2"
            shift 2
            ;;
        -r|--release)
            RELEASE_NAME="$2"
            shift 2
            ;;
        -u|--nexus-url)
            NEXUS_URL="$2"
            shift 2
            ;;
        --username)
            NEXUS_USERNAME="$2"
            shift 2
            ;;
        --password)
            NEXUS_PASSWORD="$2"
            shift 2
            ;;
        --token)
            NEXUS_TOKEN="$2"
            shift 2
            ;;
        --values-file)
            VALUES_FILE="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            echo -e "${RED}Error: Unknown option $1${NC}"
            print_usage
            exit 1
            ;;
    esac
done

# Validate required parameters
if [[ -z "$NEXUS_URL" ]]; then
    echo -e "${RED}Error: Nexus URL is required${NC}"
    print_usage
    exit 1
fi

if [[ -z "$NEXUS_TOKEN" && (-z "$NEXUS_USERNAME" || -z "$NEXUS_PASSWORD") ]]; then
    echo -e "${RED}Error: Either token or username/password is required${NC}"
    print_usage
    exit 1
fi

echo -e "${BLUE}Nexus Repository Cleanup - Setup Script${NC}"
echo "======================================="
echo ""

# Check prerequisites
echo -e "${BLUE}Checking prerequisites...${NC}"

if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}Error: kubectl is not installed${NC}"
    exit 1
fi

if ! command -v helm &> /dev/null; then
    echo -e "${RED}Error: helm is not installed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ kubectl and helm are available${NC}"

# Check cluster connectivity
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}Error: Cannot connect to Kubernetes cluster${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Kubernetes cluster is accessible${NC}"

# Create namespace if it doesn't exist
if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
    echo -e "${YELLOW}Creating namespace: $NAMESPACE${NC}"
    kubectl create namespace "$NAMESPACE"
else
    echo -e "${GREEN}✓ Namespace $NAMESPACE exists${NC}"
fi

# Create or update secret
echo -e "${BLUE}Setting up credentials secret...${NC}"

if kubectl get secret "$SECRET_NAME" -n "$NAMESPACE" &> /dev/null; then
    echo -e "${YELLOW}Secret $SECRET_NAME exists, deleting to recreate...${NC}"
    kubectl delete secret "$SECRET_NAME" -n "$NAMESPACE"
fi

if [[ -n "$NEXUS_TOKEN" ]]; then
    kubectl create secret generic "$SECRET_NAME" \
        --from-literal=token="$NEXUS_TOKEN" \
        -n "$NAMESPACE"
    echo -e "${GREEN}✓ Created secret with token authentication${NC}"
else
    kubectl create secret generic "$SECRET_NAME" \
        --from-literal=username="$NEXUS_USERNAME" \
        --from-literal=password="$NEXUS_PASSWORD" \
        -n "$NAMESPACE"
    echo -e "${GREEN}✓ Created secret with username/password authentication${NC}"
fi

# Build helm command
HELM_CMD="helm install $RELEASE_NAME ./helm/nexus-repository-cleanup"
HELM_CMD="$HELM_CMD --namespace $NAMESPACE"
HELM_CMD="$HELM_CMD --set nexusRepositoryCleanup.nexusUrl=$NEXUS_URL"
HELM_CMD="$HELM_CMD --set nexusRepositoryCleanup.credentialsSecretName=$SECRET_NAME"

if [[ "$DRY_RUN" == "true" ]]; then
    HELM_CMD="$HELM_CMD --set nexusRepositoryCleanup.otherArguments='--report-top-groups --report-repositories-summary --dry-run'"
    echo -e "${YELLOW}Note: Adding --dry-run flag for safety${NC}"
fi

if [[ -n "$VALUES_FILE" ]]; then
    if [[ ! -f "$VALUES_FILE" ]]; then
        echo -e "${RED}Error: Values file $VALUES_FILE not found${NC}"
        exit 1
    fi
    HELM_CMD="$HELM_CMD -f $VALUES_FILE"
fi

# Install the chart
echo -e "${BLUE}Installing Helm chart...${NC}"
echo "Command: $HELM_CMD"
echo ""

if eval "$HELM_CMD"; then
    echo ""
    echo -e "${GREEN}✓ Helm chart installed successfully!${NC}"
    echo ""
    echo -e "${BLUE}Next steps:${NC}"
    echo "1. Check the CronJob status:"
    echo "   kubectl get cronjob $RELEASE_NAME -n $NAMESPACE"
    echo ""
    echo "2. View the generated configuration:"
    echo "   kubectl describe cronjob $RELEASE_NAME -n $NAMESPACE"
    echo ""
    echo "3. Manually trigger a test job:"
    echo "   kubectl create job --from=cronjob/$RELEASE_NAME ${RELEASE_NAME}-manual -n $NAMESPACE"
    echo ""
    echo "4. Check job logs:"
    echo "   kubectl logs -l app.kubernetes.io/instance=$RELEASE_NAME -n $NAMESPACE --tail=100"
    echo ""
    if [[ "$DRY_RUN" == "true" ]]; then
        echo -e "${YELLOW}Important: The chart is configured with --dry-run. No actual cleanup will be performed.${NC}"
        echo -e "${YELLOW}Remove the --dry-run flag when you're ready to perform actual cleanup.${NC}"
    fi
else
    echo -e "${RED}Error: Failed to install Helm chart${NC}"
    exit 1
fi