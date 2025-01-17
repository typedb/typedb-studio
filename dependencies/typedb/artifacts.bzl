# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@typedb_dependencies//distribution/artifact:rules.bzl", "native_artifact_files")
load("@typedb_dependencies//distribution:deployment.bzl", "deployment")

def typedb_artifact():
    native_artifact_files(
        name = "typedb_artifact",
        group_name = "typedb-server-{platform}",
        artifact_name = "typedb-server-{platform}-{version}.{ext}",
        tag_source = deployment["artifact"]["release"]["download"],
        commit_source = deployment["artifact"]["snapshot"]["download"],
        tag = "2.28.3",
    )

maven_artifacts = {
    'com.typedb:typedb-runner': '2.28.3',
    'com.typedb:typedb-cloud-runner': '2.28.3',
}
