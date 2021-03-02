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
exports.ConceptMapGroup = void 0;
const dependencies_internal_1 = require("../../dependencies_internal");
class ConceptMapGroup {
    constructor(owner, conceptMaps) {
        this._owner = owner;
        this._conceptMaps = conceptMaps;
    }
    static of(res) {
        let concept;
        if (res.getOwner().hasThing())
            concept = dependencies_internal_1.ThingImpl.of(res.getOwner().getThing());
        else
            concept = dependencies_internal_1.TypeImpl.of(res.getOwner().getType());
        return new ConceptMapGroup(concept, res.getConceptMapsList().map((ans) => dependencies_internal_1.ConceptMap.of(ans)));
    }
    owner() {
        return this._owner;
    }
    conceptMaps() {
        return this._conceptMaps;
    }
}
exports.ConceptMapGroup = ConceptMapGroup;
