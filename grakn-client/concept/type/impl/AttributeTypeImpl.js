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
exports.RemoteDateTimeAttributeTypeImpl = exports.DateTimeAttributeTypeImpl = exports.RemoteStringAttributeTypeImpl = exports.StringAttributeTypeImpl = exports.RemoteDoubleAttributeTypeImpl = exports.DoubleAttributeTypeImpl = exports.RemoteLongAttributeTypeImpl = exports.LongAttributeTypeImpl = exports.RemoteBooleanAttributeTypeImpl = exports.BooleanAttributeTypeImpl = exports.RemoteAttributeTypeImpl = exports.AttributeTypeImpl = void 0;
const dependencies_internal_1 = require("../../../dependencies_internal");
const concept_pb_1 = __importDefault(require("grakn-protocol/protobuf/concept_pb"));
const assert_1 = __importDefault(require("assert"));
var ValueType = dependencies_internal_1.AttributeType.ValueType;
var isKeyable = dependencies_internal_1.AttributeType.ValueType.isKeyable;
class AttributeTypeImpl extends dependencies_internal_1.ThingTypeImpl {
    constructor(label, isRoot) {
        super(label, isRoot);
    }
    getValueType() {
        return ValueType.OBJECT;
    }
    isKeyable() {
        return isKeyable(this.getValueType());
    }
    asRemote(transaction) {
        return new RemoteAttributeTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
    isAttributeType() {
        return true;
    }
    isBoolean() {
        return false;
    }
    isString() {
        return false;
    }
    isDouble() {
        return false;
    }
    isLong() {
        return false;
    }
    isDateTime() {
        return false;
    }
    asBoolean() {
        if (this.isRoot()) {
            return new BooleanAttributeTypeImpl(AttributeTypeImpl.ROOT_LABEL, true);
        }
        throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.INVALID_CONCEPT_CASTING.message("AttributeType", "BooleanAttributeType"));
    }
    asLong() {
        if (this.isRoot()) {
            return new LongAttributeTypeImpl(AttributeTypeImpl.ROOT_LABEL, true);
        }
        throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.INVALID_CONCEPT_CASTING.message("AttributeType", "LongAttributeType"));
    }
    asDouble() {
        if (this.isRoot()) {
            return new DoubleAttributeTypeImpl(AttributeTypeImpl.ROOT_LABEL, true);
        }
        throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.INVALID_CONCEPT_CASTING.message("AttributeType", "DoubleAttributeType"));
    }
    asString() {
        if (this.isRoot()) {
            return new StringAttributeTypeImpl(AttributeTypeImpl.ROOT_LABEL, true);
        }
        throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.INVALID_CONCEPT_CASTING.message("AttributeType", "StringAttributeType"));
    }
    asDateTime() {
        if (this.isRoot()) {
            return new DateTimeAttributeTypeImpl(AttributeTypeImpl.ROOT_LABEL, true);
        }
        throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.INVALID_CONCEPT_CASTING.message("AttributeType", "DateTimeAttributeType"));
    }
}
exports.AttributeTypeImpl = AttributeTypeImpl;
AttributeTypeImpl.ROOT_LABEL = "attribute";
class RemoteAttributeTypeImpl extends dependencies_internal_1.RemoteThingTypeImpl {
    constructor(transaction, label, isRoot) {
        super(transaction, label, isRoot);
    }
    isAttributeType() {
        return true;
    }
    isBoolean() {
        return false;
    }
    isString() {
        return false;
    }
    isDouble() {
        return false;
    }
    isLong() {
        return false;
    }
    isDateTime() {
        return false;
    }
    asBoolean() {
        if (this.isRoot()) {
            return new RemoteBooleanAttributeTypeImpl(this.transaction, RemoteAttributeTypeImpl.ROOT_LABEL, true);
        }
        throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.INVALID_CONCEPT_CASTING.message("RemoteAttributeType", "RemoteBooleanAttributeType"));
    }
    asLong() {
        if (this.isRoot()) {
            return new RemoteLongAttributeTypeImpl(this.transaction, RemoteAttributeTypeImpl.ROOT_LABEL, true);
        }
        throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.INVALID_CONCEPT_CASTING.message("RemoteAttributeType", "RemoteLongAttributeType"));
    }
    asDouble() {
        if (this.isRoot()) {
            return new RemoteDoubleAttributeTypeImpl(this.transaction, RemoteAttributeTypeImpl.ROOT_LABEL, true);
        }
        throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.INVALID_CONCEPT_CASTING.message("RemoteAttributeType", "RemoteDoubleAttributeType"));
    }
    asString() {
        if (this.isRoot()) {
            return new RemoteStringAttributeTypeImpl(this.transaction, RemoteAttributeTypeImpl.ROOT_LABEL, true);
        }
        throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.INVALID_CONCEPT_CASTING.message("RemoteAttributeType", "RemoteStringAttributeType"));
    }
    asDateTime() {
        if (this.isRoot()) {
            return new RemoteDateTimeAttributeTypeImpl(this.transaction, RemoteAttributeTypeImpl.ROOT_LABEL, true);
        }
        throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.INVALID_CONCEPT_CASTING.message("RemoteAttributeType", "RemoteDateTimeAttributeType"));
    }
    getValueType() {
        return ValueType.OBJECT;
    }
    isKeyable() {
        return isKeyable(this.getValueType());
    }
    setSupertype(attributeType) {
        return super.setSupertype(attributeType);
    }
    getSubtypes() {
        const stream = super.getSubtypes();
        if (this.isRoot() && this.getValueType() != ValueType.OBJECT) {
            return stream.filter(x => x.getValueType() == this.getValueType() || x.getLabel() == this.getLabel());
        }
        return stream;
    }
    getInstances() {
        return super.getInstances();
    }
    getOwners(onlyKey) {
        const method = new concept_pb_1.default.Type.Req()
            .setAttributeTypeGetOwnersReq(new concept_pb_1.default.AttributeType.GetOwners.Req().setOnlyKey(onlyKey || false));
        return this.typeStream(method, res => res.getAttributeTypeGetOwnersRes().getOwnersList());
    }
    async putInternal(valueProto) {
        const method = new concept_pb_1.default.Type.Req().setAttributeTypePutReq(new concept_pb_1.default.AttributeType.Put.Req().setValue(valueProto));
        return dependencies_internal_1.AttributeImpl.of(await this.execute(method).then(res => res.getAttributeTypePutRes().getAttribute()));
    }
    async getInternal(valueProto) {
        const method = new concept_pb_1.default.Type.Req().setAttributeTypeGetReq(new concept_pb_1.default.AttributeType.Get.Req().setValue(valueProto));
        const response = await this.execute(method).then(res => res.getAttributeTypeGetRes());
        return response.getResCase() === concept_pb_1.default.AttributeType.Get.Res.ResCase.ATTRIBUTE ? dependencies_internal_1.AttributeImpl.of(response.getAttribute()) : null;
    }
    asRemote(transaction) {
        return new RemoteAttributeTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
}
exports.RemoteAttributeTypeImpl = RemoteAttributeTypeImpl;
RemoteAttributeTypeImpl.ROOT_LABEL = "attribute";
class BooleanAttributeTypeImpl extends AttributeTypeImpl {
    constructor(label, isRoot) {
        super(label, isRoot);
    }
    isBoolean() {
        return true;
    }
    asBoolean() {
        return this;
    }
    static of(typeProto) {
        return new BooleanAttributeTypeImpl(typeProto.getLabel(), typeProto.getRoot());
    }
    getValueType() {
        return ValueType.BOOLEAN;
    }
    asRemote(transaction) {
        return new RemoteBooleanAttributeTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
}
exports.BooleanAttributeTypeImpl = BooleanAttributeTypeImpl;
class RemoteBooleanAttributeTypeImpl extends RemoteAttributeTypeImpl {
    constructor(transaction, label, isRoot) {
        super(transaction, label, isRoot);
    }
    isBoolean() {
        return true;
    }
    asBoolean() {
        return this;
    }
    getValueType() {
        return ValueType.BOOLEAN;
    }
    asRemote(transaction) {
        return new RemoteBooleanAttributeTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
    getSubtypes() {
        return super.getSubtypes();
    }
    getInstances() {
        return super.getInstances();
    }
    setSupertype(type) {
        return super.setSupertype(type);
    }
    put(value) {
        return this.putInternal(dependencies_internal_1.ConceptProtoBuilder.booleanAttributeValue(value));
    }
    get(value) {
        return this.getInternal(dependencies_internal_1.ConceptProtoBuilder.booleanAttributeValue(value));
    }
}
exports.RemoteBooleanAttributeTypeImpl = RemoteBooleanAttributeTypeImpl;
class LongAttributeTypeImpl extends AttributeTypeImpl {
    constructor(label, isRoot) {
        super(label, isRoot);
    }
    static of(typeProto) {
        return new LongAttributeTypeImpl(typeProto.getLabel(), typeProto.getRoot());
    }
    getValueType() {
        return ValueType.LONG;
    }
    isLong() {
        return true;
    }
    asLong() {
        return this;
    }
    asRemote(transaction) {
        return new RemoteLongAttributeTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
}
exports.LongAttributeTypeImpl = LongAttributeTypeImpl;
class RemoteLongAttributeTypeImpl extends RemoteAttributeTypeImpl {
    constructor(transaction, label, isRoot) {
        super(transaction, label, isRoot);
    }
    getValueType() {
        return ValueType.LONG;
    }
    isLong() {
        return true;
    }
    asLong() {
        return this;
    }
    asRemote(transaction) {
        return new RemoteLongAttributeTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
    getSubtypes() {
        return super.getSubtypes();
    }
    getInstances() {
        return super.getInstances();
    }
    setSupertype(type) {
        return super.setSupertype(type);
    }
    put(value) {
        return this.putInternal(dependencies_internal_1.ConceptProtoBuilder.longAttributeValue(value));
    }
    get(value) {
        return this.getInternal(dependencies_internal_1.ConceptProtoBuilder.longAttributeValue(value));
    }
}
exports.RemoteLongAttributeTypeImpl = RemoteLongAttributeTypeImpl;
class DoubleAttributeTypeImpl extends AttributeTypeImpl {
    constructor(label, isRoot) {
        super(label, isRoot);
    }
    static of(typeProto) {
        return new DoubleAttributeTypeImpl(typeProto.getLabel(), typeProto.getRoot());
    }
    getValueType() {
        return ValueType.DOUBLE;
    }
    isDouble() {
        return true;
    }
    asDouble() {
        return this;
    }
    asRemote(transaction) {
        return new RemoteDoubleAttributeTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
}
exports.DoubleAttributeTypeImpl = DoubleAttributeTypeImpl;
class RemoteDoubleAttributeTypeImpl extends RemoteAttributeTypeImpl {
    constructor(transaction, label, isRoot) {
        super(transaction, label, isRoot);
    }
    getValueType() {
        return ValueType.DOUBLE;
    }
    asRemote(transaction) {
        return new RemoteDoubleAttributeTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
    isDouble() {
        return true;
    }
    asDouble() {
        return this;
    }
    getSubtypes() {
        return super.getSubtypes();
    }
    getInstances() {
        return super.getInstances();
    }
    setSupertype(type) {
        return super.setSupertype(type);
    }
    put(value) {
        return this.putInternal(dependencies_internal_1.ConceptProtoBuilder.doubleAttributeValue(value));
    }
    get(value) {
        return this.getInternal(dependencies_internal_1.ConceptProtoBuilder.doubleAttributeValue(value));
    }
}
exports.RemoteDoubleAttributeTypeImpl = RemoteDoubleAttributeTypeImpl;
class StringAttributeTypeImpl extends AttributeTypeImpl {
    constructor(label, isRoot) {
        super(label, isRoot);
    }
    static of(typeProto) {
        return new StringAttributeTypeImpl(typeProto.getLabel(), typeProto.getRoot());
    }
    getValueType() {
        return ValueType.STRING;
    }
    isString() {
        return true;
    }
    asString() {
        return this;
    }
    asRemote(transaction) {
        return new RemoteStringAttributeTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
}
exports.StringAttributeTypeImpl = StringAttributeTypeImpl;
class RemoteStringAttributeTypeImpl extends RemoteAttributeTypeImpl {
    constructor(transaction, label, isRoot) {
        super(transaction, label, isRoot);
    }
    getValueType() {
        return ValueType.STRING;
    }
    asRemote(transaction) {
        return new RemoteStringAttributeTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
    isString() {
        return true;
    }
    asString() {
        return this;
    }
    getSubtypes() {
        return super.getSubtypes();
    }
    getInstances() {
        return super.getInstances();
    }
    setSupertype(type) {
        return super.setSupertype(type);
    }
    put(value) {
        return this.putInternal(dependencies_internal_1.ConceptProtoBuilder.stringAttributeValue(value));
    }
    get(value) {
        return this.getInternal(dependencies_internal_1.ConceptProtoBuilder.stringAttributeValue(value));
    }
    async getRegex() {
        return (await this.execute(new concept_pb_1.default.Type.Req().setAttributeTypeGetRegexReq(new concept_pb_1.default.AttributeType.GetRegex.Req()))).getAttributeTypeGetRegexRes().getRegex();
    }
    async setRegex(regex) {
        await this.execute(new concept_pb_1.default.Type.Req().setAttributeTypeSetRegexReq(new concept_pb_1.default.AttributeType.SetRegex.Req()
            .setRegex(regex || "")));
    }
}
exports.RemoteStringAttributeTypeImpl = RemoteStringAttributeTypeImpl;
class DateTimeAttributeTypeImpl extends AttributeTypeImpl {
    constructor(label, isRoot) {
        super(label, isRoot);
    }
    static of(typeProto) {
        return new DateTimeAttributeTypeImpl(typeProto.getLabel(), typeProto.getRoot());
    }
    getValueType() {
        return ValueType.DATETIME;
    }
    isDateTime() {
        return true;
    }
    asDateTime() {
        return this;
    }
    asRemote(transaction) {
        return new RemoteDateTimeAttributeTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
}
exports.DateTimeAttributeTypeImpl = DateTimeAttributeTypeImpl;
class RemoteDateTimeAttributeTypeImpl extends RemoteAttributeTypeImpl {
    constructor(transaction, label, isRoot) {
        super(transaction, label, isRoot);
    }
    getValueType() {
        return ValueType.DATETIME;
    }
    isDateTime() {
        return true;
    }
    asDateTime() {
        return this;
    }
    asRemote(transaction) {
        return new RemoteDateTimeAttributeTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
    getSubtypes() {
        return super.getSubtypes();
    }
    getInstances() {
        return super.getInstances();
    }
    setSupertype(type) {
        return super.setSupertype(type);
    }
    put(value) {
        return this.putInternal(dependencies_internal_1.ConceptProtoBuilder.dateTimeAttributeValue(value));
    }
    get(value) {
        return this.getInternal(dependencies_internal_1.ConceptProtoBuilder.dateTimeAttributeValue(value));
    }
}
exports.RemoteDateTimeAttributeTypeImpl = RemoteDateTimeAttributeTypeImpl;
(function (AttributeTypeImpl) {
    function of(typeProto) {
        switch (typeProto.getValueType()) {
            case concept_pb_1.default.AttributeType.ValueType.BOOLEAN:
                return new BooleanAttributeTypeImpl(typeProto.getLabel(), typeProto.getRoot());
            case concept_pb_1.default.AttributeType.ValueType.LONG:
                return new LongAttributeTypeImpl(typeProto.getLabel(), typeProto.getRoot());
            case concept_pb_1.default.AttributeType.ValueType.DOUBLE:
                return new DoubleAttributeTypeImpl(typeProto.getLabel(), typeProto.getRoot());
            case concept_pb_1.default.AttributeType.ValueType.STRING:
                return new StringAttributeTypeImpl(typeProto.getLabel(), typeProto.getRoot());
            case concept_pb_1.default.AttributeType.ValueType.DATETIME:
                return new DateTimeAttributeTypeImpl(typeProto.getLabel(), typeProto.getRoot());
            case concept_pb_1.default.AttributeType.ValueType.OBJECT:
                assert_1.default(typeProto.getRoot());
                return new AttributeTypeImpl(typeProto.getLabel(), typeProto.getRoot());
            default:
                throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.BAD_VALUE_TYPE.message(typeProto.getValueType()));
        }
    }
    AttributeTypeImpl.of = of;
})(AttributeTypeImpl = exports.AttributeTypeImpl || (exports.AttributeTypeImpl = {}));
