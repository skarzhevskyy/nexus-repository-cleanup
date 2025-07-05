{{/*
Expand the name of the chart.
*/}}
{{- define "nexus-repository-cleanup.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "nexus-repository-cleanup.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "nexus-repository-cleanup.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "nexus-repository-cleanup.labels" -}}
helm.sh/chart: {{ include "nexus-repository-cleanup.chart" . }}
{{ include "nexus-repository-cleanup.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "nexus-repository-cleanup.selectorLabels" -}}
app.kubernetes.io/name: {{ include "nexus-repository-cleanup.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "nexus-repository-cleanup.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "nexus-repository-cleanup.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the image name
*/}}
{{- define "nexus-repository-cleanup.image" -}}
{{- $registry := .Values.image.registry -}}
{{- $repository := .Values.image.repository -}}
{{- $tag := .Values.image.tag | default .Chart.AppVersion -}}
{{- if $registry }}
{{- printf "%s/%s:%s" $registry $repository $tag }}
{{- else }}
{{- printf "%s:%s" $repository $tag }}
{{- end }}
{{- end }}

{{/*
Create the config map name for cleanup rules
*/}}
{{- define "nexus-repository-cleanup.configMapName" -}}
{{- if .Values.nexusRepositoryCleanup.existingCleanupRulesConfigMapName }}
{{- .Values.nexusRepositoryCleanup.existingCleanupRulesConfigMapName }}
{{- else }}
{{- include "nexus-repository-cleanup.fullname" . }}-rules
{{- end }}
{{- end }}

{{/*
Create command arguments
*/}}
{{- define "nexus-repository-cleanup.args" -}}
{{- $args := list }}
{{- if or .Values.nexusRepositoryCleanup.rules .Values.nexusRepositoryCleanup.existingCleanupRulesConfigMapName }}
{{- $args = append $args "--rules" }}
{{- $args = append $args "/app/config/cleanup-rules.yml" }}
{{- end }}
{{- if .Values.nexusRepositoryCleanup.nexusUrl }}
{{- $args = append $args "--url" }}
{{- $args = append $args .Values.nexusRepositoryCleanup.nexusUrl }}
{{- end }}
{{- if .Values.nexusRepositoryCleanup.dryRun }}
{{- $args = append $args "--dry-run" }}
{{- end }}
{{- if .Values.nexusRepositoryCleanup.otherArguments }}
{{- $otherArgs := splitList " " .Values.nexusRepositoryCleanup.otherArguments }}
{{- $args = concat $args $otherArgs }}
{{- end }}
{{- $args | toJson }}
{{- end }}
