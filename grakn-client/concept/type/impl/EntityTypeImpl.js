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
exports.RemoteEntityTypeImpl = exports.EntityTypeImpl = void 0;
const dependencies_internal_1 = require("../../../dependencies_internal");
const concept_pb_1 = __importDefault(require("grakn-protocol/protobuf/concept_pb"));
class EntityTypeImpl extends dependencies_internal_1.ThingTypeImpl {
    constructor(label, isRoot) {
        super(label, isRoot);
    }
    static of(typeProto) {
        return new EntityTypeImpl(typeProto.getLabel(), typeProto.getRoot());
    }
    asRemote(transaction) {
        return new RemoteEntityTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
    isEntityType() {
        return true;
    }
}
exports.EntityTypeImpl = EntityTypeImpl;
class RemoteEntityTypeImpl extends dependencies_internal_1.RemoteThingTypeImpl {
    constructor(transaction, label, isRoot) {
        super(transaction, label, isRoot);
    }
    isEntityType() {
        return true;
    }
    create() {
        const method = new concept_pb_1.default.Type.Req().setEntityTypeCreateReq(new concept_pb_1.default.EntityType.Create.Req());
        return this.execute(method).then(res => dependencies_internal_1.EntityImpl.of(res.getEntityTypeCreateRes().getEntity()));
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
    asRemote(transaction) {
        return new RemoteEntityTypeImpl(transaction, this.getLabel(), this.isRoot());
    }
}
exports.RemoteEntityTypeImpl = RemoteEntityTypeImpl;
