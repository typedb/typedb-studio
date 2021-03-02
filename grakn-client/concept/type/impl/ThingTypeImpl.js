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
exports.RemoteThingTypeImpl = exports.ThingTypeImpl = void 0;
const dependencies_internal_1 = require("../../../dependencies_internal");
const concept_pb_1 = __importDefault(require("grakn-protocol/protobuf/concept_pb"));
const assert_1 = __importDefault(require("assert"));
class ThingTypeImpl extends dependencies_internal_1.TypeImpl {
    constructor(label, isRoot) {
        super(label, isRoot);
    }
    asRemote(transaction) {
        return new RemoteThingTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
    isThingType() {
        return true;
    }
}
exports.ThingTypeImpl = ThingTypeImpl;
class RemoteThingTypeImpl extends dependencies_internal_1.RemoteTypeImpl {
    constructor(transaction, label, isRoot) {
        super(transaction, label, isRoot);
    }
    isThingType() {
        return true;
    }
    setSupertype(thingType) {
        return super.setSupertype(thingType);
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
    getInstances() {
        const request = new concept_pb_1.default.Type.Req().setThingTypeGetInstancesReq(new concept_pb_1.default.ThingType.GetInstances.Req());
        return this.thingStream(request, res => res.getThingTypeGetInstancesRes().getThingsList());
    }
    async setAbstract() {
        await this.execute(new concept_pb_1.default.Type.Req().setThingTypeSetAbstractReq(new concept_pb_1.default.ThingType.SetAbstract.Req()));
    }
    async unsetAbstract() {
        await this.execute(new concept_pb_1.default.Type.Req().setThingTypeUnsetAbstractReq(new concept_pb_1.default.ThingType.UnsetAbstract.Req()));
    }
    async setPlays(role, overriddenType) {
        const setPlaysReq = new concept_pb_1.default.ThingType.SetPlays.Req().setRole(dependencies_internal_1.ConceptProtoBuilder.type(role));
        if (overriddenType)
            setPlaysReq.setOverriddenRole(dependencies_internal_1.ConceptProtoBuilder.type(overriddenType));
        await this.execute(new concept_pb_1.default.Type.Req().setThingTypeSetPlaysReq(setPlaysReq));
    }
    async setOwns(attributeType, isKeyOrOverriddenType, isKey) {
        const setOwnsReq = new concept_pb_1.default.ThingType.SetOwns.Req().setAttributeType(dependencies_internal_1.ConceptProtoBuilder.type(attributeType))
            .setIsKey(typeof isKeyOrOverriddenType === "boolean" ? isKeyOrOverriddenType : typeof isKey === "boolean" ? isKey : false);
        if (isKeyOrOverriddenType instanceof dependencies_internal_1.AttributeTypeImpl)
            setOwnsReq.setOverriddenType(dependencies_internal_1.ConceptProtoBuilder.type(isKeyOrOverriddenType));
        await this.execute(new concept_pb_1.default.Type.Req().setThingTypeSetOwnsReq(setOwnsReq));
    }
    getPlays() {
        const request = new concept_pb_1.default.Type.Req().setThingTypeGetPlaysReq(new concept_pb_1.default.ThingType.GetPlays.Req());
        return this.typeStream(request, res => res.getThingTypeGetPlaysRes().getRolesList());
    }
    getOwns(valueTypeOrKeysOnly, keysOnly) {
        const getOwnsReq = new concept_pb_1.default.ThingType.GetOwns.Req()
            .setKeysOnly(typeof valueTypeOrKeysOnly === "boolean" ? valueTypeOrKeysOnly : typeof keysOnly === "boolean" ? keysOnly : false);
        // Here we take advantage of the fact that AttributeType.ValueType is a string enum
        if (typeof valueTypeOrKeysOnly === "string")
            getOwnsReq.setValueType(dependencies_internal_1.ConceptProtoBuilder.valueType(valueTypeOrKeysOnly));
        const request = new concept_pb_1.default.Type.Req().setThingTypeGetOwnsReq(getOwnsReq);
        return this.typeStream(request, res => res.getThingTypeGetOwnsRes().getAttributeTypesList());
    }
    async unsetPlays(role) {
        await this.execute(new concept_pb_1.default.Type.Req().setThingTypeUnsetPlaysReq(new concept_pb_1.default.ThingType.UnsetPlays.Req().setRole(dependencies_internal_1.ConceptProtoBuilder.type(role))));
    }
    async unsetOwns(attributeType) {
        await this.execute(new concept_pb_1.default.Type.Req().setThingTypeUnsetOwnsReq(new concept_pb_1.default.ThingType.UnsetOwns.Req().setAttributeType(dependencies_internal_1.ConceptProtoBuilder.type(attributeType))));
    }
    asRemote(transaction) {
        return new RemoteThingTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
}
exports.RemoteThingTypeImpl = RemoteThingTypeImpl;
(function (ThingTypeImpl) {
    function of(typeProto) {
        switch (typeProto.getEncoding()) {
            case concept_pb_1.default.Type.Encoding.ENTITY_TYPE:
                return dependencies_internal_1.EntityTypeImpl.of(typeProto);
            case concept_pb_1.default.Type.Encoding.RELATION_TYPE:
                return dependencies_internal_1.RelationTypeImpl.of(typeProto);
            case concept_pb_1.default.Type.Encoding.ATTRIBUTE_TYPE:
                return dependencies_internal_1.AttributeTypeImpl.of(typeProto);
            case concept_pb_1.default.Type.Encoding.THING_TYPE:
                assert_1.default(typeProto.getRoot());
                return new ThingTypeImpl(typeProto.getLabel(), typeProto.getRoot());
            default:
                throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.BAD_ENCODING.message(typeProto.getEncoding()));
        }
    }
    ThingTypeImpl.of = of;
})(ThingTypeImpl = exports.ThingTypeImpl || (exports.ThingTypeImpl = {}));
