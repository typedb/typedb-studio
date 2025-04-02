/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
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
