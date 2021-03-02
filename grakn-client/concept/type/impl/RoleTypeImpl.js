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
exports.RemoteRoleTypeImpl = exports.RoleTypeImpl = void 0;
const dependencies_internal_1 = require("../../../dependencies_internal");
const concept_pb_1 = __importDefault(require("grakn-protocol/protobuf/concept_pb"));
class RoleTypeImpl extends dependencies_internal_1.TypeImpl {
    constructor(label, scope, isRoot) {
        super(label, isRoot);
        this._scope = scope;
    }
    static of(typeProto) {
        return new RoleTypeImpl(typeProto.getLabel(), typeProto.getScope(), typeProto.getRoot());
    }
    getScope() {
        return this._scope;
    }
    getScopedLabel() {
        return `${this._scope}:${this.getLabel()}`;
    }
    asRemote(transaction) {
        return new RemoteRoleTypeImpl(transaction, this.getLabel(), this.getScope(), this.isRoot());
    }
    toString() {
        return `${this.constructor.name}[label: ${this._scope ? `${this._scope}:${this.getLabel()}` : this.getLabel()}]`;
    }
    isRoleType() {
        return true;
    }
}
exports.RoleTypeImpl = RoleTypeImpl;
class RemoteRoleTypeImpl extends dependencies_internal_1.RemoteTypeImpl {
    constructor(transaction, label, scope, isRoot) {
        super(transaction, label, isRoot);
        this._scope = scope;
    }
    getScope() {
        return this._scope;
    }
    getScopedLabel() {
        return `${this._scope}:${this.getLabel()}`;
    }
    getSupertype() {
        return super.getSupertype();
    }
    getSupertypes() {
        return super.getSupertypes();
    }
    getSubtypes() {
        return super.getSubtypes();
    }
    asRemote(transaction) {
        return new RemoteRoleTypeImpl(transaction, this.getLabel(), this._scope, this.isRoot());
    }
    async getRelationType() {
        const method = new concept_pb_1.default.Type.Req().setRoleTypeGetRelationTypeReq(new concept_pb_1.default.RoleType.GetRelationType.Req());
        const response = (await this.execute(method)).getRoleTypeGetRelationTypeRes();
        return dependencies_internal_1.TypeImpl.of(response.getRelationType());
    }
    getRelationTypes() {
        return this.typeStream(new concept_pb_1.default.Type.Req().setRoleTypeGetRelationTypesReq(new concept_pb_1.default.RoleType.GetRelationTypes.Req()), res => res.getRoleTypeGetRelationTypesRes().getRelationTypesList());
    }
    getPlayers() {
        return this.typeStream(new concept_pb_1.default.Type.Req().setRoleTypeGetPlayersReq(new concept_pb_1.default.RoleType.GetPlayers.Req()), res => res.getRoleTypeGetPlayersRes().getThingTypesList());
    }
    typeStream(method, typeGetter) {
        return super.typeStream(method.setScope(this._scope), typeGetter);
    }
    execute(method) {
        return super.execute(method.setScope(this._scope));
    }
    toString() {
        return `${this.constructor.name}[label: ${this._scope ? `${this._scope}:${this.getLabel()}` : this.getLabel()}]`;
    }
    isRoleType() {
        return true;
    }
}
exports.RemoteRoleTypeImpl = RemoteRoleTypeImpl;
