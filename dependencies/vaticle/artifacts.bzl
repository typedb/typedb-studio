# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@vaticle_dependencies//distribution/artifact:rules.bzl", "native_artifact_files")
load("@vaticle_dependencies//distribution:deployment.bzl", "deployment")

def vaticle_typedb_artifact():
    native_artifact_files(
        name = "vaticle_typedb_artifact",
        group_name = "typedb-server-{platform}",
        artifact_name = "typedb-server-{platform}-{version}.{ext}",
        tag_source = deployment["artifact"]["release"]["download"],
        commit_source = deployment["artifact"]["snapshot"]["download"],
        tag = "2.26.3",
    )

maven_artifacts = {
    'com.vaticle.typedb:typedb-runner': '0.0.0-e182b2dfcaf5f4cf7e7ce3dbb46edad3dccc48e0',
    'com.vaticle.typedb:typedb-cloud-runner': '0.0.0-78f5c1c83be5f0c8982b86ae4d15dd0b2f377905',
}
