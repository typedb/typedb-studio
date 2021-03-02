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
exports.ServerAddress = void 0;
const GraknClientError_1 = require("../../common/errors/GraknClientError");
const ErrorMessage_1 = require("../../common/errors/ErrorMessage");
var ILLEGAL_ARGUMENT = ErrorMessage_1.ErrorMessage.Internal.ILLEGAL_ARGUMENT;
class ServerAddress {
    constructor(externalHost, externalPort, internalHost, internalPort) {
        this._externalHost = externalHost;
        this._externalPort = externalPort;
        this._internalHost = internalHost;
        this._internalPort = internalPort;
    }
    static parse(address) {
        const s1 = address.split(",");
        if (s1.length == 1) {
            const s2 = address.split(":");
            return new ServerAddress(s2[0], parseInt(s2[1]), s2[0], parseInt(s2[2]));
        }
        else if (s1.length == 2) {
            const clientAddress = s1[0].split(":");
            const serverAddress = s1[1].split(":");
            if (clientAddress.length != 2 || serverAddress.length != 2)
                throw new GraknClientError_1.GraknClientError(ILLEGAL_ARGUMENT.message(address));
            return new ServerAddress(clientAddress[0], parseInt(clientAddress[1]), serverAddress[0], parseInt(serverAddress[1]));
        }
        else
            throw new GraknClientError_1.GraknClientError(ILLEGAL_ARGUMENT.message(address));
    }
    external() {
        return `${this._externalHost}:${this._externalPort}`;
    }
    externalHost() {
        return this._externalHost;
    }
    externalPort() {
        return this._externalPort;
    }
    internal() {
        return `${this._internalHost}:${this._internalPort}`;
    }
    internalHost() {
        return this._internalHost;
    }
    internalPort() {
        return this._internalPort;
    }
    toString() {
        return `${this.external()},${this.internal()}`;
    }
}
exports.ServerAddress = ServerAddress;
