/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
