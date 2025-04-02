/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

export interface MenuItem {
    label: string;
    action: () => void;
    checkbox?: boolean;
    disabled?: boolean;
    disabledReason?: string;
    dangerous?: boolean;
}
