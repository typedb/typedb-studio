/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

export type OS = "mac" | "windows" | "linux";

export function detectOS(): OS {
    // Prefer userAgentData if available
    const uaData = (navigator as any).userAgentData;

    if (uaData?.platform) {
        const platform = uaData.platform.toLowerCase();
        if (platform.includes("mac")) return "mac";
        if (platform.includes("win")) return "windows";
        if (platform.includes("linux") || platform.includes("chrome os")) return "linux";
    }

    // Fallback to userAgent string
    const ua = navigator.userAgent.toLowerCase();
    if (ua.includes("mac")) return "mac";
    if (ua.includes("win")) return "windows";
    if (ua.includes("linux")) return "linux";

    // Default fallback
    return "mac";
}
