/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Component, Input, OnInit, ViewChild } from "@angular/core";
import { MatMenuModule, MatMenuTrigger } from "@angular/material/menu";
import { BaseConcept } from "../../../concept/base";
import { ResourceTable } from "../../../service/resource-table.service";
import { ButtonComponent, ButtonStyle, MenuItem } from "typedb-platform-framework";
import { sanitiseHtmlID } from "typedb-web-common/lib";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { FilterGroupComponent, FilterSpec } from "../filter-group";
import { MatIconModule } from "@angular/material/icon";

export interface TableToolbarAction {
    text: string;
    style?: ButtonStyle;
    enabled?: boolean;
    onClick: () => void;
}

@Component({
    selector: "tp-table-toolbar",
    templateUrl: "./table-toolbar.component.html",
    styleUrls: ["./table-toolbar.component.scss"],
    standalone: true,
    imports: [ButtonComponent, FilterGroupComponent, MatMenuModule, MatCheckboxModule, MatIconModule],
})
export class TableToolbarComponent<ENTITY extends BaseConcept> implements OnInit {
    @Input({ required: true }) tableTitle!: string;
    @Input({ required: true }) filterGroupId!: string;
    @Input({ required: true }) table!: ResourceTable<ENTITY, string>;
    @Input() actions?: TableToolbarAction[];
    @ViewChild(MatMenuTrigger) columnSwitcherTrigger?: MatMenuTrigger;

    availableFilterSpecs!: FilterSpec[];

    columnSwitcherMenuItems: MenuItem[] = [
        {
            label: "Select all columns",
            action: () => {
                this.setDisplayedColumnNames(this.table.properties.map(x => x.name));
            },
        },
        {
            label: "Unselect all columns",
            action: () => {
                this.setDisplayedColumnNames([this.table.properties.find(x => x.id === this.table.primaryProperty.id)!.name]);
            },
        },
    ];

    ngOnInit() {
        this.availableFilterSpecs = this.table.filterSpecs.filter(x => this.table.properties.some(y => y.id === x.property.id));
        this.columnSwitcherMenuItems.push(...this.table.properties.map(prop => {
            const item: MenuItem = {
                label: prop.name,
                action: () => {
                    if (this.table.displayedProperties.includes(prop)) {
                        this.table.displayedProperties.remove(prop);
                    } else {
                        const columnNames = [...this.table.displayedProperties.map(x => x.name), prop.name];
                        this.setDisplayedColumnNames(columnNames);
                    }
                },
                checkbox: true,
                disabled: prop.id === this.table.primaryProperty.id
            };
            return item;
        }));
    }

    isDisplayedColumn(item: MenuItem) {
        return this.table.displayedProperties.some(x => x.name === item.label);
    }

    setDisplayedColumnNames(columnNames: string[]) {
        this.table.displayedProperties = this.table.properties.filter(x => columnNames.includes(x.name));
    }

    get columnSwitcherIsOpen(): boolean {
        return this.columnSwitcherTrigger?.menuOpen || false;
    }

    buttonId(action: TableToolbarAction) {
        return sanitiseHtmlID(`${this.tableTitle}_${action.text}`);
    }
}
