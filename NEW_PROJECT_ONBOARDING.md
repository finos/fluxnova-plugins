# New Project Onboarding (Build + Release)

This guide explains the required workflow and config changes to onboard a new project in this monorepo.

## Guardrails (Read First)

- Do **not** modify the repository root `pom.xml` for project-specific implementation details.
- Keep project-level dependencies, plugins, and release behavior inside the new project directory only.
- Root `pom.xml` is a shared parent/build contract and should stay stable.

## 1) Create the Project Directory

Create a new top-level project directory with its own `pom.xml` and source layout.

Example:

- `new-project/pom.xml`
- `new-project/src/main/...`
- `new-project/src/test/...`

## 2) Register the Project in Mapping Config

Update `.github/config/projects.yaml` with a new entry.

Required fields:

- `path`: relative path to the project root directory
- `tagKey`: unique prefix used in release tags

Example:

```yaml
projects:
  new-project:
    path: new-project
    tagKey: new-project
```

Notes:

- The `new-project` key is the `project-id` used by build/release workflows.
- `tagKey` is used by release workflow to create tags in the format `<tagKey>-v<version>`.

## 3) Add a Project Build Workflow

Create a new workflow file under `.github/workflows/` similar to existing project build workflows.

Recommended file: `.github/workflows/new-project-build.yaml`

Minimal pattern:

```yaml
name: "New Project Build"

on:
  push:
    branches:
      - main
  pull_request:
    paths:
      - "new-project/**"
      - ".github/**"
      - "pom.xml"
  workflow_dispatch:

concurrency:
  group: ${{ github.ref == 'refs/heads/main' && format('ci-default-branch-{0}-{1}', github.sha, github.workflow) || format('ci-pr-{0}-{1}', github.ref, github.workflow) }}
  cancel-in-progress: true

jobs:
  build:
    uses: ./.github/workflows/common-build.yaml
    with:
      project-id: "new-project"
    secrets: inherit
```

**Why this is required:**
- Build is automatically triggered on PRs and main branch pushes.

## 4) Add the Project to Release Workflow Input Options

Update `.github/workflows/release.yaml` and add the new project key under:

- `on.workflow_dispatch.inputs.project.options`

Example:

```yaml
options:
  - fluxnova-backward-compatibility-plugin
  - agentic
  - agentic-subprocess
  - new-project
```

**Why this is required:**
- Release is manually triggered from GitHub UI.
- Only listed options are selectable.

## 5) Project POM Expectations for Release

Ensure the project `pom.xml` follows release prerequisites:

- Version must start as `1.0.0-SNAPSHOT` for first release.
- Parent should reference the repository shared parent where applicable.
- Project release metadata should be defined at project level (not in root for project-only behavior).

## 6) Tag and Versioning Behavior (Current Workflow Logic)

Release workflow computes:

- `release-version`: strips `-SNAPSHOT` from project version
- `development-version`:
  - `release/*` branch -> next major `X+1.0.0-SNAPSHOT`
  - `hotfix/*` branch -> next minor `X.Y+1.0-SNAPSHOT`
- `tag`: `<tagKey>-v<release-version>`

Example first release:

- project version in POM: `1.0.0-SNAPSHOT`
- release tag created: `new-project-v1.0.0`

## 7) Branch Naming Rules for Release Runs

Release workflow allows only:

- `release/*`
- `hotfix/*`

Any other branch name will fail validation.

## 8) Validation Checklist Before First Release

- [ ] Project directory and `pom.xml` created
- [ ] Entry added in `.github/config/projects.yaml`
- [ ] Build workflow created under `.github/workflows/`
- [ ] New project added in release workflow project options
- [ ] Project POM version is `1.0.0-SNAPSHOT`
- [ ] Dry-run release executed successfully from GitHub UI
- [ ] Real release executed after dry-run validation

## 9) What Must Stay Out of Root POM

Do not add project-specific items to root `pom.xml`, including:

- Project-only dependencies
- Project-only plugin executions
- Project-specific release customizations

Keep all such settings inside the project directory (`new-project/`).

## 10) Quick Troubleshooting

- "Unknown project-id": check key in `.github/config/projects.yaml` and wrapper workflow `project-id`.
- "Invalid branch name": release branch must start with `release/` or `hotfix/`.
- "Version is not SNAPSHOT": set project version to `*-SNAPSHOT` before release.
- Wrong tag prefix: verify `tagKey` in `.github/config/projects.yaml`.
- 