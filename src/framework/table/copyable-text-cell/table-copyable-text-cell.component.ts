/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
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
