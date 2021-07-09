import { TypeDBClient } from "typedb-client/api/connection/TypeDBClient";
import { StateHook } from "./state-hook";
import { studioDarkTheme } from "../styles/theme";

export const typeDBClientState = new StateHook<TypeDBClient>(null);
export const databaseState = new StateHook<string>(null);
export const themeState = new StateHook(studioDarkTheme);
