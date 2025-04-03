/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Routes } from "@angular/router";

import { _404PageComponent } from "../module/404/404-page.component";
import { ConnectionCreatorComponent } from "../module/connection/create/connection-creator.component";
import { HomeComponent } from "../module/home/home.component";
import { QueryPageComponent } from "../module/query/query-page.component";

export const routes: Routes = [
    { path: "", component: HomeComponent, title: "Home", pathMatch: "full" },
    { path: "connections/new", component: ConnectionCreatorComponent, title: "Create connection" },
    { path: "query", component: QueryPageComponent, title: "Query" },
    { path: "**", component: _404PageComponent, title: "404" },
];
