% openLCA development: branches and versions
% June 2023

# Introduction

This document describes how we (should) handle branches, releases, and their versions in openLCA. This is related to the `olca-app` and `olca-modules` repositories. openLCA has also other dependencies, also other `olca-*` libraries, but these live in their own repositories and can have their own version and branch management. Such dependencies should be linked with a specific version in openLCA (and not, for example, via a property that is related to the version of the `olca-modules`).


# The `master` branch

The `olca-app` and `olca-modules` repositories should both have a `master` branch that is always in sync with each other. It should be always possible to build a version from this branch that can be shared. This means, features that are merged into this branch should be tested and stable. Specifically, the test suite should be green, no compile errors should occur, etc. Of course there can be bugs and things may need to be changed later, but we should try to keep the `master` branch always stable and releasable. This also means, if you have a bug fix or a tested and stable feature, it is fine to merge it into the `master` branch.

Changes and features where feedback is needed, should not go into the master branch before the feedback gives the okay. Such things should be developed in a feature branch and versions of that feature branch should be shared with users for feedback (see also feature branches below). For example, if you change the icons or CSS styles of the application, do not merge this into the master branch until you received an okay from the team.


# Feature branches

Larger features should be developed in their own branch. If it affects the `olca-app` and `olca-modules` repository, both repositories should have the same branch name for that feature. If openLCA versions with that feature are shared, it can be useful to add the branch name to the version number of the `olca-modules` project and openLCA application (e.g. `2.0.1-ext-props` where `ext-props` is the branch name). Building a test version of a feature branch does not include installers, signed app-bundles, etc. A simple export and zip-packaging is enough. If the feature is merged, delete the branch. Do not reuse merged branches that you did not created.


# Releases and versions

When the current `master` branch is under development, so not released yet, the version should have the `SNAPSHOT` suffix attached, e.g. `2.0.1-SNAPSHOT` for the Maven modules and `2.0.1.SNAPSHOT` for the application (as the hyphen is not allowed here). Before a release the version should be changed removing this suffix. The release version can be higher than the snapshot version depending on the changes that are released (for bug-fixes and minor things we usually just increment the _patch_ version, for larger features and database changes we increment the _minor_ version, big releases get a _major_ version update; we do not maintain API compatibility in patch and minor versions). In the `master` branch, the version of the modules and application should be always the same.

When a new version was released, the `olca-app` and `olca-modules` are tagged with that version; e.g. `v2.0.1`. The modules of that version are published to the Maven Central repository. After the release, the patch version is incremented and the `SNAPSHOT` suffix is attached again in the `master` branch. For example, when a version `2.1.0` was released, the version is updated to `2.1.1-SNAPSHOT` in the master branch.

# Changing the version

The `versions` plugin can be used to update the version in the `olca-modules`:

```bash
cd olca-modules
mvn versions:set -DnewVersion=2.0.1-SNAPSHOT -DgenerateBackupPoms=false
```

This will set the same version in the parent- and sub-modules. Then, in the `olca-app` project you first need to set that same version in the `pom.xml` file so that the correct Maven dependencies are pulled here. Additionally, you set the version in the `openLCA.product` and `MANIFEST.MF` files.
