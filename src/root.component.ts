/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
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
