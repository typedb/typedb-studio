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
exports.DatabaseManagerRPC = void 0;
const dependencies_internal_1 = require("../dependencies_internal");
const database_pb_1 = __importDefault(require("grakn-protocol/protobuf/database_pb"));
const DatabaseRPC_1 = require("./DatabaseRPC");
class DatabaseManagerRPC {
    constructor(client) {
        this._grpcClient = client;
    }
    contains(name) {
        if (!name)
            throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Client.MISSING_DB_NAME);
        const req = new database_pb_1.default.Database.Contains.Req().setName(name);
        return new Promise((resolve, reject) => {
            this._grpcClient.database_contains(req, (err, res) => {
                if (err)
                    reject(new dependencies_internal_1.GraknClientError(err));
                else
                    resolve(res.getContains());
            });
        });
    }
    create(name) {
        if (!name)
            throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Client.MISSING_DB_NAME);
        const req = new database_pb_1.default.Database.Create.Req().setName(name);
        return new Promise((resolve, reject) => {
            this._grpcClient.database_create(req, (err) => {
                if (err)
                    reject(new dependencies_internal_1.GraknClientError(err));
                else
                    resolve();
            });
        });
    }
    async get(name) {
        if (await this.contains(name))
            return new DatabaseRPC_1.DatabaseRPC(this._grpcClient, name);
        else
            throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Client.DB_DOES_NOT_EXIST);
    }
    all() {
        const allRequest = new database_pb_1.default.Database.All.Req();
        return new Promise((resolve, reject) => {
            this._grpcClient.database_all(allRequest, (err, res) => {
                if (err)
                    reject(new dependencies_internal_1.GraknClientError(err));
                else
                    resolve(res.getNamesList().map(name => new DatabaseRPC_1.DatabaseRPC(this._grpcClient, name)));
            });
        });
    }
    grpcClient() {
        return this._grpcClient;
    }
}
exports.DatabaseManagerRPC = DatabaseManagerRPC;
