/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { Title } from "@angular/platform-browser";
import { RouterStateSnapshot, TitleStrategy } from "@angular/router";

@Injectable({
    providedIn: "root",
})
export class TypeDBStudioTitleStrategy extends TitleStrategy {
    constructor(private title: Title) {
        super();
    }

    override updateTitle(routerState: RouterStateSnapshot): void {
        const routeTitle = this.buildTitle(routerState);
        let fullTitle = `TypeDB Studio`;
        if (routeTitle) fullTitle += ` - ${routeTitle}`;
        this.title.setTitle(fullTitle);
    }
}
