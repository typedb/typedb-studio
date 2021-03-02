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
exports.RemoteEntityImpl = exports.EntityImpl = void 0;
const dependencies_internal_1 = require("../../../dependencies_internal");
class EntityImpl extends dependencies_internal_1.ThingImpl {
    constructor(iid, type) {
        super(iid);
        this._type = type;
    }
    static of(protoThing) {
        return new EntityImpl(dependencies_internal_1.Bytes.bytesToHexString(protoThing.getIid_asU8()), dependencies_internal_1.EntityTypeImpl.of(protoThing.getType()));
    }
    getType() {
        return this._type;
    }
    asRemote(transaction) {
        return new RemoteEntityImpl(transaction, this.getIID(), this._type);
    }
    isEntity() {
        return true;
    }
}
exports.EntityImpl = EntityImpl;
class RemoteEntityImpl extends dependencies_internal_1.RemoteThingImpl {
    constructor(transaction, iid, type) {
        super(transaction, iid);
        this._type = type;
    }
    asRemote(transaction) {
        return new RemoteEntityImpl(transaction, this.getIID(), this._type);
    }
    getType() {
        return this._type;
    }
    isEntity() {
        return true;
    }
}
exports.RemoteEntityImpl = RemoteEntityImpl;
