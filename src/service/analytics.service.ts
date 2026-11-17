/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { environment } from "../environments/environment";
import posthog, { Properties } from "posthog-js/dist/module.no-external";

const GOOGLE_TAG_ID = "G-SNVZCNLJ9R"; // used by Google Analytics

@Injectable({
    providedIn: "root",
})
export class AnalyticsService {
    public constructor() {}

    posthog = {
        alias: (alias: string, original?: string) => {
            posthog.alias(alias, original);
        },
        capture: (event: string, properties?: Properties) => {
            posthog.capture(event, properties);
        },
        capturePageView: () => {
            posthog.capture("$pageview");
        },
        identify: (id: string, userPropertiesToSet?: Properties) => {
            posthog.identify(id, userPropertiesToSet);
        },
        reset: () => {
            posthog.reset();
        },
        set: (userPropertiesToSet: Properties) => {
            posthog.setPersonProperties(userPropertiesToSet);
        },
    };

    google = {
        loadScriptTag: () => {
            if (environment.env !== "production") return;
            const scriptEl = document.createElement("script");
            scriptEl.src = `https://www.googletagmanager.com/gtag/js?id=${GOOGLE_TAG_ID}`;
            const scriptEl2 = document.createElement("script");
            scriptEl2.innerHTML = `window.dataLayer = window.dataLayer || [];
        function gtag(){dataLayer.push(arguments);}
        gtag('js', new Date());
        gtag('config', '${GOOGLE_TAG_ID}');`;
            document.head.appendChild(scriptEl);
            document.head.appendChild(scriptEl2);
        },
    };
}
