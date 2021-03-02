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
exports.SessionRPC = void 0;
const dependencies_internal_1 = require("../dependencies_internal");
const session_pb_1 = __importDefault(require("grakn-protocol/protobuf/session_pb"));
class SessionRPC {
    constructor(client, database, type) {
        this._database = new dependencies_internal_1.DatabaseRPC(client.grpcClient(), database);
        this._type = type;
        this._client = client;
        this._grpcClient = client.grpcClient();
    }
    async open(options = dependencies_internal_1.GraknOptions.core()) {
        const openReq = new session_pb_1.default.Session.Open.Req()
            .setDatabase(this._database.name())
            .setType(sessionType(this._type))
            .setOptions(dependencies_internal_1.OptionsProtoBuilder.options(options));
        this._options = options;
        this._isOpen = true;
        const res = await new Promise((resolve, reject) => {
            this._grpcClient.session_open(openReq, (err, res) => {
                if (err)
                    reject(new dependencies_internal_1.GraknClientError(err));
                else
                    resolve(res);
            });
        });
        this._id = res.getSessionId_asB64();
        this._pulse = setTimeout(() => this.pulse(), 5000);
        return this;
    }
    transaction(type, options = dependencies_internal_1.GraknOptions.core()) {
        const transaction = new dependencies_internal_1.TransactionRPC(this._grpcClient, type);
        return transaction.open(this._id, options);
    }
    type() {
        return this._type;
    }
    options() {
        return this._options;
    }
    isOpen() {
        return this._isOpen;
    }
    async close() {
        if (this._isOpen) {
            this._isOpen = false;
            clearTimeout(this._pulse);
            const req = new session_pb_1.default.Session.Close.Req().setSessionId(this._id);
            await new Promise(resolve => {
                this._grpcClient.session_close(req, () => {
                    resolve();
                });
            });
        }
    }
    database() {
        return this._database;
    }
    id() {
        return this._id;
    }
    pulse() {
        if (!this._isOpen)
            return;
        const pulse = new session_pb_1.default.Session.Pulse.Req().setSessionId(this._id);
        this._grpcClient.session_pulse(pulse, (err, res) => {
            if (err || !res.getAlive())
                this._isOpen = false;
            else
                this._pulse = setTimeout(() => this.pulse(), 5000);
        });
    }
}
exports.SessionRPC = SessionRPC;
function sessionType(type) {
    switch (type) {
        case dependencies_internal_1.SessionType.DATA:
            return session_pb_1.default.Session.Type.DATA;
        case dependencies_internal_1.SessionType.SCHEMA:
            return session_pb_1.default.Session.Type.SCHEMA;
        default:
            throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Client.UNRECOGNISED_SESSION_TYPE);
    }
}
