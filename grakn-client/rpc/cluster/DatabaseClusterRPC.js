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
Object.defineProperty(exports, "__esModule", { value: true });
exports.DatabaseReplicaRPC = exports.DatabaseClusterRPC = void 0;
const dependencies_internal_1 = require("../../dependencies_internal");
class DatabaseClusterRPC {
    constructor(databaseManagerCluster, database) {
        this._databases = {};
        for (const address of Object.keys(databaseManagerCluster.databaseManagers())) {
            const databaseManager = databaseManagerCluster.databaseManagers()[address];
            this._databases[address] = new dependencies_internal_1.DatabaseRPC(databaseManager.grpcClient(), database);
        }
        this._name = database;
        this._databaseManagerCluster = databaseManagerCluster;
        this._replicas = [];
    }
    static of(protoDB, databaseManagerCluster) {
        const database = protoDB.getName();
        const databaseClusterRPC = new DatabaseClusterRPC(databaseManagerCluster, database);
        databaseClusterRPC.replicas().push(...protoDB.getReplicasList().map(rep => DatabaseReplicaRPC.of(rep, databaseClusterRPC)));
        console.info(`Discovered database cluster: ${databaseClusterRPC}`);
        return databaseClusterRPC;
    }
    primaryReplica() {
        const primaryReplicas = this._replicas.filter(rep => rep.isPrimary());
        if (primaryReplicas.length)
            return primaryReplicas.reduce((current, next) => next.term() > current.term() ? next : current);
        else
            return null;
    }
    preferredSecondaryReplica() {
        return this._replicas.find(rep => rep.isPreferredSecondary()) || this._replicas[0];
    }
    name() {
        return this._name;
    }
    async delete() {
        for (const address of Object.keys(this._databases)) {
            if (await this._databaseManagerCluster.databaseManagers()[address].contains(this._name)) {
                await this._databases[address].delete();
            }
        }
    }
    replicas() {
        return this._replicas;
    }
    toString() {
        return this._name;
    }
}
exports.DatabaseClusterRPC = DatabaseClusterRPC;
class DatabaseReplicaRPC {
    constructor(database, address, term, isPrimary, isPreferredSecondary) {
        this._database = database;
        this._id = new ReplicaId(address, database.name());
        this._term = term;
        this._isPrimary = isPrimary;
        this._isPreferredSecondary = isPreferredSecondary;
    }
    static of(replica, database) {
        return new DatabaseReplicaRPC(database, dependencies_internal_1.ServerAddress.parse(replica.getAddress()), replica.getTerm(), replica.getPrimary(), replica.getPreferredSecondary());
    }
    id() {
        return this._id;
    }
    database() {
        return this._database;
    }
    term() {
        return this._term;
    }
    isPrimary() {
        return this._isPrimary;
    }
    isPreferredSecondary() {
        return this._isPreferredSecondary;
    }
    address() {
        return this.id().address();
    }
    toString() {
        return `${this._id}:${this._isPrimary ? "P" : "S"}:${this._term}`;
    }
}
exports.DatabaseReplicaRPC = DatabaseReplicaRPC;
class ReplicaId {
    constructor(address, databaseName) {
        this._address = address;
        this._databaseName = databaseName;
    }
    address() {
        return this._address;
    }
    toString() {
        return `${this._address}/${this._databaseName}`;
    }
}
