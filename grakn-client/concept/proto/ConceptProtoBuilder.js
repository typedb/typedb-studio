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
exports.ConceptProtoBuilder = void 0;
// TODO: Can we get rid of this?
const concept_pb_1 = __importDefault(require("grakn-protocol/protobuf/concept_pb"));
const dependencies_internal_1 = require("../../dependencies_internal");
var ConceptProtoBuilder;
(function (ConceptProtoBuilder) {
    function thing(thing) {
        return new concept_pb_1.default.Thing().setIid(dependencies_internal_1.Bytes.hexStringToBytes(thing.getIID()));
    }
    ConceptProtoBuilder.thing = thing;
    function type(type) {
        const typeProto = new concept_pb_1.default.Type()
            .setLabel(type.getLabel())
            .setEncoding(encoding(type));
        if (type.isRoleType()) {
            typeProto.setScope(type.getScope());
        }
        return typeProto;
    }
    ConceptProtoBuilder.type = type;
    function types(types) {
        return types.map(type);
    }
    ConceptProtoBuilder.types = types;
    // The 'attributeValue' functions are split up like this to avoid ambiguity between Long and Double
    function booleanAttributeValue(value) {
        return new concept_pb_1.default.Attribute.Value().setBoolean(value);
    }
    ConceptProtoBuilder.booleanAttributeValue = booleanAttributeValue;
    function longAttributeValue(value) {
        return new concept_pb_1.default.Attribute.Value().setLong(value);
    }
    ConceptProtoBuilder.longAttributeValue = longAttributeValue;
    function doubleAttributeValue(value) {
        return new concept_pb_1.default.Attribute.Value().setDouble(value);
    }
    ConceptProtoBuilder.doubleAttributeValue = doubleAttributeValue;
    function stringAttributeValue(value) {
        return new concept_pb_1.default.Attribute.Value().setString(value);
    }
    ConceptProtoBuilder.stringAttributeValue = stringAttributeValue;
    function dateTimeAttributeValue(value) {
        return new concept_pb_1.default.Attribute.Value().setDateTime(value.getTime());
    }
    ConceptProtoBuilder.dateTimeAttributeValue = dateTimeAttributeValue;
    function valueType(valueType) {
        switch (valueType) {
            case dependencies_internal_1.AttributeType.ValueType.OBJECT:
                return concept_pb_1.default.AttributeType.ValueType.OBJECT;
            case dependencies_internal_1.AttributeType.ValueType.BOOLEAN:
                return concept_pb_1.default.AttributeType.ValueType.BOOLEAN;
            case dependencies_internal_1.AttributeType.ValueType.LONG:
                return concept_pb_1.default.AttributeType.ValueType.LONG;
            case dependencies_internal_1.AttributeType.ValueType.DOUBLE:
                return concept_pb_1.default.AttributeType.ValueType.DOUBLE;
            case dependencies_internal_1.AttributeType.ValueType.STRING:
                return concept_pb_1.default.AttributeType.ValueType.STRING;
            case dependencies_internal_1.AttributeType.ValueType.DATETIME:
                return concept_pb_1.default.AttributeType.ValueType.DATETIME;
        }
    }
    ConceptProtoBuilder.valueType = valueType;
    function encoding(type) {
        if (type.isEntityType()) {
            return concept_pb_1.default.Type.Encoding.ENTITY_TYPE;
        }
        else if (type.isRelationType()) {
            return concept_pb_1.default.Type.Encoding.RELATION_TYPE;
        }
        else if (type.isAttributeType()) {
            return concept_pb_1.default.Type.Encoding.ATTRIBUTE_TYPE;
        }
        else if (type.isRoleType()) {
            return concept_pb_1.default.Type.Encoding.ROLE_TYPE;
        }
        else if (type.isThingType()) {
            return concept_pb_1.default.Type.Encoding.THING_TYPE;
        }
        else {
            throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.BAD_ENCODING.message(type));
        }
    }
    ConceptProtoBuilder.encoding = encoding;
})(ConceptProtoBuilder = exports.ConceptProtoBuilder || (exports.ConceptProtoBuilder = {}));
