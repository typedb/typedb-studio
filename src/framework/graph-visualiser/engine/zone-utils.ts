/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Registers rAF loops / high-frequency DOM listeners in the zone.js root zone,
 * so they don't trigger app-wide change detection on every frame or mouse move.
 * The engine is framework-agnostic, hence no NgZone; no-op without zone.js.
 * Callbacks re-run (and re-schedule) in the zone they were registered from.
 */
export function runOutsideAngularZone<T>(fn: () => T): T {
    const zone = (globalThis as any).Zone;
    if (zone?.root && zone.current !== zone.root) return zone.root.run(fn);
    return fn();
}
