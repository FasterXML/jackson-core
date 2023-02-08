#!/bin/bash

# This script simulates the Maven Release Plugin, but only performs
# release:clean and release:prepare. The release:perform step is handled by the
# CI when the tag is pushed.
#
# However, release:perform on Git requires the release.properties file. We must
# therefore modify the first commit created by release:prepare to include this
# file, and then delete the file in the second commit.
#
# This will ensure that release.properties is available to release:perform in
# the CI, while keeping with the expectation that this file does not get
# commited (long-term) to the repository.

set -euo pipefail

# Prepare but don't push, we'll need to modify the commits
./mvnw release:clean release:prepare -DpushChanges=false

# Step back to the first commit (from SNAPSHOT to release)
git reset HEAD~1

# delete tag created by release:prepare
tag_name="$(git tag --points-at)"
git tag -d "$tag_name"

# Add release.properties to that commit
git add release.properties
git commit --amend --no-edit

# recreate tag
git tag "$tag_name" -m "[maven-release-plugin] copy for tag $tag_name"

# Recreate second commit (from release to SNAPSHOT), removing
# release.properties from the repository
git rm release.properties
git add pom.xml
git commit -m "[maven-release-plugin] prepare for next development iteration"

# push everything
git push
git push origin "$tag_name"

# clean up
rm pom.xml.releaseBackup
