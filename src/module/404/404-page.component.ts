/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Component } from "@angular/core";
import { RouterLink } from "@angular/router";
import { ButtonComponent } from "../../framework/button/button.component";
import { PageScaffoldComponent } from "../scaffold/page/page-scaffold.component";

@Component({
    selector: "tp-404-page",
    templateUrl: "404-page.component.html",
    styleUrls: ["404-page.component.scss"],
    standalone: true,
    imports: [ButtonComponent, RouterLink, PageScaffoldComponent],
})
export class _404PageComponent {}
