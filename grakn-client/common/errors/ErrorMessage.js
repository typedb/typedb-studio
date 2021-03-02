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
exports.ErrorMessage = void 0;
class ErrorMessage {
    constructor(codePrefix, codeNumber, messagePrefix, messageBody) {
        this._codePrefix = codePrefix;
        this._codeNumber = codeNumber;
        this._messagePrefix = messagePrefix;
        this._messageBody = messageBody;
        if (!ErrorMessage.knownErrors.has(codePrefix)) {
            ErrorMessage.knownErrors.set(codePrefix, new Map());
        }
        ErrorMessage.knownErrors.get(codePrefix).set(codeNumber, this);
        ErrorMessage.maxCodeNumber = Math.max(ErrorMessage.maxCodeNumber, codeNumber);
        ErrorMessage.maxCodeDigits = String(ErrorMessage.maxCodeNumber).length;
    }
    code() {
        if (this._code == null) {
            let zeros = "";
            for (let length = String(this._code).length; length < ErrorMessage.maxCodeDigits; length++) {
                zeros += "0";
            }
            this._code = `${this._codePrefix}${zeros}${this._codeNumber}`;
        }
        return this._code;
    }
    message(...args) {
        return `[${this.code()}] ${this._messagePrefix}: ${this._messageBody(args)}`;
    }
    toString() {
        return `[${this.code()}] ${this._messagePrefix}: ${this._messageBody([])}`;
    }
}
exports.ErrorMessage = ErrorMessage;
ErrorMessage.knownErrors = new Map();
ErrorMessage.maxCodeNumber = 0;
(function (ErrorMessage) {
    class Client extends ErrorMessage {
        constructor(code, message) { super("CLI", code, "Client Error", message); }
    }
    ErrorMessage.Client = Client;
    (function (Client) {
        Client.TRANSACTION_CLOSED = new Client(1, () => `The transaction has been closed and no further operation is allowed.`);
        Client.UNABLE_TO_CONNECT = new Client(2, () => `Unable to connect to Grakn server.`);
        Client.NEGATIVE_VALUE_NOT_ALLOWED = new Client(3, (args) => `Value cannot be less than 1, was: '${args[0]}'.`);
        Client.MISSING_DB_NAME = new Client(4, () => `Database name cannot be null.`);
        Client.DB_DOES_NOT_EXIST = new Client(5, (args) => `The database '${args[0]}' does not exist.`);
        Client.MISSING_RESPONSE = new Client(6, (args) => `The required field 'res' of type '${args[0]}' was not set.`);
        Client.UNKNOWN_REQUEST_ID = new Client(7, (args) => `Received a response with unknown request id '${args[0]}'.`);
        Client.CLUSTER_NO_PRIMARY_REPLICA_YET = new Client(8, (args) => `No replica has been marked as the primary replica for latest known term '${args[0]}'.`);
        Client.CLUSTER_UNABLE_TO_CONNECT = new Client(9, (args) => `Unable to connect to Grakn Cluster. Attempted connecting to the cluster members, but none are available: '${args[0]}'.`);
        Client.CLUSTER_REPLICA_NOT_PRIMARY = new Client(10, () => `The replica is not the primary replica.`);
        Client.CLUSTER_ALL_NODES_FAILED = new Client(11, (args) => `Attempted connecting to all cluster members, but the following errors occurred: \n'${args[0]}'`);
        Client.UNRECOGNISED_SESSION_TYPE = new Client(12, (args) => `Session type '${args[0]}' was not recognised.`);
    })(Client = ErrorMessage.Client || (ErrorMessage.Client = {}));
    class Concept extends ErrorMessage {
        constructor(code, message) { super("CON", code, "Concept Error", message); }
    }
    ErrorMessage.Concept = Concept;
    (function (Concept) {
        Concept.INVALID_CONCEPT_CASTING = new Concept(1, (args) => `Invalid concept conversion from '${args[0]}' to '${args[1]}'.`);
        Concept.MISSING_TRANSACTION = new Concept(2, () => `Transaction cannot be null.`);
        Concept.MISSING_IID = new Concept(3, () => `IID cannot be null or empty.`);
        Concept.MISSING_LABEL = new Concept(4, () => `Label cannot be null or empty.`);
        Concept.BAD_ENCODING = new Concept(5, (args) => `The encoding '${args[0]}' was not recognised.`);
        Concept.BAD_VALUE_TYPE = new Concept(6, (args) => `The value type '${args[0]}' was not recognised.`);
        Concept.BAD_ATTRIBUTE_VALUE = new Concept(7, (args) => `The attribute value '${args[0]}' was not recognised.`);
    })(Concept = ErrorMessage.Concept || (ErrorMessage.Concept = {}));
    class Query extends ErrorMessage {
        constructor(code, message) { super("QRY", code, "Query Error", message); }
    }
    ErrorMessage.Query = Query;
    (function (Query) {
        Query.VARIABLE_DOES_NOT_EXIST = new Query(1, (args) => `The variable '${args[0]}' does not exist.`);
        Query.NO_EXPLANATION = new Query(2, () => `No explanation was found.`);
        Query.BAD_ANSWER_TYPE = new Query(3, (args) => `The answer type '${args[0]}' was not recognised.`);
        Query.MISSING_ANSWER = new Query(4, (args) => `The required field 'answer' of type '${args[0]}' was not set.`);
    })(Query = ErrorMessage.Query || (ErrorMessage.Query = {}));
    class Internal extends ErrorMessage {
        constructor(code, message) { super("INT", code, "Internal Error", message); }
    }
    ErrorMessage.Internal = Internal;
    (function (Internal) {
        Internal.ILLEGAL_CAST = new Internal(2, (args) => `Illegal casting operation from '${args[0]}' to '${args[1]}'.`);
        Internal.ILLEGAL_ARGUMENT = new Internal(3, (args) => `Illegal argument provided: '${args[0]}'`);
    })(Internal = ErrorMessage.Internal || (ErrorMessage.Internal = {}));
})(ErrorMessage = exports.ErrorMessage || (exports.ErrorMessage = {}));
