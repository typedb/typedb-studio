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
exports.RemoteRelationTypeImpl = exports.RelationTypeImpl = void 0;
const dependencies_internal_1 = require("../../../dependencies_internal");
const concept_pb_1 = __importDefault(require("grakn-protocol/protobuf/concept_pb"));
class RelationTypeImpl extends dependencies_internal_1.ThingTypeImpl {
    constructor(label, isRoot) {
        super(label, isRoot);
    }
    static of(typeProto) {
        return new RelationTypeImpl(typeProto.getLabel(), typeProto.getRoot());
    }
    asRemote(transaction) {
        return new RemoteRelationTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
    isRelationType() {
        return true;
    }
}
exports.RelationTypeImpl = RelationTypeImpl;
class RemoteRelationTypeImpl extends dependencies_internal_1.RemoteThingTypeImpl {
    constructor(transaction, label, isRoot) {
        super(transaction, label, isRoot);
    }
    asRemote(transaction) {
        return new RemoteRelationTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
    isRelationType() {
        return true;
    }
    create() {
        const method = new concept_pb_1.default.Type.Req().setRelationTypeCreateReq(new concept_pb_1.default.RelationType.Create.Req());
        return this.execute(method).then(res => dependencies_internal_1.RelationImpl.of(res.getRelationTypeCreateRes().getRelation()));
    }
    getRelates(roleLabel) {
        if (roleLabel != null) {
            const method = new concept_pb_1.default.Type.Req().setRelationTypeGetRelatesForRoleLabelReq(new concept_pb_1.default.RelationType.GetRelatesForRoleLabel.Req().setLabel(roleLabel));
            return this.execute(method).then(res => {
                const getRelatesRes = res.getRelationTypeGetRelatesForRoleLabelRes();
                if (getRelatesRes.hasRoleType())
                    return dependencies_internal_1.TypeImpl.of(getRelatesRes.getRoleType());
                else
                    return null;
            });
        }
        return this.typeStream(new concept_pb_1.default.Type.Req().setRelationTypeGetRelatesReq(new concept_pb_1.default.RelationType.GetRelates.Req()), res => res.getRelationTypeGetRelatesRes().getRolesList());
    }
    async setRelates(roleLabel, overriddenLabel) {
        const setRelatesReq = new concept_pb_1.default.RelationType.SetRelates.Req().setLabel(roleLabel);
        if (overriddenLabel != null)
            setRelatesReq.setOverriddenLabel(overriddenLabel);
        await this.execute(new concept_pb_1.default.Type.Req().setRelationTypeSetRelatesReq(setRelatesReq));
    }
    async unsetRelates(roleLabel) {
        await this.execute(new concept_pb_1.default.Type.Req()
            .setRelationTypeUnsetRelatesReq(new concept_pb_1.default.RelationType.UnsetRelates.Req().setLabel(roleLabel)));
    }
    setSupertype(relationType) {
        return super.setSupertype(relationType);
    }
    getSubtypes() {
        return super.getSubtypes();
    }
    getInstances() {
        return super.getInstances();
    }
}
exports.RemoteRelationTypeImpl = RemoteRelationTypeImpl;
