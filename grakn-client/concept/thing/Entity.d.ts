import { Thing, RemoteThing, EntityType, GraknClient } from "../../dependencies_internal";
import Transaction = GraknClient.Transaction;
export interface Entity extends Thing {
    getType(): EntityType;
    asRemote(transaction: Transaction): RemoteEntity;
}
export interface RemoteEntity extends RemoteThing {
    getType(): EntityType;
    asRemote(transaction: Transaction): RemoteEntity;
}
