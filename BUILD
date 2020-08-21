#
# GRAKN.AI - THE KNOWLEDGE GRAPH
# Copyright (C) 2019 Grakn Labs Ltd
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#


load("@graknlabs_bazel_distribution//brew:rules.bzl", "deploy_brew")
load("@graknlabs_dependencies//distribution/artifact:rules.bzl", "artifact_extractor")
load("@graknlabs_dependencies//tool/release:rules.bzl", "release_validate_deps")

deploy_brew(
    name = "deploy-brew",
    type = "cask",
    deployment_properties = "@graknlabs_dependencies//distribution:deployment.properties",
    formula = "//config/brew:grakn-workbase.rb",
)

artifact_extractor(
    name = "grakn-extractor",
    artifact = "@graknlabs_grakn_core_artifact//file",
)

release_validate_deps(
    name = "release-validate-deps",
    refs = "@graknlabs_workbase_workspace_refs//:refs.json",
    tagged_deps = [
        "graknlabs_grakn_core",
        "graknlabs_client_nodejs",
    ],
    tags = ["manual"]  # in order for bazel test //... to not fail
)