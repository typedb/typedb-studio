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
exports.Stream = void 0;
const transaction_pb_1 = __importDefault(require("grakn-protocol/protobuf/transaction_pb"));
const dependencies_internal_1 = require("../dependencies_internal");
class Stream {
    constructor(requestId, writableStream, responseCollector, transformResponse) {
        this._requestId = requestId;
        this._transformResponse = transformResponse;
        this._writableStream = writableStream;
        this._responseCollector = responseCollector;
    }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    async *[Symbol.asyncIterator]() {
        while (true) {
            const next = await this.next();
            if (next != null)
                yield next;
            else
                break;
        }
    }
    async next() {
        var _a;
        if ((_a = this._receivedAnswers) === null || _a === void 0 ? void 0 : _a.length) {
            return this._receivedAnswers.shift();
        }
        const res = await this._responseCollector.take();
        switch (res.getResCase()) {
            case transaction_pb_1.default.Transaction.Res.ResCase.CONTINUE:
                this._writableStream.write(new transaction_pb_1.default.Transaction.Req()
                    .setId(this._requestId).setContinue(true));
                return this.next();
            case transaction_pb_1.default.Transaction.Res.ResCase.DONE:
                return undefined;
            case transaction_pb_1.default.Transaction.Res.ResCase.RES_NOT_SET:
                throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Client.MISSING_RESPONSE.message());
            default:
                this._receivedAnswers = this._transformResponse(res);
                return this.next();
        }
    }
    async collect() {
        const answers = [];
        for await (const answer of this) {
            answers.push(answer);
        }
        return answers;
    }
    async every(callbackFn) {
        for await (const item of this) {
            if (!callbackFn(item))
                return false;
        }
        return true;
    }
    filter(callbackFn) {
        return new Stream(this._requestId, this._writableStream, this._responseCollector, res => this._transformResponse(res).filter(callbackFn));
    }
    map(callbackFn) {
        return new Stream(this._requestId, this._writableStream, this._responseCollector, res => this._transformResponse(res).map(callbackFn));
    }
    async some(callbackFn) {
        for await (const item of this) {
            if (callbackFn(item))
                return true;
        }
        return false;
    }
}
exports.Stream = Stream;
