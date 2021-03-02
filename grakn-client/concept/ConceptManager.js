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
exports.ConceptManager = void 0;
const dependencies_internal_1 = require("../dependencies_internal");
const concept_pb_1 = __importDefault(require("grakn-protocol/protobuf/concept_pb"));
const transaction_pb_1 = __importDefault(require("grakn-protocol/protobuf/transaction_pb"));
class ConceptManager {
    constructor(transactionRPC) {
        this._transactionRPC = transactionRPC;
    }
    async getRootThingType() {
        return await this.getThingType("thing");
    }
    async getRootEntityType() {
        return await this.getEntityType("entity");
    }
    async getRootRelationType() {
        return await this.getRelationType("relation");
    }
    async getRootAttributeType() {
        return await this.getAttributeType("attribute");
    }
    async putEntityType(label) {
        const req = new concept_pb_1.default.ConceptManager.Req()
            .setPutEntityTypeReq(new concept_pb_1.default.ConceptManager.PutEntityType.Req().setLabel(label));
        const res = await this.execute(req);
        return dependencies_internal_1.EntityTypeImpl.of(res.getPutEntityTypeRes().getEntityType());
    }
    async getEntityType(label) {
        const type = await this.getThingType(label);
        if (type && type.isEntityType())
            return type;
        else
            return null;
    }
    async putRelationType(label) {
        const req = new concept_pb_1.default.ConceptManager.Req()
            .setPutRelationTypeReq(new concept_pb_1.default.ConceptManager.PutRelationType.Req().setLabel(label));
        const res = await this.execute(req);
        return dependencies_internal_1.RelationTypeImpl.of(res.getPutRelationTypeRes().getRelationType());
    }
    async getRelationType(label) {
        const type = await this.getThingType(label);
        if (type && type.isRelationType())
            return type;
        else
            return null;
    }
    async putAttributeType(label, valueType) {
        const req = new concept_pb_1.default.ConceptManager.Req()
            .setPutAttributeTypeReq(new concept_pb_1.default.ConceptManager.PutAttributeType.Req()
            .setLabel(label)
            .setValueType(dependencies_internal_1.ConceptProtoBuilder.valueType(valueType)));
        const res = await this.execute(req);
        return dependencies_internal_1.AttributeTypeImpl.of(res.getPutAttributeTypeRes().getAttributeType());
    }
    async getAttributeType(label) {
        const type = await this.getThingType(label);
        if (type && type.isAttributeType())
            return type;
        else
            return null;
    }
    async getThing(iid) {
        const req = new concept_pb_1.default.ConceptManager.Req()
            .setGetThingReq(new concept_pb_1.default.ConceptManager.GetThing.Req().setIid(dependencies_internal_1.Bytes.hexStringToBytes(iid)));
        const res = await this.execute(req);
        if (res.getGetThingRes().getResCase() === concept_pb_1.default.ConceptManager.GetThing.Res.ResCase.THING)
            return dependencies_internal_1.ThingImpl.of(res.getGetThingRes().getThing());
        else
            return null;
    }
    async getThingType(label) {
        const req = new concept_pb_1.default.ConceptManager.Req()
            .setGetThingTypeReq(new concept_pb_1.default.ConceptManager.GetThingType.Req().setLabel(label));
        const res = await this.execute(req);
        if (res.getGetThingTypeRes().getResCase() === concept_pb_1.default.ConceptManager.GetThingType.Res.ResCase.THING_TYPE)
            return dependencies_internal_1.ThingTypeImpl.of(res.getGetThingTypeRes().getThingType());
        else
            return null;
    }
    async execute(conceptManagerReq) {
        const transactionReq = new transaction_pb_1.default.Transaction.Req()
            .setConceptManagerReq(conceptManagerReq);
        return await this._transactionRPC.execute(transactionReq, res => res.getConceptManagerRes());
    }
}
exports.ConceptManager = ConceptManager;
