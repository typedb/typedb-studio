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
exports.TransactionType = exports.SessionType = exports.GraknClient = void 0;
const dependencies_internal_1 = require("./dependencies_internal");
var GraknClient;
(function (GraknClient) {
    GraknClient.DEFAULT_ADDRESS = "localhost:1729";
    function core(address = GraknClient.DEFAULT_ADDRESS) {
        return new dependencies_internal_1.ClientRPC(address);
    }
    GraknClient.core = core;
    function cluster(addresses) {
        return new dependencies_internal_1.ClientClusterRPC().open(addresses);
    }
    GraknClient.cluster = cluster;
})(GraknClient = exports.GraknClient || (exports.GraknClient = {}));
var SessionType;
(function (SessionType) {
    SessionType[SessionType["DATA"] = 0] = "DATA";
    SessionType[SessionType["SCHEMA"] = 1] = "SCHEMA";
})(SessionType = exports.SessionType || (exports.SessionType = {}));
var TransactionType;
(function (TransactionType) {
    TransactionType[TransactionType["READ"] = 0] = "READ";
    TransactionType[TransactionType["WRITE"] = 1] = "WRITE";
})(TransactionType = exports.TransactionType || (exports.TransactionType = {}));
