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
exports.DatabaseManagerClusterRPC = void 0;
const dependencies_internal_1 = require("../../dependencies_internal");
var CLUSTER_ALL_NODES_FAILED = dependencies_internal_1.ErrorMessage.Client.CLUSTER_ALL_NODES_FAILED;
const database_pb_1 = __importDefault(require("grakn-protocol/protobuf/cluster/database_pb"));
class DatabaseManagerClusterRPC {
    constructor(client, databaseManagers) {
        this._client = client;
        this._databaseManagers = databaseManagers;
    }
    async contains(name) {
        let errors = "";
        for (const address of Object.keys(this._databaseManagers)) {
            try {
                return await this._databaseManagers[address].contains(name);
            }
            catch (e) {
                errors += `- ${address}: ${e}\n`;
            }
        }
        throw new dependencies_internal_1.GraknClientError(CLUSTER_ALL_NODES_FAILED.message(errors));
    }
    async create(name) {
        for (const databaseManager of Object.values(this._databaseManagers)) {
            if (!(await databaseManager.contains(name))) {
                await databaseManager.create(name);
            }
        }
    }
    async get(name) {
        let errors = "";
        for (const address of Object.keys(this._databaseManagers)) {
            try {
                const res = await new Promise((resolve, reject) => {
                    this._client.graknClusterRPC(address).database_get(new database_pb_1.default.Database.Get.Req().setName(name), (err, res) => {
                        if (err)
                            reject(new dependencies_internal_1.GraknClientError(err));
                        else
                            resolve(res);
                    });
                });
                return dependencies_internal_1.DatabaseClusterRPC.of(res.getDatabase(), this);
            }
            catch (e) {
                errors += `- ${address}: ${e}\n`;
            }
        }
        throw new dependencies_internal_1.GraknClientError(CLUSTER_ALL_NODES_FAILED.message(errors));
    }
    async all() {
        let errors = "";
        for (const address of Object.keys(this._databaseManagers)) {
            try {
                const res = await new Promise((resolve, reject) => {
                    this._client.graknClusterRPC(address).database_all(new database_pb_1.default.Database.All.Req(), (err, res) => {
                        if (err)
                            reject(new dependencies_internal_1.GraknClientError(err));
                        else
                            resolve(res);
                    });
                });
                return res.getDatabasesList().map(db => dependencies_internal_1.DatabaseClusterRPC.of(db, this));
            }
            catch (e) {
                errors += `- ${address}: ${e}\n`;
            }
        }
        throw new dependencies_internal_1.GraknClientError(CLUSTER_ALL_NODES_FAILED.message(errors));
    }
    databaseManagers() {
        return this._databaseManagers;
    }
}
exports.DatabaseManagerClusterRPC = DatabaseManagerClusterRPC;
