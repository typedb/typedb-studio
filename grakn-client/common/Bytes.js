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
exports.Bytes = void 0;
class Bytes {
    static hexStringToBytes(hexString) {
        if (hexString.length % 2 != 0)
            throw new Error("hexString length not divisible by 2: " + hexString.length);
        if (!hexString.startsWith(Bytes.PREFIX))
            throw new Error("hexString does not start with '" + Bytes.PREFIX + "': " + hexString + "'");
        hexString = hexString.replace(Bytes.PREFIX, "");
        return Uint8Array.from(Buffer.from(hexString, 'hex'));
    }
    static bytesToHexString(bytes) {
        return Bytes.PREFIX + Buffer.from(bytes).toString('hex');
    }
}
exports.Bytes = Bytes;
Bytes.PREFIX = "0x";
