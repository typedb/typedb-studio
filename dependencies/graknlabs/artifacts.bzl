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

load("@graknlabs_dependencies//distribution/artifact:rules.bzl", "artifact_file")

def graknlabs_grakn_core_artifact():
    artifact_file(
        name = "graknlabs_grakn_core_artifact",
        group_name = "graknlabs_grakn_core",
        artifact_name = "grakn-core-all-linux-{version}.tar.gz",
        tag = "1.8.1",
    )
    artifact_file(
        name = "graknlabs_grakn_core_artifact_windows",
        group_name = "graknlabs_grakn_core",
        artifact_name = "grakn-core-all-windows-{version}.zip",
        tag = "1.8.1",
    )
