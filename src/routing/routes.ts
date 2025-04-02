/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
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
