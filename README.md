# Jenkins Pipeline Library

Reusable Jenkins shared library for CI/CD pipeline orchestration. Built for Selenium test automation pipelines running on Docker-based Selenium Grid infrastructure.

## Overview

This library provides pipeline building blocks for:
- **Grid Lifecycle** — Deterministic Docker Selenium Grid provisioning and teardown
- **Branch Policies** — Centralized branch configuration for pipeline behavior
- **Quality Gates** — Post-execution test result evaluation with threshold enforcement
- **Reporting** — HTML dashboard generation, report archival, and email notifications
- **Test Management** — Qase.io integration for test run tracking
- **AI Analysis** — Optional, advisory-only failure analysis using LLM providers

All functions are designed to be idempotent and deterministic to avoid dependency on Docker runtime state.

## Usage

Reference this library in your `Jenkinsfile`:

```groovy
@Library('jenkins-pipeline-library@v1.3.2') _

def branchConfig = getBranchConfig()

pipeline {
    agent none
    // ...
}
```

The library is registered in Jenkins via [JCasC](https://github.com/GauravYadav9/jenkins-controller-setup) with `implicit: false`, requiring explicit `@Library` declaration in every Jenkinsfile that uses it.

## Function Reference

### Grid Lifecycle

| Function | Description |
|---|---|
| [`startDockerGrid`](vars/startDockerGrid.groovy) | Provisions Selenium Grid via Docker Compose. Executes a deterministic **teardown → create → connect** sequence to prevent stale network state. Includes health polling with configurable timeout and interval. |
| [`stopDockerGrid`](vars/stopDockerGrid.groovy) | Graceful grid shutdown using sanitized project names. Non-blocking — logs a warning if grid is already stopped. |

### Pipeline Configuration

| Function | Description |
|---|---|
| [`getBranchConfig`](vars/getBranchConfig.groovy) | Returns centralized branch policy configuration: active branches, production candidates, AI analysis branches, and experimental branch patterns (`feature/*`, `bugfix/*`). |
| [`determineTestSuite`](vars/determineTestSuite.groovy) | Selects test suite based on build trigger: `regression` for timer/cron triggers, parameterized `SUITE_NAME` for manual, defaults to `smoke`. |

### Quality Gate

| Function | Description |
|---|---|
| [`checkTestFailures`](vars/checkTestFailures.groovy) | Parses Surefire/TestNG XML reports to count total tests, failures, and errors. Returns a map `[total, failures, errors]` consumed by the Jenkinsfile's quality gate stage for threshold-based pass/fail decisions. |

### Reporting & Notifications

| Function | Description |
|---|---|
| [`archiveAndPublishReports`](vars/archiveAndPublishReports.groovy) | Archives `reports/` and `logs/` directories, publishes HTML Test Dashboard to Jenkins UI via `publishHTML`. |
| [`generateDashboard`](vars/generateDashboard.groovy) | Generates a consolidated HTML dashboard (`reports/index.html`) with links to Chrome and Firefox reports, color-coded failure summary, and build metadata. |
| [`sendBuildSummaryEmail`](vars/sendBuildSummaryEmail.groovy) | Sends HTML email notifications via `emailext`. Uses branch-aware credential selection: `recipient-email-list` for production branches, `dev-recipient-email-list` for feature branches. Email includes failure summary and dashboard link. |
| [`printBuildMetadata`](vars/printBuildMetadata.groovy) | Logs build context (job name, build number, branch, trigger source, suite name) for pipeline debugging. |

### Integrations

| Function | Description |
|---|---|
| [`updateQase`](vars/updateQase.groovy) | Qase.io test management integration: creates a test run, uploads TestNG XML results, and marks the run as complete. Uses Jenkins credentials for API token. Non-blocking on failure. |

### AI Analysis

| Function | Description |
|---|---|
| [`analyzeFailuresWithAi`](vars/analyzeFailuresWithAi.groovy) | AI-powered failure root cause analysis. Runs **after** the quality gate, **never blocks** the build. Generates a Markdown analysis report archived as a build artifact. Gracefully skips if no failures exist or LLM provider is unavailable. Branch-gated — only runs on configured branches. |

## Architecture

```
Jenkinsfile
    └── @Library('jenkins-pipeline-library@v1.3.2')
         │
         ├── Setup Phase
         │    ├── getBranchConfig()
         │    ├── determineTestSuite()
         │    ├── printBuildMetadata()
         │    └── startDockerGrid()
         │
         ├── Test Execution (Maven/TestNG)
         │
         ├── Post-Execution Phase
         │    ├── checkTestFailures()       ← Quality Gate
         │    ├── generateDashboard()
         │    ├── archiveAndPublishReports()
         │    ├── analyzeFailuresWithAi()   ← Advisory only
         │    ├── updateQase()
         │    └── sendBuildSummaryEmail()
         │
         └── Cleanup
              └── stopDockerGrid()
```

## Design Principles

- **Idempotent operations**: Grid functions always tear down before creating, ensuring no state leakage between builds
- **Graceful degradation**: Every function handles its own failures — no single function can break the pipeline
- **Branch-aware behavior**: Quality gate thresholds, email recipients, and AI analysis are all branch-configurable
- **Non-blocking integrations**: Qase, AI analysis, and email are advisory — test results and quality gate drive the build status

## Prerequisites

- Jenkins with [Pipeline Shared Libraries](https://www.jenkins.io/doc/book/pipeline/shared-libraries/) support
- Docker and Docker Compose on the Jenkins agent
- JCasC configuration registering this library (see [jenkins-controller-setup](https://github.com/GauravYadav9/jenkins-controller-setup))

## Versioning

This library uses Git tags for version pinning:

```groovy
@Library('jenkins-pipeline-library@v1.3.2') _   // Pinned — safe for production
@Library('jenkins-pipeline-library@main') _      // Latest — for testing
```
