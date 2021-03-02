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
exports.ClientClusterRPC = void 0;
const dependencies_internal_1 = require("../../dependencies_internal");
const grpc_js_1 = require("@grpc/grpc-js");
const grakn_cluster_grpc_pb_1 = require("grakn-protocol/protobuf/cluster/grakn_cluster_grpc_pb");
const cluster_pb_1 = __importDefault(require("grakn-protocol/protobuf/cluster/cluster_pb"));
var CLUSTER_UNABLE_TO_CONNECT = dependencies_internal_1.ErrorMessage.Client.CLUSTER_UNABLE_TO_CONNECT;
class ClientClusterRPC {
    async open(addresses) {
        const serverAddresses = await this.fetchClusterServers(addresses);
        this._coreClients = serverAddresses.reduce((obj, addr) => {
            obj[addr.toString()] = new dependencies_internal_1.ClientRPC(addr.external());
            return obj;
        }, {});
        this._graknClusterRPCs = serverAddresses.reduce((obj, addr) => {
            obj[addr.toString()] = new grakn_cluster_grpc_pb_1.GraknClusterClient(addr.external(), grpc_js_1.ChannelCredentials.createInsecure());
            return obj;
        }, {});
        const databaseManagers = Object.entries(this._coreClients).reduce((obj, [addr, client]) => {
            obj[addr] = client.databases();
            return obj;
        }, {});
        this._databaseManagers = new dependencies_internal_1.DatabaseManagerClusterRPC(this, databaseManagers);
        this._clusterDatabases = {};
        this._isOpen = true;
        return this;
    }
    session(database, type, options = dependencies_internal_1.GraknOptions.cluster()) {
        if (options.readAnyReplica) {
            return this.sessionAnyReplica(database, type, options);
        }
        else {
            return this.sessionPrimaryReplica(database, type, options);
        }
    }
    sessionPrimaryReplica(database, type, options) {
        return new OpenSessionFailsafeTask(database, type, options, this).runPrimaryReplica();
    }
    sessionAnyReplica(database, type, options) {
        return new OpenSessionFailsafeTask(database, type, options, this).runAnyReplica();
    }
    databases() {
        return this._databaseManagers;
    }
    isOpen() {
        return this._isOpen;
    }
    close() {
        if (this._isOpen) {
            this._isOpen = false;
            Object.values(this._coreClients).forEach(client => client.close());
        }
    }
    isCluster() {
        return true;
    }
    clusterDatabases() {
        return this._clusterDatabases;
    }
    clusterMembers() {
        return Object.keys(this._coreClients);
    }
    coreClient(address) {
        return this._coreClients[address];
    }
    graknClusterRPC(address) {
        return this._graknClusterRPCs[address];
    }
    async fetchClusterServers(addresses) {
        for (const address of addresses) {
            const client = new dependencies_internal_1.ClientRPC(address);
            try {
                console.info(`Fetching list of cluster servers from ${address}...`);
                const grpcClusterClient = new grakn_cluster_grpc_pb_1.GraknClusterClient(address, grpc_js_1.ChannelCredentials.createInsecure());
                const res = await new Promise((resolve, reject) => {
                    grpcClusterClient.cluster_servers(new cluster_pb_1.default.Cluster.Servers.Req(), (err, res) => {
                        if (err)
                            reject(new dependencies_internal_1.GraknClientError(err));
                        else
                            resolve(res);
                    });
                });
                const members = res.getServersList().map(x => dependencies_internal_1.ServerAddress.parse(x));
                console.info(`The cluster servers are ${members}`);
                return members;
            }
            catch (e) {
                console.error(`Fetching cluster servers from ${address} failed.`, e);
            }
            finally {
                client.close();
            }
        }
        throw new dependencies_internal_1.GraknClientError(CLUSTER_UNABLE_TO_CONNECT.message(addresses.join(",")));
    }
}
exports.ClientClusterRPC = ClientClusterRPC;
class OpenSessionFailsafeTask extends dependencies_internal_1.FailsafeTask {
    constructor(database, type, options, client) {
        super(client, database);
        this._type = type;
        this._options = options;
    }
    run(replica) {
        const session = new dependencies_internal_1.SessionClusterRPC(this.client, replica.address());
        return session.open(replica.address(), this.database, this._type, this._options);
    }
}
