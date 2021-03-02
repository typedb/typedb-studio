"use strict";
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.FailsafeTask = void 0;
const dependencies_internal_1 = require("../../dependencies_internal");
const database_pb_1 = __importDefault(require("grakn-protocol/protobuf/cluster/database_pb"));
var CLUSTER_REPLICA_NOT_PRIMARY = dependencies_internal_1.ErrorMessage.Client.CLUSTER_REPLICA_NOT_PRIMARY;
var UNABLE_TO_CONNECT = dependencies_internal_1.ErrorMessage.Client.UNABLE_TO_CONNECT;
var CLUSTER_UNABLE_TO_CONNECT = dependencies_internal_1.ErrorMessage.Client.CLUSTER_UNABLE_TO_CONNECT;
const PRIMARY_REPLICA_TASK_MAX_RETRIES = 10;
const FETCH_REPLICAS_MAX_RETRIES = 10;
const WAIT_FOR_PRIMARY_REPLICA_SELECTION_MS = 2000;
class FailsafeTask {
    constructor(client, database) {
        this._client = client;
        this._database = database;
    }
    rerun(replica) {
        return this.run(replica);
    }
    async runPrimaryReplica() {
        var _a;
        if (!((_a = this._client.clusterDatabases()[this._database]) === null || _a === void 0 ? void 0 : _a.primaryReplica())) {
            await this.seekPrimaryReplica();
        }
        let replica = this._client.clusterDatabases()[this._database].primaryReplica();
        let retries = 0;
        while (true) { // eslint-disable-line no-constant-condition
            try {
                return retries == 0 ? await this.run(replica) : await this.rerun(replica);
            }
            catch (e) {
                if (e instanceof dependencies_internal_1.GraknClientError && [CLUSTER_REPLICA_NOT_PRIMARY, UNABLE_TO_CONNECT].includes(e.errorMessage())) {
                    console.info("Unable to open a session or transaction, retrying in 2s...", e);
                    await this.waitForPrimaryReplicaSelection();
                    replica = await this.seekPrimaryReplica();
                }
                else
                    throw e;
            }
            if (++retries > PRIMARY_REPLICA_TASK_MAX_RETRIES)
                throw this.clusterNotAvailableError();
        }
    }
    async runAnyReplica() {
        let databaseClusterRPC = this._client.clusterDatabases()[this._database];
        if (!databaseClusterRPC)
            databaseClusterRPC = await this.fetchDatabaseReplicas();
        // Try the preferred secondary replica first, then go through the others
        const replicas = [databaseClusterRPC.preferredSecondaryReplica()]
            .concat(databaseClusterRPC.replicas().filter(rep => !rep.isPreferredSecondary()));
        let retries = 0;
        for (const replica of replicas) {
            try {
                return retries == 0 ? await this.run(replica) : await this.rerun(replica);
            }
            catch (e) {
                if (e instanceof dependencies_internal_1.GraknClientError && UNABLE_TO_CONNECT === e.errorMessage()) {
                    console.info("Unable to open a session or transaction to " + replica.id() + ". Attempting next replica.", e);
                }
                else
                    throw e;
            }
            retries++;
        }
        throw this.clusterNotAvailableError();
    }
    get client() {
        return this._client;
    }
    get database() {
        return this._database;
    }
    async seekPrimaryReplica() {
        let retries = 0;
        while (retries < FETCH_REPLICAS_MAX_RETRIES) {
            const databaseClusterRPC = await this.fetchDatabaseReplicas();
            if (databaseClusterRPC.primaryReplica()) {
                return databaseClusterRPC.primaryReplica();
            }
            else {
                await this.waitForPrimaryReplicaSelection();
                retries++;
            }
        }
        throw this.clusterNotAvailableError();
    }
    async fetchDatabaseReplicas() {
        for (const serverAddress of this._client.clusterMembers()) {
            try {
                console.info(`Fetching replica info from ${serverAddress}`);
                const res = await new Promise((resolve, reject) => {
                    this._client.graknClusterRPC(serverAddress).database_get(new database_pb_1.default.Database.Get.Req().setName(this._database), (err, res) => {
                        if (err)
                            reject(new dependencies_internal_1.GraknClientError(err));
                        else
                            resolve(res);
                    });
                });
                const databaseClusterRPC = dependencies_internal_1.DatabaseClusterRPC.of(res.getDatabase(), this._client.databases());
                this._client.clusterDatabases()[this._database] = databaseClusterRPC;
                return databaseClusterRPC;
            }
            catch (e) {
                console.info(`Failed to fetch replica info for database '${this._database}' from ${serverAddress}. Attempting next server.`, e);
            }
        }
        throw this.clusterNotAvailableError();
    }
    waitForPrimaryReplicaSelection() {
        return new Promise(resolve => setTimeout(resolve, WAIT_FOR_PRIMARY_REPLICA_SELECTION_MS));
    }
    clusterNotAvailableError() {
        const addresses = this._client.clusterMembers().join(",");
        return new dependencies_internal_1.GraknClientError(CLUSTER_UNABLE_TO_CONNECT.message(addresses));
    }
}
exports.FailsafeTask = FailsafeTask;
