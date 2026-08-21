# Build and Release Workflows Guide

This document explains how to use the build and release workflows in this monorepo.

## Scope

- Use this file for running and operating CI/CD workflows.
- Use `../NEW_PROJECT_ONBOARDING.md` for adding a brand-new project to the monorepo.

## Workflow Map

- `workflows/common-build.yaml`: reusable build workflow used by project-specific wrappers.
- `<project-id>-build.yaml`: build entry for `<project-id>`. (example: `workflows/agentic-build.yaml`: build entry for `agentic`.)
- `workflows/release.yaml`: targeted project release to Maven Central (manual trigger).

## Project Mapping (Source of Truth)

Project metadata is maintained in `config/projects.yaml`.

Each project must define:
- `path`: directory that contains the project `../pom.xml`
- `tagKey`: tag prefix used by release

Example:

```yaml
projects:
  fluxnova-backward-compatibility-plugin:
    path: fluxnova-backward-compatibility-plugin
    tagKey: bc-plugin
```

## Build Workflows

### How Build Is Automatically Triggered (PRs and Pushes)

Each project build workflow triggers on:
- `push` to `main`
- `pull_request` path changes for project directory (plus `.github/**` and `../pom.xml`)
- `workflow_dispatch` for manual runs

Builds on PRs run Maven build and tests for the affected project.
Builds on `main` branch run Maven `deploy` for the selected project.

## Release Workflow is Manually Triggered

Release is manually run from GitHub UI using `workflows/release.yaml`.

Inputs:
- `project`: one of the configured options in the workflow file, to be selected
- `dry-run`: defaults to `true` (for testing the release plan without deploying artifacts)

Post release, the artifacts are published to Sonatype Central Portal for validation.
They must then be manually promoted in Sonatype Central to become available on Maven Central.

## Important Guardrails

- Do not add project-specific build or release customizations in the repository root `../pom.xml`; keep them within the target project directory and its own `pom.xml`.
- Keep project-specific dependencies/plugins/config in project directory only.
- Keep `config/projects.yaml` in sync with workflow project ids/options.

## Quick Troubleshooting

- "Unknown project-id": check key in `config/projects.yaml` and wrapper workflow `project-id`.
- "Invalid branch name": release branch must start with `release/` or `hotfix/`.
- "Version is not SNAPSHOT": set project version to `*-SNAPSHOT` before release.
- Wrong tag prefix: verify `tagKey` in `config/projects.yaml`.

