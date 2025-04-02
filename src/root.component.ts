/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AfterViewInit, Component } from "@angular/core";
import { SpinnerComponent } from "./framework/spinner/spinner.component";
import { AnalyticsService } from "./service/analytics.service";
import { GuardsCheckEnd, GuardsCheckStart, NavigationCancel, Router, RouterOutlet, Event as RouterEvent, NavigationEnd } from "@angular/router";
import { EMPTY, filter, of, switchMap } from "rxjs";
import { AsyncPipe } from "@angular/common";

@Component({
    selector: "ts-root", // eslint-disable-line @angular-eslint/component-selector
    templateUrl: "./root.component.html",
    styleUrls: ["root.component.scss"],
    standalone: true,
    imports: [RouterOutlet, SpinnerComponent, AsyncPipe],
})
export class RootComponent implements AfterViewInit {
    routeIsLoading$ = this.router.events.pipe(
        switchMap((event) => {
            if (event instanceof GuardsCheckStart) {
                return of(true);
            } else if (event instanceof GuardsCheckEnd || event instanceof NavigationCancel) {
                return of(false);
            } else {
                return EMPTY;
            }
        })
    );

    constructor(private analytics: AnalyticsService, private router: Router) {
        this.informAnalyticsOnPageView(router, analytics);
    }

    private informAnalyticsOnPageView(router: Router, analytics: AnalyticsService) {
        router.events.pipe(filter((event: RouterEvent) => event instanceof NavigationEnd)).subscribe(() => {
            analytics.posthog.capturePageView();
            analytics.cio.page();
        });
    }

    ngAfterViewInit() {
        this.analytics.google.loadScriptTag();
    }
}
