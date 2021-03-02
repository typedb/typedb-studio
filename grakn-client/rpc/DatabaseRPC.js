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
exports.DatabaseRPC = void 0;
const dependencies_internal_1 = require("../dependencies_internal");
const database_pb_1 = __importDefault(require("grakn-protocol/protobuf/database_pb"));
class DatabaseRPC {
    constructor(grpcClient, name) {
        this._name = name;
        this._grpcClient = grpcClient;
    }
    name() {
        return this._name;
    }
    delete() {
        if (!this._name)
            throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Client.MISSING_DB_NAME.message());
        const req = new database_pb_1.default.Database.Delete.Req().setName(this._name);
        return new Promise((resolve, reject) => {
            this._grpcClient.database_delete(req, (err) => {
                if (err)
                    reject(err);
                else
                    resolve();
            });
        });
    }
    toString() {
        return this._name;
    }
}
exports.DatabaseRPC = DatabaseRPC;
