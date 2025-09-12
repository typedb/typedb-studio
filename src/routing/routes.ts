/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { inject } from "@angular/core";
import { CanActivateFn, Router, Routes } from "@angular/router";
import { of } from "rxjs";
import { ADDRESS, USERNAME } from "../framework/util/url-params";

import { _404PageComponent } from "../module/404/404-page.component";
import { ConnectionCreatorComponent } from "../module/connection/create/connection-creator.component";
import { HomeComponent } from "../module/home/home.component";
import { QueryPageComponent } from "../module/query/query-page.component";
import { UsersPageComponent } from "../module/user/users-page.component";
import { AppData } from "../service/app-data.service";
import { SchemaPageComponent } from "../module/schema/schema-page.component";

const connectGuard: CanActivateFn = (route) => {
    const [address, username] = [route.queryParamMap.get(ADDRESS), route.queryParamMap.get(USERNAME)];
    if (username == null || address == null) return true;
    const appData = inject(AppData);
    const startupConnection = appData.connections.findStartupConnection();
    if (!startupConnection) return true;
    if (startupConnection.params.username !== username || !("addresses" in startupConnection.params) || startupConnection.params.addresses[0] !== address) return true;
    const router = inject(Router);
    switch (appData.viewState.lastUsedTool()) {
        case "query": return of(router.parseUrl(`query`));
        case "explore": return of(router.parseUrl(`explore`));
        case "schema": return of(router.parseUrl(`schema`));
    }
}

export const routes: Routes = [
    { path: "", component: HomeComponent, title: "Home", pathMatch: "full" },
    { path: "connect", component: ConnectionCreatorComponent, canActivate: [connectGuard], title: "Connect" },
    { path: "query", component: QueryPageComponent, title: "Query" },
    { path: "schema", component: SchemaPageComponent, title: "Schema" },
    { path: "users", component: UsersPageComponent, title: "Users" },
    { path: "**", component: _404PageComponent, title: "404" },
];
