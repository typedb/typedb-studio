/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, Input } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatSortModule } from "@angular/material/sort";
import { MatTableModule } from "@angular/material/table";
import { MatTreeModule } from "@angular/material/tree";
import { MatTooltipModule } from "@angular/material/tooltip";
import { SchemaTreeNode } from "../../../service/schema-tool-window-state.service";

@Component({
    selector: "ts-schema-tree-node",
    templateUrl: "schema-tree-node.component.html",
    styleUrls: ["schema-tree-node.component.scss"],
    imports: [
        MatDividerModule, MatFormFieldModule, MatTreeModule, MatIconModule,
        MatInputModule, FormsModule, ReactiveFormsModule, MatButtonToggleModule,
        MatTableModule, MatSortModule, MatTooltipModule, MatButtonModule,
    ]
})
export class SchemaTreeNodeComponent {
    @Input({ required: true }) data!: SchemaTreeNode;
}
