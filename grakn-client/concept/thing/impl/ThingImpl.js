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
exports.RemoteThingImpl = exports.ThingImpl = void 0;
const dependencies_internal_1 = require("../../../dependencies_internal");
const concept_pb_1 = __importDefault(require("grakn-protocol/protobuf/concept_pb"));
const transaction_pb_1 = __importDefault(require("grakn-protocol/protobuf/transaction_pb"));
class ThingImpl extends dependencies_internal_1.ConceptImpl {
    constructor(iid) {
        super();
        if (!iid)
            throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.MISSING_IID.message());
        this._iid = iid;
    }
    getIID() {
        return this._iid;
    }
    isRemote() {
        return false;
    }
    isThing() {
        return true;
    }
    toString() {
        return `${ThingImpl.name}[iid:${this._iid}]`;
    }
    equals(concept) {
        if (!(concept === null || concept === void 0 ? void 0 : concept.isThing()))
            return false;
        return concept.getIID() === this.getIID();
    }
}
exports.ThingImpl = ThingImpl;
class RemoteThingImpl extends dependencies_internal_1.RemoteConceptImpl {
    constructor(transaction, iid) {
        super();
        if (!transaction)
            throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.MISSING_TRANSACTION.message());
        if (!iid)
            throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.MISSING_IID.message());
        this._iid = iid;
        this._transactionRPC = transaction;
    }
    getIID() {
        return this._iid;
    }
    async isInferred() {
        return (await this.execute(new concept_pb_1.default.Thing.Req().setThingIsInferredReq(new concept_pb_1.default.Thing.IsInferred.Req()))).getThingIsInferredRes().getInferred();
    }
    isRemote() {
        return true;
    }
    isThing() {
        return true;
    }
    equals(concept) {
        if (!concept.isThing())
            return false;
        return concept.getIID() === this.getIID();
    }
    getHas(arg) {
        if (typeof arg === "undefined") {
            const method = new concept_pb_1.default.Thing.Req().setThingGetHasReq(new concept_pb_1.default.Thing.GetHas.Req());
            return this.thingStream(method, res => res.getThingGetHasRes().getAttributesList());
        }
        if (typeof arg === "boolean") {
            const method = new concept_pb_1.default.Thing.Req().setThingGetHasReq(new concept_pb_1.default.Thing.GetHas.Req().setKeysOnly(arg));
            return this.thingStream(method, res => res.getThingGetHasRes().getAttributesList());
        }
        if (Array.isArray(arg)) {
            const method = new concept_pb_1.default.Thing.Req()
                .setThingGetHasReq(new concept_pb_1.default.Thing.GetHas.Req().setAttributeTypesList(dependencies_internal_1.ConceptProtoBuilder.types(arg)));
            return this.thingStream(method, res => res.getThingGetHasRes().getAttributesList());
        }
        const method = new concept_pb_1.default.Thing.Req()
            .setThingGetHasReq(new concept_pb_1.default.Thing.GetHas.Req().setAttributeTypesList([dependencies_internal_1.ConceptProtoBuilder.type(arg)]));
        const stream = this.thingStream(method, res => res.getThingGetHasRes().getAttributesList());
        if (arg instanceof dependencies_internal_1.BooleanAttributeTypeImpl)
            return stream;
        if (arg instanceof dependencies_internal_1.LongAttributeTypeImpl)
            return stream;
        if (arg instanceof dependencies_internal_1.DoubleAttributeTypeImpl)
            return stream;
        if (arg instanceof dependencies_internal_1.StringAttributeTypeImpl)
            return stream;
        if (arg instanceof dependencies_internal_1.DateTimeAttributeTypeImpl)
            return stream;
        throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.BAD_VALUE_TYPE.message(arg));
    }
    getPlays() {
        const method = new concept_pb_1.default.Thing.Req().setThingGetPlaysReq(new concept_pb_1.default.Thing.GetPlays.Req());
        return this.typeStream(method, res => res.getThingGetPlaysRes().getRoleTypesList());
    }
    getRelations(roleTypes = []) {
        const method = new concept_pb_1.default.Thing.Req().setThingGetRelationsReq(new concept_pb_1.default.Thing.GetRelations.Req().setRoleTypesList(dependencies_internal_1.ConceptProtoBuilder.types(roleTypes)));
        return this.thingStream(method, res => res.getThingGetRelationsRes().getRelationsList());
    }
    async setHas(attribute) {
        await this.execute(new concept_pb_1.default.Thing.Req().setThingSetHasReq(new concept_pb_1.default.Thing.SetHas.Req().setAttribute(dependencies_internal_1.ConceptProtoBuilder.thing(attribute))));
    }
    async unsetHas(attribute) {
        await this.execute(new concept_pb_1.default.Thing.Req().setThingUnsetHasReq(new concept_pb_1.default.Thing.UnsetHas.Req().setAttribute(dependencies_internal_1.ConceptProtoBuilder.thing(attribute))));
    }
    async delete() {
        await this.execute(new concept_pb_1.default.Thing.Req().setThingDeleteReq(new concept_pb_1.default.Thing.Delete.Req()));
    }
    async isDeleted() {
        return !(await this._transactionRPC.concepts().getThing(this._iid));
    }
    get transaction() {
        return this._transactionRPC;
    }
    typeStream(method, typeGetter) {
        const request = new transaction_pb_1.default.Transaction.Req().setThingReq(method.setIid(dependencies_internal_1.Bytes.hexStringToBytes(this._iid)));
        return (this._transactionRPC).stream(request, res => typeGetter(res.getThingRes()).map(dependencies_internal_1.TypeImpl.of));
    }
    thingStream(method, thingGetter) {
        const request = new transaction_pb_1.default.Transaction.Req().setThingReq(method.setIid(dependencies_internal_1.Bytes.hexStringToBytes(this._iid)));
        return this._transactionRPC.stream(request, res => thingGetter(res.getThingRes()).map(ThingImpl.of));
    }
    execute(method) {
        const request = new transaction_pb_1.default.Transaction.Req().setThingReq(method.setIid(dependencies_internal_1.Bytes.hexStringToBytes(this._iid)));
        return this._transactionRPC.execute(request, res => res.getThingRes());
    }
    toString() {
        return `${RemoteThingImpl.name}[iid:${this._iid}]`;
    }
}
exports.RemoteThingImpl = RemoteThingImpl;
(function (ThingImpl) {
    function of(thingProto) {
        switch (thingProto.getType().getEncoding()) {
            case concept_pb_1.default.Type.Encoding.ENTITY_TYPE:
                return dependencies_internal_1.EntityImpl.of(thingProto);
            case concept_pb_1.default.Type.Encoding.RELATION_TYPE:
                return dependencies_internal_1.RelationImpl.of(thingProto);
            case concept_pb_1.default.Type.Encoding.ATTRIBUTE_TYPE:
                return dependencies_internal_1.AttributeImpl.of(thingProto);
            default:
                throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.BAD_ENCODING.message(thingProto.getType().getEncoding()));
        }
    }
    ThingImpl.of = of;
})(ThingImpl = exports.ThingImpl || (exports.ThingImpl = {}));
