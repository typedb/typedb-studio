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
exports.QueryManager = void 0;
const dependencies_internal_1 = require("../dependencies_internal");
const query_pb_1 = __importDefault(require("grakn-protocol/protobuf/query_pb"));
var Query = query_pb_1.default.Query;
const transaction_pb_1 = __importDefault(require("grakn-protocol/protobuf/transaction_pb"));
var Transaction = transaction_pb_1.default.Transaction;
class QueryManager {
    constructor(transaction) {
        this._transactionRPC = transaction;
    }
    match(query, options) {
        const matchQuery = new Query.Req().setMatchReq(new Query.Match.Req().setQuery(query));
        return this.iterateQuery(matchQuery, options ? options : dependencies_internal_1.GraknOptions.core(), (res) => res.getQueryRes().getMatchRes().getAnswersList().map(dependencies_internal_1.ConceptMap.of));
    }
    matchAggregate(query, options) {
        const matchAggregateQuery = new Query.Req().setMatchAggregateReq(new Query.MatchAggregate.Req().setQuery(query));
        return this.runQuery(matchAggregateQuery, options ? options : dependencies_internal_1.GraknOptions.core(), (res) => dependencies_internal_1.Numeric.of(res.getQueryRes().getMatchAggregateRes().getAnswer()));
    }
    matchGroup(query, options) {
        const matchGroupQuery = new Query.Req().setMatchGroupReq(new Query.MatchGroup.Req().setQuery(query));
        return this.iterateQuery(matchGroupQuery, options ? options : dependencies_internal_1.GraknOptions.core(), (res) => res.getQueryRes().getMatchGroupRes().getAnswersList().map(dependencies_internal_1.ConceptMapGroup.of));
    }
    matchGroupAggregate(query, options) {
        const matchGroupAggregateQuery = new Query.Req().setMatchGroupAggregateReq(new Query.MatchGroupAggregate.Req().setQuery(query));
        return this.iterateQuery(matchGroupAggregateQuery, options ? options : dependencies_internal_1.GraknOptions.core(), (res) => res.getQueryRes().getMatchGroupAggregateRes().getAnswersList().map(dependencies_internal_1.NumericGroup.of));
    }
    insert(query, options) {
        const insertQuery = new Query.Req().setInsertReq(new Query.Insert.Req().setQuery(query));
        return this.iterateQuery(insertQuery, options ? options : dependencies_internal_1.GraknOptions.core(), (res) => res.getQueryRes().getInsertRes().getAnswersList().map(dependencies_internal_1.ConceptMap.of));
    }
    delete(query, options) {
        const deleteQuery = new Query.Req().setDeleteReq(new Query.Delete.Req().setQuery(query));
        return this.runQuery(deleteQuery, options ? options : dependencies_internal_1.GraknOptions.core(), () => null);
    }
    update(query, options) {
        const updateQuery = new Query.Req().setUpdateReq(new Query.Update.Req().setQuery(query));
        return this.iterateQuery(updateQuery, options ? options : dependencies_internal_1.GraknOptions.core(), (res) => res.getQueryRes().getUpdateRes().getAnswersList().map(dependencies_internal_1.ConceptMap.of));
    }
    define(query, options) {
        const defineQuery = new Query.Req().setDefineReq(new Query.Define.Req().setQuery(query));
        return this.runQuery(defineQuery, options ? options : dependencies_internal_1.GraknOptions.core(), () => null);
    }
    undefine(query, options) {
        const undefineQuery = new Query.Req().setUndefineReq(new Query.Undefine.Req().setQuery(query));
        return this.runQuery(undefineQuery, options ? options : dependencies_internal_1.GraknOptions.core(), () => null);
    }
    iterateQuery(request, options, responseReader) {
        const transactionRequest = new Transaction.Req()
            .setQueryReq(request.setOptions(dependencies_internal_1.OptionsProtoBuilder.options(options)));
        return this._transactionRPC.stream(transactionRequest, responseReader);
    }
    async runQuery(request, options, mapper) {
        const transactionRequest = new Transaction.Req()
            .setQueryReq(request.setOptions(dependencies_internal_1.OptionsProtoBuilder.options(options)));
        return this._transactionRPC.execute(transactionRequest, mapper);
    }
}
exports.QueryManager = QueryManager;
