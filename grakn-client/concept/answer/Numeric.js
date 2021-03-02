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
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    Object.defineProperty(o, k2, { enumerable: true, get: function() { return m[k]; } });
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.Numeric = void 0;
const answer_pb = __importStar(require("grakn-protocol/protobuf/answer_pb"));
const dependencies_internal_1 = require("../../dependencies_internal");
class Numeric {
    constructor(numberValue) {
        this.toString = () => {
            return this.isNumber() ? `${this.asNumber()}` : 'NaN';
        };
        this._numberValue = numberValue;
    }
    static of(answer) {
        switch (answer.getValueCase()) {
            case answer_pb.Numeric.ValueCase.LONG_VALUE:
                return Numeric.ofNumber(answer.getLongValue());
            case answer_pb.Numeric.ValueCase.DOUBLE_VALUE:
                return Numeric.ofNumber(answer.getDoubleValue());
            case answer_pb.Numeric.ValueCase.NAN:
                return Numeric.ofNaN();
            default:
                throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Query.BAD_ANSWER_TYPE.message(answer.getValueCase()));
        }
    }
    isNumber() {
        return this._numberValue != null;
    }
    isNaN() {
        return !this.isNumber();
    }
    asNumber() {
        if (this.isNumber())
            return this._numberValue;
        else
            throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Internal.ILLEGAL_CAST.message("NaN", "number"));
    }
    static ofNumber(value) {
        return new Numeric(value);
    }
    static ofNaN() {
        return new Numeric(null);
    }
}
exports.Numeric = Numeric;
