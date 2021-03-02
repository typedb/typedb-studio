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
exports.LogicManager = void 0;
const dependencies_internal_1 = require("../dependencies_internal");
const logic_pb_1 = __importDefault(require("grakn-protocol/protobuf/logic_pb"));
const transaction_pb_1 = __importDefault(require("grakn-protocol/protobuf/transaction_pb"));
class LogicManager {
    constructor(transactionRPC) {
        this._transactionRPC = transactionRPC;
    }
    async putRule(label, when, then) {
        const req = new logic_pb_1.default.LogicManager.Req()
            .setPutRuleReq(new logic_pb_1.default.LogicManager.PutRule.Req()
            .setLabel(label)
            .setWhen(when)
            .setThen(then));
        const res = await this.execute(req);
        return dependencies_internal_1.RuleImpl.of(res.getPutRuleRes().getRule());
    }
    async getRule(label) {
        const req = new logic_pb_1.default.LogicManager.Req()
            .setGetRuleReq(new logic_pb_1.default.LogicManager.GetRule.Req().setLabel(label));
        const res = await this.execute(req);
        if (res.getGetRuleRes().getResCase() === logic_pb_1.default.LogicManager.GetRule.Res.ResCase.RULE)
            return dependencies_internal_1.RuleImpl.of(res.getGetRuleRes().getRule());
        return null;
    }
    getRules() {
        const method = new logic_pb_1.default.LogicManager.Req().setGetRulesReq(new logic_pb_1.default.LogicManager.GetRules.Req());
        return this.ruleStream(method, res => res.getGetRulesRes().getRulesList());
    }
    async execute(logicManagerReq) {
        const transactionReq = new transaction_pb_1.default.Transaction.Req()
            .setLogicManagerReq(logicManagerReq);
        return await this._transactionRPC.execute(transactionReq, res => res.getLogicManagerRes());
    }
    ruleStream(method, ruleListGetter) {
        const request = new transaction_pb_1.default.Transaction.Req().setLogicManagerReq(method);
        return this._transactionRPC.stream(request, res => ruleListGetter(res.getLogicManagerRes()).map(dependencies_internal_1.RuleImpl.of));
    }
}
exports.LogicManager = LogicManager;
