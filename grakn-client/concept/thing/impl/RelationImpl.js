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
exports.RemoteRelationImpl = exports.RelationImpl = void 0;
const dependencies_internal_1 = require("../../../dependencies_internal");
const concept_pb_1 = __importDefault(require("grakn-protocol/protobuf/concept_pb"));
const transaction_pb_1 = __importDefault(require("grakn-protocol/protobuf/transaction_pb"));
class RelationImpl extends dependencies_internal_1.ThingImpl {
    constructor(iid, type) {
        super(iid);
        this._type = type;
    }
    static of(protoThing) {
        return new RelationImpl(dependencies_internal_1.Bytes.bytesToHexString(protoThing.getIid_asU8()), dependencies_internal_1.RelationTypeImpl.of(protoThing.getType()));
    }
    asRemote(transaction) {
        return new RemoteRelationImpl(transaction, this.getIID(), this._type);
    }
    getType() {
        return this._type;
    }
    isRelation() {
        return true;
    }
}
exports.RelationImpl = RelationImpl;
class RemoteRelationImpl extends dependencies_internal_1.RemoteThingImpl {
    constructor(transaction, iid, type) {
        super(transaction, iid);
        this._type = type;
    }
    asRemote(transaction) {
        return new RemoteRelationImpl(transaction, this.getIID(), this._type);
    }
    getType() {
        return this._type;
    }
    async getPlayersByRoleType() {
        const method = new concept_pb_1.default.Thing.Req()
            .setRelationGetPlayersByRoleTypeReq(new concept_pb_1.default.Relation.GetPlayersByRoleType.Req())
            .setIid(dependencies_internal_1.Bytes.hexStringToBytes(this.getIID()));
        const request = new transaction_pb_1.default.Transaction.Req().setThingReq(method);
        const stream = this.transaction.stream(request, res => res.getThingRes().getRelationGetPlayersByRoleTypeRes().getRoleTypesWithPlayersList());
        const rolePlayerMap = new Map();
        for await (const rolePlayer of stream) {
            const role = dependencies_internal_1.TypeImpl.of(rolePlayer.getRoleType());
            const player = dependencies_internal_1.ThingImpl.of(rolePlayer.getPlayer());
            let addedToExistingEntry = false;
            for (const roleKey of rolePlayerMap.keys()) {
                if (roleKey.getScopedLabel() === role.getScopedLabel()) {
                    rolePlayerMap.get(roleKey).push(player);
                    addedToExistingEntry = true;
                    break;
                }
            }
            if (!addedToExistingEntry)
                rolePlayerMap.set(role, [player]);
        }
        return rolePlayerMap;
    }
    getPlayers(roleTypes = []) {
        const method = new concept_pb_1.default.Thing.Req().setRelationGetPlayersReq(new concept_pb_1.default.Relation.GetPlayers.Req().setRoleTypesList(roleTypes.map(roleType => dependencies_internal_1.ConceptProtoBuilder.type(roleType))));
        return this.thingStream(method, res => res.getRelationGetPlayersRes().getThingsList());
    }
    async addPlayer(roleType, player) {
        await this.execute(new concept_pb_1.default.Thing.Req().setRelationAddPlayerReq(new concept_pb_1.default.Relation.AddPlayer.Req()
            .setPlayer(dependencies_internal_1.ConceptProtoBuilder.thing(player))
            .setRoleType(dependencies_internal_1.ConceptProtoBuilder.type(roleType))));
    }
    async removePlayer(roleType, player) {
        await this.execute(new concept_pb_1.default.Thing.Req().setRelationRemovePlayerReq(new concept_pb_1.default.Relation.RemovePlayer.Req()
            .setPlayer(dependencies_internal_1.ConceptProtoBuilder.thing(player))
            .setRoleType(dependencies_internal_1.ConceptProtoBuilder.type(roleType))));
    }
    isRelation() {
        return true;
    }
}
exports.RemoteRelationImpl = RemoteRelationImpl;
