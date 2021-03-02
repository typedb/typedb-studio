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
exports.RemoteTypeImpl = exports.TypeImpl = void 0;
const dependencies_internal_1 = require("../../../dependencies_internal");
const concept_pb_1 = __importDefault(require("grakn-protocol/protobuf/concept_pb"));
const transaction_pb_1 = __importDefault(require("grakn-protocol/protobuf/transaction_pb"));
class TypeImpl extends dependencies_internal_1.ConceptImpl {
    constructor(label, root) {
        super();
        if (!label)
            throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.MISSING_LABEL.message());
        this._label = label;
        this._root = root;
    }
    getLabel() {
        return this._label;
    }
    isRoot() {
        return this._root;
    }
    isType() {
        return true;
    }
    isRemote() {
        return false;
    }
    toString() {
        return `${this.constructor.name}[label:${this._label}]`;
    }
    equals(concept) {
        if (!(concept === null || concept === void 0 ? void 0 : concept.isType()))
            return false;
        return concept.getLabel() === this.getLabel();
    }
}
exports.TypeImpl = TypeImpl;
class RemoteTypeImpl extends dependencies_internal_1.RemoteConceptImpl {
    constructor(transaction, label, isRoot) {
        super();
        if (!transaction)
            throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.MISSING_TRANSACTION.message());
        if (!label)
            throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.MISSING_LABEL.message());
        this._rpcTransaction = transaction;
        this._label = label;
        this._isRoot = isRoot;
    }
    getLabel() {
        return this._label;
    }
    isRoot() {
        return this._isRoot;
    }
    isType() {
        return true;
    }
    isRemote() {
        return true;
    }
    equals(concept) {
        if (!concept.isType())
            return false;
        return concept.getLabel() === this.getLabel();
    }
    async setLabel(label) {
        await this.execute(new concept_pb_1.default.Type.Req()
            .setTypeSetLabelReq(new concept_pb_1.default.Type.SetLabel.Req().setLabel(label)));
        this._label = label;
    }
    async isAbstract() {
        return (await this.execute(new concept_pb_1.default.Type.Req()
            .setTypeIsAbstractReq(new concept_pb_1.default.Type.IsAbstract.Req())))
            .getTypeIsAbstractRes().getAbstract();
    }
    async setSupertype(type) {
        await this.execute(new concept_pb_1.default.Type.Req()
            .setTypeSetSupertypeReq(new concept_pb_1.default.Type.SetSupertype.Req().setType(dependencies_internal_1.ConceptProtoBuilder.type(type))));
    }
    async getSupertype() {
        const response = (await this.execute(new concept_pb_1.default.Type.Req()
            .setTypeGetSupertypeReq(new concept_pb_1.default.Type.GetSupertype.Req())))
            .getTypeGetSupertypeRes();
        return response.getResCase() === concept_pb_1.default.Type.GetSupertype.Res.ResCase.TYPE ? TypeImpl.of(response.getType()) : null;
    }
    getSupertypes() {
        const method = new concept_pb_1.default.Type.Req().setTypeGetSupertypesReq(new concept_pb_1.default.Type.GetSupertypes.Req());
        return this.typeStream(method, res => res.getTypeGetSupertypesRes().getTypesList());
    }
    getSubtypes() {
        const method = new concept_pb_1.default.Type.Req().setTypeGetSubtypesReq(new concept_pb_1.default.Type.GetSubtypes.Req());
        return this.typeStream(method, res => res.getTypeGetSubtypesRes().getTypesList());
    }
    async delete() {
        await this.execute(new concept_pb_1.default.Type.Req().setTypeDeleteReq(new concept_pb_1.default.Type.Delete.Req()));
    }
    async isDeleted() {
        return !(await this._rpcTransaction.concepts().getThingType(this._label));
    }
    get transaction() {
        return this._rpcTransaction;
    }
    typeStream(method, typeGetter) {
        const request = new transaction_pb_1.default.Transaction.Req().setTypeReq(method.setLabel(this._label));
        return this._rpcTransaction.stream(request, res => typeGetter(res.getTypeRes()).map(TypeImpl.of));
    }
    thingStream(method, thingGetter) {
        const request = new transaction_pb_1.default.Transaction.Req().setTypeReq(method.setLabel(this._label));
        return this._rpcTransaction.stream(request, res => thingGetter(res.getTypeRes()).map(dependencies_internal_1.ThingImpl.of));
    }
    execute(method) {
        const request = new transaction_pb_1.default.Transaction.Req().setTypeReq(method.setLabel(this._label));
        return this._rpcTransaction.execute(request, res => res.getTypeRes());
    }
    toString() {
        return `${this.constructor.name}[label:${this._label}]`;
    }
}
exports.RemoteTypeImpl = RemoteTypeImpl;
(function (TypeImpl) {
    function of(typeProto) {
        switch (typeProto.getEncoding()) {
            case concept_pb_1.default.Type.Encoding.ROLE_TYPE:
                return dependencies_internal_1.RoleTypeImpl.of(typeProto);
            default:
                return dependencies_internal_1.ThingTypeImpl.of(typeProto);
        }
    }
    TypeImpl.of = of;
})(TypeImpl = exports.TypeImpl || (exports.TypeImpl = {}));
