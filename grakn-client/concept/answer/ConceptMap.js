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
Object.defineProperty(exports, "__esModule", { value: true });
exports.ConceptMap = void 0;
const dependencies_internal_1 = require("../../dependencies_internal");
class ConceptMap {
    constructor(map) {
        this._map = map;
    }
    static of(res) {
        const variableMap = new Map();
        res.getMapMap().forEach((resConcept, resLabel) => {
            let concept;
            if (resConcept.hasThing())
                concept = dependencies_internal_1.ThingImpl.of(resConcept.getThing());
            else
                concept = dependencies_internal_1.TypeImpl.of(resConcept.getType());
            variableMap.set(resLabel, concept);
        });
        return new ConceptMap(variableMap);
    }
    map() { return this._map; }
    concepts() { return this._map.values(); }
    get(variable) {
        const concept = this._map.get(variable);
        if (concept == null)
            throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Query.VARIABLE_DOES_NOT_EXIST.message(variable));
        return concept;
    }
    toString() {
        let output = "";
        for (const entry of this._map.entries()) {
            output += `[${entry[0]}/${entry[1]}]`;
        }
        return output;
    }
}
exports.ConceptMap = ConceptMap;
