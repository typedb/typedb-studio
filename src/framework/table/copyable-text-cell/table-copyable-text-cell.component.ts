/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, Input } from "@angular/core";
import { SnackbarService } from "../../../service/snackbar.service";
import { MatButtonModule } from "@angular/material/button";
import { MatTooltipModule } from "@angular/material/tooltip";

@Component({
    selector: "tp-table-copyable-text-cell",
    templateUrl: "./table-copyable-text-cell.component.html",
    styleUrls: ["./table-copyable-text-cell.component.scss"],
    standalone: true,
    imports: [MatButtonModule, MatTooltipModule],
})
export class TableCopyableTextCellComponent {
    @Input({ required: true }) text!: string;

    constructor(private snackbar: SnackbarService) {
    }

    copyText() {
        navigator.clipboard.writeText(this.text);
        this.snackbar.success("Copied", { duration: 1250 });
    }
}
