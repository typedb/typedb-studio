/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import "typedb-web-common/lib/prism";

import { bootstrapApplication } from "@angular/platform-browser";
import { provideAnimations } from "@angular/platform-browser/animations";
import { environment } from "./environments/environment";
import { RootComponent } from "./root.component";
import { ErrorHandler, importProvidersFrom, inject } from "@angular/core";
import { MAT_RIPPLE_GLOBAL_OPTIONS, RippleGlobalOptions } from "@angular/material/core";
// import { FirebaseOptions, initializeApp, provideFirebaseApp } from "@angular/fire/app";
// import { getAuth, provideAuth } from "@angular/fire/auth";
import { provideHttpClient } from "@angular/common/http";
import { routes } from "./routing/routes";
import { TypeDBStudioTitleStrategy } from "./routing/title-strategy";
import { MAT_DIALOG_DEFAULT_OPTIONS, MAT_DIALOG_SCROLL_STRATEGY } from "@angular/material/dialog";
import { provideRouter, TitleStrategy } from "@angular/router";
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from "@angular/material/form-field";
import { MAT_TOOLTIP_DEFAULT_OPTIONS, MatTooltipDefaultOptions } from "@angular/material/tooltip";
import { MAT_CHECKBOX_DEFAULT_OPTIONS, MatCheckboxDefaultOptions } from "@angular/material/checkbox";
import Intercom from "@intercom/messenger-js-sdk";
import { Overlay } from "@angular/cdk/overlay";
import "posthog-js/dist/recorder";
import "posthog-js/dist/surveys";
import "posthog-js/dist/exception-autocapture";
import "posthog-js/dist/tracing-headers";
import "posthog-js/dist/web-vitals";
import "posthog-js/dist/dead-clicks-autocapture";
import posthog from "posthog-js/dist/module.no-external";
import { StudioErrorHandler } from "./service/error-handler.service";
import { provideMarkdown } from "ngx-markdown";

const rippleGlobalConfig: RippleGlobalOptions = {
    disabled: true,
    animation: {
        enterDuration: 0,
        exitDuration: 0,
    },
};

const tooltipGlobalConfig: MatTooltipDefaultOptions = {
    showDelay: 0,
    hideDelay: 0,
    touchendHideDelay: 0,
    position: "above",
};

const checkboxGlobalConfig: MatCheckboxDefaultOptions = {
    color: "primary"
};

// let firebaseOptions: FirebaseOptions & { tenantId: string } = { tenantId: "" };

if (environment.env !== "local") {
    const posthogProjectApiKey = environment.env === "production" ? "phc_w6b3dE1UxM9LKE2FLbDP9yiHFEXegbtxv1feHm0yigA" : "phc_kee7J4vlLnef61l6krVU8Fg5B6tYIgSEVOyW7yxwLSk";
    posthog.init(
        posthogProjectApiKey,
        {
            api_host: "https://typedb.com/ingest",
            ui_host: "https://us.posthog.com",
            person_profiles: "always",
            capture_pageview: false,
            capture_pageleave: true,
            disable_session_recording: true,
        }
    );
}

Intercom({
    app_id: "zof896ic",
    hide_default_launcher: true,
    custom_launcher_selector: ".ask-typedb-ai",
});

bootstrapApplication(RootComponent, {
    providers: [
        provideRouter(routes),
        { provide: TitleStrategy, useClass: TypeDBStudioTitleStrategy },
        provideHttpClient(),
        provideAnimations(),
        { provide: ErrorHandler, useClass: StudioErrorHandler },
        provideMarkdown(),
        importProvidersFrom(
            // provideFirebaseApp(() => initializeApp(firebaseOptions)),
            // provideAuth(() => {
            //     const auth = getAuth();
            //     auth.tenantId = firebaseOptions.tenantId;
            //     return auth;
            // }),
        ),
        {
            provide: MAT_DIALOG_SCROLL_STRATEGY,
            useFactory: () => {
                const overlay = inject(Overlay)
                return () => overlay.scrollStrategies.noop();
            }
        },
        { provide: MAT_DIALOG_DEFAULT_OPTIONS, useValue: { width: "480px" } },
        { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: "outline" } },
        { provide: MAT_RIPPLE_GLOBAL_OPTIONS, useValue: rippleGlobalConfig },
        { provide: MAT_TOOLTIP_DEFAULT_OPTIONS, useValue: tooltipGlobalConfig },
        { provide: MAT_CHECKBOX_DEFAULT_OPTIONS, useValue: checkboxGlobalConfig },
    ]
});
