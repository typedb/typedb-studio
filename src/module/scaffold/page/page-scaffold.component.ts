/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { animate, state, style, transition, trigger } from "@angular/animations";
import { NgClass } from "@angular/common";
import { AfterViewInit, Component, ElementRef, Input, ViewChild } from "@angular/core";
import { MatSidenavModule } from "@angular/material/sidenav";
import { ButtonComponent } from "../../../framework/button/button.component";
import { SpinnerComponent } from "../../../framework/spinner/spinner.component";
import { AppData } from "../../../service/app-data.service";
import { ConnectionWidgetComponent } from "../../connection/widget/connection-widget.component";
import { RightSidebarComponent } from "../sidebar/right/right-sidebar.component";
import { SidebarComponent } from "../sidebar/sidebar.component";

export type ResourceAvailability = "ready" | "loading" | "failed";

@Component({
    selector: "ts-page-scaffold",
    templateUrl: "./page-scaffold.component.html",
    styleUrls: ["./page-scaffold.component.scss"],
    imports: [
        SidebarComponent, RightSidebarComponent, SpinnerComponent, NgClass, MatSidenavModule, ButtonComponent, ConnectionWidgetComponent,
    ],
    animations: [
        trigger("sidebarLeftMargin", [
            state("open", style({ "margin-left": "289px" })),
            state("collapsed", style({ "margin-left": "101px" })),
        ])
    ]
})
export class PageScaffoldComponent implements AfterViewInit {
    @ViewChild("actionBar") actionBar!: ElementRef;
    @Input() pageAvailability: ResourceAvailability | null = "ready";
    @Input() hideTransactionWidget = false;

    leftSidebarState = this.appData.viewState.sidebarState();
    condensed = false;
    initializing = true;

    constructor(private appData: AppData) {
        // Disable CSS transitions during initial render to prevent sidebar flicker
        setTimeout(() => this.initializing = false);
    }

    ngAfterViewInit() {
        // actionBar may not exist if pageAvailability is not "ready"
        if (!this.actionBar?.nativeElement) return;

        const observer = new ResizeObserver(entries => {
            const actionBarWidth = entries[0].contentRect.width;
            this.condensed = actionBarWidth < 1200;
        });

        observer.observe(this.actionBar.nativeElement);
    }

    get sidebarLeftMarginAnimationState() {
        return this.leftSidebarState === 'collapsed' ? 'collapsed' : 'open';
    }

    get actionBarClass() {
        return this.condensed ? `action-bar condensed` : `action-bar`;
    }
}
