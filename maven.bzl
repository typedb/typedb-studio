# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

# FIXME: Copied from @vaticle_dependencies//library/maven:rules.bzl
load("@rules_jvm_external//:defs.bzl", rje_maven_install = "maven_install")
load("@rules_jvm_external//:specs.bzl", rje_maven = "maven", rje_parse = "parse")
load("@vaticle_dependencies//library/maven:artifacts.bzl", maven_artifacts_org = "artifacts")
load("@vaticle_dependencies//distribution:deployment.bzl", "deployment")
# FIXME: Studio compose dependencies are held back, out of sync with dependencies
load("//dependencies/maven:artifacts.bzl", typedb_studio_maven_overrides = "version_overrides")

def maven(artifacts_org, internal_artifacts = {}, artifacts_repo={}, override_targets={}, fail_on_missing_checksum=True, generate_compat_repositories=False):
    if len(artifacts_repo) > 0:
        _warn("There are {} artifacts_repo found. Overriding artifacts_org with `artifacts_repo` is discouraged!".format(len(artifacts_repo)))
    for a in artifacts_org:
        if a not in maven_artifacts_org.keys():
            fail("'" + a + "' has not been declared in @vaticle_dependencies")
    artifacts_selected = []
    for a in artifacts_org:
        artifact = maven_artifact(a, artifacts_repo.get(a, maven_artifacts_org[a]))
        artifacts_selected.append(artifact)
    for coordinate, info in internal_artifacts.items():
        if not coordinate.startswith("com.vaticle."):
            fail("'" + coordinate + "' is not an internal dependency and must be declared in @vaticle_dependencies")
        artifacts_selected.append(maven_artifact(coordinate, info))
    rje_maven_install(
        # FIXME studio compose dependencies are held back, out of sync with dependencies
        artifacts = artifacts_selected + typedb_studio_maven_overrides,
        repositories = [
            "https://repo1.maven.org/maven2",
            "https://repo.maven.apache.org/maven2/",
            deployment["maven"]["release"]["download"],
            deployment["maven"]["snapshot"]["download"],
            "https://dl.google.com/dl/android/maven2",
            "https://maven.pkg.jetbrains.space/public/p/compose/dev",
        ],
        override_targets = override_targets,
        strict_visibility = True,
        version_conflict_policy = "pinned",
        fetch_sources = True,
        fail_on_missing_checksum = fail_on_missing_checksum,
        generate_compat_repositories = generate_compat_repositories,
    )

def maven_artifact(artifact, artifact_info):
    group, artifact_id = artifact.split(':')
    if type(artifact_info) == type(' '):
        artifact = rje_maven.artifact(
            group = group,
            artifact = artifact_id,
            version = artifact_info,
        )
    elif type(artifact_info) == type({}):
        exclusions = None
        if 'exclude' in artifact_info:
            exclusions = []
            for e in artifact_info['exclude']:
                exclusions.append(e)
        artifact = rje_maven.artifact(
            group = group,
            artifact = artifact_id,
            version = artifact_info['version'],
            packaging = artifact_info['packaging'] if 'packaging' in artifact_info else None,
            classifier = artifact_info['classifier'] if 'classifier' in artifact_info else None,
            exclusions =  exclusions
        )
    else:
        fail("The info for '" + artifact + "' must either be a 'string' (eg., '1.8.1') or a 'dict' (eg., {'version': '1.8.1', 'exclude': ['org.slf4j:slf4j']})")
    return artifact

def _warn(msg):
    print('{red}{msg}{nc}'.format(red='\033[0;31m', msg=msg, nc='\033[0m'))
