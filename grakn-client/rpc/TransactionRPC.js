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
exports.ResponseCollector = exports.TransactionRPC = void 0;
const dependencies_internal_1 = require("../dependencies_internal");
const transaction_pb_1 = __importDefault(require("grakn-protocol/protobuf/transaction_pb"));
class TransactionRPC {
    constructor(grpcClient, type) {
        this._type = type;
        this._conceptManager = new dependencies_internal_1.ConceptManager(this);
        this._logicManager = new dependencies_internal_1.LogicManager(this);
        this._queryManager = new dependencies_internal_1.QueryManager(this);
        this._collectors = new ResponseCollectors(this);
        this._isOpen = false;
        this._grpcClient = grpcClient;
    }
    async open(sessionId, options) {
        this.openTransactionStream();
        this._options = options;
        this._isOpen = true;
        const openRequest = new transaction_pb_1.default.Transaction.Req()
            .setOpenReq(new transaction_pb_1.default.Transaction.Open.Req()
            .setSessionId(sessionId)
            .setType(this._type === dependencies_internal_1.TransactionType.READ ? transaction_pb_1.default.Transaction.Type.READ : transaction_pb_1.default.Transaction.Type.WRITE)
            .setOptions(dependencies_internal_1.OptionsProtoBuilder.options(options)));
        const startTime = new Date().getTime();
        const res = await this.execute(openRequest, res => res.getOpenRes());
        const endTime = new Date().getTime();
        this._networkLatencyMillis = endTime - startTime - res.getProcessingTimeMillis();
        return this;
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
    concepts() {
        return this._conceptManager;
    }
    logic() {
        return this._logicManager;
    }
    query() {
        return this._queryManager;
    }
    async commit() {
        const commitReq = new transaction_pb_1.default.Transaction.Req()
            .setCommitReq(new transaction_pb_1.default.Transaction.Commit.Req());
        try {
            await this.execute(commitReq);
        }
        finally {
            await this.close();
        }
    }
    async rollback() {
        const rollbackReq = new transaction_pb_1.default.Transaction.Req()
            .setRollbackReq(new transaction_pb_1.default.Transaction.Rollback.Req());
        await this.execute(rollbackReq);
    }
    async close() {
        if (this._isOpen) {
            this._isOpen = false;
            this._collectors.clearWithError(new ErrorResponse(new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Client.TRANSACTION_CLOSED)));
            this._stream.end();
        }
    }
    execute(request, transformResponse = () => null) {
        const responseCollector = new ResponseCollector();
        const requestId = dependencies_internal_1.uuidv4();
        request.setId(requestId);
        this._collectors.put(requestId, responseCollector);
        this._stream.write(request);
        return responseCollector.take().then(transformResponse);
    }
    stream(request, transformResponse) {
        const responseCollector = new ResponseCollector();
        const requestId = dependencies_internal_1.uuidv4();
        request.setId(requestId);
        request.setLatencyMillis(this._networkLatencyMillis);
        this._collectors.put(requestId, responseCollector);
        this._stream.write(request);
        return new dependencies_internal_1.Stream(requestId, this._stream, responseCollector, transformResponse);
    }
    openTransactionStream() {
        this._stream = this._grpcClient.transaction();
        this._stream.on("data", (res) => {
            const requestId = res.getId();
            const collector = this._collectors.get(requestId);
            if (!collector)
                throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Client.UNKNOWN_REQUEST_ID.message(requestId));
            collector.add(new OkResponse(res));
        });
        this._stream.on("error", (err) => {
            this._collectors.clearWithError(new ErrorResponse(err));
            this.close();
        });
        this._stream.on("end", () => {
            this.close();
        });
        // TODO: look into _stream.on(status) + any other events
    }
}
exports.TransactionRPC = TransactionRPC;
class ResponseCollectors {
    constructor(transaction) {
        this._map = {};
        this._transaction = transaction;
    }
    get(uuid) {
        return this._map[uuid];
    }
    put(uuid, collector) {
        if (!this._transaction.isOpen())
            throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Client.TRANSACTION_CLOSED);
        this._map[uuid] = collector;
    }
    clearWithError(error) {
        Object.keys(this._map).forEach((requestId) => this._map[requestId].add(error));
        for (const requestId in this._map)
            delete this._map[requestId];
    }
}
class ResponseCollector {
    constructor() {
        this._responseBuffer = new dependencies_internal_1.BlockingQueue();
    }
    add(response) {
        this._responseBuffer.add(response);
    }
    async take() {
        const response = await this._responseBuffer.take();
        return response.read();
    }
}
exports.ResponseCollector = ResponseCollector;
class Response {
}
class OkResponse extends Response {
    constructor(res) {
        super();
        this._res = res;
    }
    read() {
        return this._res;
    }
    toString() {
        return "OkResponse {" + this._res.toString() + "}";
    }
}
class ErrorResponse extends Response {
    constructor(error) {
        super();
        this._error = error;
    }
    read() {
        throw this._error;
    }
    toString() {
        return "ErrorResponse {" + this._error.toString() + "}";
    }
}
