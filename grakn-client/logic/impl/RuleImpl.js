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
exports.RemoteRuleImpl = exports.RuleImpl = void 0;
const logic_pb_1 = __importDefault(require("grakn-protocol/protobuf/logic_pb"));
const transaction_pb_1 = __importDefault(require("grakn-protocol/protobuf/transaction_pb"));
class RuleImpl {
    constructor(label, when, then) {
        this._label = label;
        this._when = when;
        this._then = then;
    }
    static of(ruleProto) {
        return new RuleImpl(ruleProto.getLabel(), ruleProto.getWhen(), ruleProto.getThen());
    }
    getLabel() {
        return this._label;
    }
    getThen() {
        return this._then;
    }
    getWhen() {
        return this._when;
    }
    asRemote(transaction) {
        return new RemoteRuleImpl(transaction, this.getLabel(), this.getWhen(), this.getThen());
    }
    isRemote() {
        return false;
    }
    toString() {
        return `${this.constructor.name}[label:${this._label}]`;
    }
}
exports.RuleImpl = RuleImpl;
class RemoteRuleImpl {
    constructor(transaction, label, when, then) {
        this._rpcTransaction = transaction;
        this._label = label;
        this._when = when;
        this._then = then;
    }
    getLabel() {
        return this._label;
    }
    getThen() {
        return this._then;
    }
    getWhen() {
        return this._when;
    }
    async setLabel(label) {
        await this.execute(new logic_pb_1.default.Rule.Req().setRuleSetLabelReq(new logic_pb_1.default.Rule.SetLabel.Req().setLabel(label)));
        this._label = label;
    }
    async delete() {
        await this.execute(new logic_pb_1.default.Rule.Req().setRuleDeleteReq(new logic_pb_1.default.Rule.Delete.Req()));
    }
    async isDeleted() {
        return !(await this.rpcTransaction.logic().getRule(this._label));
    }
    asRemote(transaction) {
        return new RemoteRuleImpl(transaction, this.getLabel(), this.getWhen(), this.getThen());
    }
    isRemote() {
        return true;
    }
    execute(method) {
        const request = new transaction_pb_1.default.Transaction.Req().setRuleReq(method);
        return this._rpcTransaction.execute(request, res => res.getRuleRes());
    }
    get rpcTransaction() {
        return this._rpcTransaction;
    }
}
exports.RemoteRuleImpl = RemoteRuleImpl;
