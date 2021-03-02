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
exports.SessionClusterRPC = void 0;
const dependencies_internal_1 = require("../../dependencies_internal");
class SessionClusterRPC {
    constructor(clusterClient, serverAddress) {
        this._clusterClient = clusterClient;
        this._coreClient = clusterClient.coreClient(serverAddress.toString());
    }
    async open(serverAddress, database, type, options) {
        console.info(`Opening a session to '${serverAddress}'`);
        this._coreSession = await this._coreClient.session(database, type, options);
        this._options = options;
        return this;
    }
    transaction(type, options = dependencies_internal_1.GraknOptions.cluster()) {
        if (options.readAnyReplica) {
            return this.transactionAnyReplica(type, options);
        }
        else {
            return this.transactionPrimaryReplica(type, options);
        }
    }
    transactionPrimaryReplica(type, options) {
        return new TransactionFailsafeTask(this, type, options).runPrimaryReplica();
    }
    transactionAnyReplica(type, options) {
        return new TransactionFailsafeTask(this, type, options).runAnyReplica();
    }
    type() {
        return this._coreSession.type();
    }
    isOpen() {
        return this._coreSession.isOpen();
    }
    options() {
        return this._options;
    }
    close() {
        return this._coreSession.close();
    }
    database() {
        return this._coreSession.database();
    }
    get clusterClient() {
        return this._clusterClient;
    }
    get coreClient() {
        return this._coreClient;
    }
    set coreClient(client) {
        this._coreClient = client;
    }
    get coreSession() {
        return this._coreSession;
    }
    set coreSession(session) {
        this._coreSession = session;
    }
}
exports.SessionClusterRPC = SessionClusterRPC;
class TransactionFailsafeTask extends dependencies_internal_1.FailsafeTask {
    constructor(clusterSession, type, options) {
        super(clusterSession.clusterClient, clusterSession.database().name());
        this._clusterSession = clusterSession;
        this._type = type;
        this._options = options;
    }
    run(replica) {
        return this._clusterSession.coreSession.transaction(this._type, this._options);
    }
    async rerun(replica) {
        if (this._clusterSession.coreSession)
            await this._clusterSession.coreSession.close();
        this._clusterSession.coreClient = this._clusterSession.clusterClient.coreClient(replica.address().toString());
        this._clusterSession.coreSession = await this._clusterSession.coreClient.session(this.database, this._clusterSession.type(), this._clusterSession.options());
        return await this._clusterSession.coreSession.transaction(this._type, this._options);
    }
}
