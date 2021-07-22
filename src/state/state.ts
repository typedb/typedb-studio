import { StateHook } from "./state-hook";
import { studioDarkTheme } from "../styles/theme";

export interface DatabaseServer {
    address: string;
    dbs: string[];
}

export const dbServerState = new StateHook<DatabaseServer>({ address: null, dbs: [] });
export const databaseState = new StateHook<string>(null);
export const themeState = new StateHook(studioDarkTheme);
