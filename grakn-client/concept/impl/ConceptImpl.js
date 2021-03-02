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
exports.RemoteConceptImpl = exports.ConceptImpl = void 0;
class ConceptImpl {
    isRemote() {
        return false;
    }
    isType() {
        return false;
    }
    isThingType() {
        return false;
    }
    isEntityType() {
        return false;
    }
    isAttributeType() {
        return false;
    }
    isRelationType() {
        return false;
    }
    isRoleType() {
        return false;
    }
    isThing() {
        return false;
    }
    isEntity() {
        return false;
    }
    isAttribute() {
        return false;
    }
    isRelation() {
        return false;
    }
}
exports.ConceptImpl = ConceptImpl;
class RemoteConceptImpl {
    isRemote() {
        return true;
    }
    isType() {
        return false;
    }
    isRoleType() {
        return false;
    }
    isThingType() {
        return false;
    }
    isEntityType() {
        return false;
    }
    isAttributeType() {
        return false;
    }
    isRelationType() {
        return false;
    }
    isThing() {
        return false;
    }
    isEntity() {
        return false;
    }
    isAttribute() {
        return false;
    }
    isRelation() {
        return false;
    }
}
exports.RemoteConceptImpl = RemoteConceptImpl;
