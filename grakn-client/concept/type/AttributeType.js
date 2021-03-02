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
exports.AttributeType = void 0;
const dependencies_internal_1 = require("../../dependencies_internal");
const concept_pb_1 = __importDefault(require("grakn-protocol/protobuf/concept_pb"));
var AttributeType;
(function (AttributeType) {
    let ValueType;
    (function (ValueType) {
        ValueType["OBJECT"] = "OBJECT";
        ValueType["BOOLEAN"] = "BOOLEAN";
        ValueType["LONG"] = "LONG";
        ValueType["DOUBLE"] = "DOUBLE";
        ValueType["STRING"] = "STRING";
        ValueType["DATETIME"] = "DATETIME";
    })(ValueType = AttributeType.ValueType || (AttributeType.ValueType = {}));
    (function (ValueType) {
        function of(valueType) {
            switch (valueType) {
                case concept_pb_1.default.AttributeType.ValueType.STRING:
                    return ValueType.STRING;
                case concept_pb_1.default.AttributeType.ValueType.BOOLEAN:
                    return ValueType.BOOLEAN;
                case concept_pb_1.default.AttributeType.ValueType.LONG:
                    return ValueType.LONG;
                case concept_pb_1.default.AttributeType.ValueType.DOUBLE:
                    return ValueType.DOUBLE;
                case concept_pb_1.default.AttributeType.ValueType.DATETIME:
                    return ValueType.DATETIME;
                default:
                    throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Concept.BAD_VALUE_TYPE.message(valueType));
            }
        }
        ValueType.of = of;
        function isKeyable(valueType) {
            return [ValueType.LONG, ValueType.STRING, ValueType.DATETIME].includes(valueType);
        }
        ValueType.isKeyable = isKeyable;
        function isWritable(valueType) {
            return valueType !== ValueType.OBJECT;
        }
        ValueType.isWritable = isWritable;
    })(ValueType = AttributeType.ValueType || (AttributeType.ValueType = {}));
})(AttributeType = exports.AttributeType || (exports.AttributeType = {}));
