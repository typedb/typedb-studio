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
exports.RemoteDateTimeAttributeImpl = exports.DateTimeAttributeImpl = exports.RemoteStringAttributeImpl = exports.StringAttributeImpl = exports.RemoteDoubleAttributeImpl = exports.DoubleAttributeImpl = exports.RemoteLongAttributeImpl = exports.LongAttributeImpl = exports.RemoteBooleanAttributeImpl = exports.BooleanAttributeImpl = exports.RemoteAttributeImpl = exports.AttributeImpl = void 0;
const dependencies_internal_1 = require("../../../dependencies_internal");
const concept_pb_1 = __importDefault(require("grakn-protocol/protobuf/concept_pb"));
class AttributeImpl extends dependencies_internal_1.ThingImpl {
    constructor(iid) {
        super(iid);
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
    isAttribute() {
        return true;
    }
}
exports.AttributeImpl = AttributeImpl;
class RemoteAttributeImpl extends dependencies_internal_1.RemoteThingImpl {
    constructor(transaction, iid) {
        super(transaction, iid);
    }
    getOwners(ownerType) {
        const getOwnersReq = new concept_pb_1.default.Attribute.GetOwners.Req();
        if (ownerType)
            getOwnersReq.setThingType(dependencies_internal_1.ConceptProtoBuilder.type(ownerType));
        const method = new concept_pb_1.default.Thing.Req().setAttributeGetOwnersReq(getOwnersReq);
        return this.thingStream(method, res => res.getAttributeGetOwnersRes().getThingsList());
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
    isAttribute() {
        return true;
    }
}
exports.RemoteAttributeImpl = RemoteAttributeImpl;
class BooleanAttributeImpl extends AttributeImpl {
    constructor(iid, type, value) {
        super(iid);
        this._type = type;
        this._value = value;
    }
    static of(protoThing) {
        return new BooleanAttributeImpl(dependencies_internal_1.Bytes.bytesToHexString(protoThing.getIid_asU8()), dependencies_internal_1.BooleanAttributeTypeImpl.of(protoThing.getType()), protoThing.getValue().getBoolean());
    }
    asRemote(transaction) {
        return new RemoteBooleanAttributeImpl(transaction, this.getIID(), this._type, this._value);
    }
    getType() {
        return this._type;
    }
    getValue() {
        return this._value;
    }
    isBoolean() {
        return true;
    }
}
exports.BooleanAttributeImpl = BooleanAttributeImpl;
class RemoteBooleanAttributeImpl extends RemoteAttributeImpl {
    constructor(transaction, iid, type, value) {
        super(transaction, iid);
        this._type = type;
        this._value = value;
    }
    getValue() {
        return this._value;
    }
    getType() {
        return this._type;
    }
    asRemote(transaction) {
        return new RemoteBooleanAttributeImpl(transaction, this.getIID(), this._type, this._value);
    }
    isBoolean() {
        return true;
    }
}
exports.RemoteBooleanAttributeImpl = RemoteBooleanAttributeImpl;
class LongAttributeImpl extends AttributeImpl {
    constructor(iid, type, value) {
        super(iid);
        this._type = type;
        this._value = value;
    }
    static of(protoThing) {
        return new LongAttributeImpl(dependencies_internal_1.Bytes.bytesToHexString(protoThing.getIid_asU8()), dependencies_internal_1.LongAttributeTypeImpl.of(protoThing.getType()), protoThing.getValue().getLong());
    }
    asRemote(transaction) {
        return new RemoteLongAttributeImpl(transaction, this.getIID(), this._type, this._value);
    }
    getType() {
        return this._type;
    }
    getValue() {
        return this._value;
    }
    isLong() {
        return true;
    }
}
exports.LongAttributeImpl = LongAttributeImpl;
class RemoteLongAttributeImpl extends RemoteAttributeImpl {
    constructor(transaction, iid, type, value) {
        super(transaction, iid);
        this._type = type;
        this._value = value;
    }
    getValue() {
        return this._value;
    }
    getType() {
        return this._type;
    }
    asRemote(transaction) {
        return new RemoteLongAttributeImpl(transaction, this.getIID(), this._type, this._value);
    }
    isLong() {
        return true;
    }
}
exports.RemoteLongAttributeImpl = RemoteLongAttributeImpl;
class DoubleAttributeImpl extends AttributeImpl {
    constructor(iid, type, value) {
        super(iid);
        this._type = type;
        this._value = value;
    }
    static of(protoThing) {
        return new DoubleAttributeImpl(dependencies_internal_1.Bytes.bytesToHexString(protoThing.getIid_asU8()), dependencies_internal_1.DoubleAttributeTypeImpl.of(protoThing.getType()), protoThing.getValue().getDouble());
    }
    asRemote(transaction) {
        return new RemoteDoubleAttributeImpl(transaction, this.getIID(), this._type, this._value);
    }
    getType() {
        return this._type;
    }
    getValue() {
        return this._value;
    }
    isDouble() {
        return true;
    }
}
exports.DoubleAttributeImpl = DoubleAttributeImpl;
class RemoteDoubleAttributeImpl extends RemoteAttributeImpl {
    constructor(transaction, iid, type, value) {
        super(transaction, iid);
        this._type = type;
        this._value = value;
    }
    getValue() {
        return this._value;
    }
    getType() {
        return this._type;
    }
    asRemote(transaction) {
        return new RemoteDoubleAttributeImpl(transaction, this.getIID(), this._type, this._value);
    }
    isDouble() {
        return true;
    }
}
exports.RemoteDoubleAttributeImpl = RemoteDoubleAttributeImpl;
class StringAttributeImpl extends AttributeImpl {
    constructor(iid, type, value) {
        super(iid);
        this._type = type;
        this._value = value;
    }
    static of(protoThing) {
        return new StringAttributeImpl(dependencies_internal_1.Bytes.bytesToHexString(protoThing.getIid_asU8()), dependencies_internal_1.StringAttributeTypeImpl.of(protoThing.getType()), protoThing.getValue().getString());
    }
    asRemote(transaction) {
        return new RemoteStringAttributeImpl(transaction, this.getIID(), this._type, this._value);
    }
    getType() {
        return this._type;
    }
    getValue() {
        return this._value;
    }
    isString() {
        return true;
    }
}
exports.StringAttributeImpl = StringAttributeImpl;
class RemoteStringAttributeImpl extends RemoteAttributeImpl {
    constructor(transaction, iid, type, value) {
        super(transaction, iid);
        this._type = type;
        this._value = value;
    }
    getValue() {
        return this._value;
    }
    getType() {
        return this._type;
    }
    asRemote(transaction) {
        return new RemoteStringAttributeImpl(transaction, this.getIID(), this._type, this._value);
    }
    isString() {
        return true;
    }
}
exports.RemoteStringAttributeImpl = RemoteStringAttributeImpl;
class DateTimeAttributeImpl extends AttributeImpl {
    constructor(iid, type, value) {
        super(iid);
        this._type = type;
        this._value = value;
    }
    static of(protoThing) {
        return new DateTimeAttributeImpl(dependencies_internal_1.Bytes.bytesToHexString(protoThing.getIid_asU8()), dependencies_internal_1.DateTimeAttributeTypeImpl.of(protoThing.getType()), new Date(protoThing.getValue().getDateTime()));
    }
    asRemote(transaction) {
        return new RemoteDateTimeAttributeImpl(transaction, this.getIID(), this._type, this._value);
    }
    getType() {
        return this._type;
    }
    getValue() {
        return this._value;
    }
    isDateTime() {
        return true;
    }
}
exports.DateTimeAttributeImpl = DateTimeAttributeImpl;
class RemoteDateTimeAttributeImpl extends RemoteAttributeImpl {
    constructor(transaction, iid, type, value) {
        super(transaction, iid);
        this._type = type;
        this._value = value;
    }
    getValue() {
        return this._value;
    }
    getType() {
        return this._type;
    }
    asRemote(transaction) {
        return new RemoteDateTimeAttributeImpl(transaction, this.getIID(), this._type, this._value);
    }
    isDateTime() {
        return true;
    }
}
exports.RemoteDateTimeAttributeImpl = RemoteDateTimeAttributeImpl;
(function (AttributeImpl) {
    function of(thingProto) {
        switch (thingProto.getType().getValueType()) {
            case concept_pb_1.default.AttributeType.ValueType.BOOLEAN:
                return BooleanAttributeImpl.of(thingProto);
            case concept_pb_1.default.AttributeType.ValueType.LONG:
                return LongAttributeImpl.of(thingProto);
            case concept_pb_1.default.AttributeType.ValueType.DOUBLE:
                return DoubleAttributeImpl.of(thingProto);
            case concept_pb_1.default.AttributeType.ValueType.STRING:
                return StringAttributeImpl.of(thingProto);
            case concept_pb_1.default.AttributeType.ValueType.DATETIME:
                return DateTimeAttributeImpl.of(thingProto);
            default:
                throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.BAD_VALUE_TYPE.message(thingProto.getType().getValueType()));
        }
    }
    AttributeImpl.of = of;
})(AttributeImpl = exports.AttributeImpl || (exports.AttributeImpl = {}));
