/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Component, OnInit, Input } from "@angular/core";
import { FormGroup, FormControl, Validators, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { BaseConcept } from "../../../concept/base";
import { ResourceTable } from "../../../service/resource-table.service";
import { AsyncPipe } from "@angular/common";
import { Observable, combineLatest, map } from "rxjs";
import { MatSelectModule } from "@angular/material/select";

const DEFAULT_ITEMS_PER_PAGE = 10;

@Component({
    selector: "tp-table-paginator",
    templateUrl: "./table-paginator.component.html",
    styleUrls: ["./table-paginator.component.scss"],
    standalone: true,
    imports: [FormsModule, ReactiveFormsModule, AsyncPipe, MatSelectModule],
})
export class TablePaginatorComponent<ENTITY extends BaseConcept> implements OnInit {
    readonly itemsPerPageOptions = [10, 20, 30, 50];
    readonly stringify = (val: number) => {
        return val.toString();
    };
    public formGroup!: FormGroup;
    @Input({ required: true }) table!: ResourceTable<ENTITY, string>;
    currentPageInfoText$!: Observable<string>;
    nextPageButtonEnabled$!: Observable<boolean>;

    get pageNumber(): FormControl<number> {
        return this.formGroup.get("pageNumber") as FormControl<number>;
    }

    get rowsPerPage(): FormControl<number> {
        return this.formGroup.get("rowsPerPage") as FormControl<number>;
    }

    constructor() {}

    ngOnInit() {
        this.formGroup = new FormGroup({
            rowsPerPage: new FormControl<number>(DEFAULT_ITEMS_PER_PAGE, [
                Validators.required,
            ]),
            pageNumber: new FormControl<number>(1, [Validators.required]),
        });
        this.formGroup.valueChanges.subscribe((data) =>
            this.onFormUpdate(data)
        );
        this.currentPageInfoText$ = combineLatest([this.table.items$, this.table.totalItems$]).pipe(
            map(([items, total]) => {
                if (items == null || total == null) return "";
                if (!items.length) return "(no items)";
                const startIndex = (this.pageNumber.value - 1) * this.rowsPerPage.value + 1; // 1-indexed
                const endIndex = startIndex + items.length - 1;
                return `${startIndex} – ${endIndex} of ${total}`;
            }),
        );
        this.nextPageButtonEnabled$ = this.table.totalItems$.pipe(map((total) => total === "many"));
    }

    private onFormUpdate(data: { rowsPerPage?: number; pageNumber?: number; }) {
        if (data.rowsPerPage && data.pageNumber) {
            const offset = data.rowsPerPage * (data.pageNumber - 1);
            const limit = data.rowsPerPage;
            this.table.pagination$.next({ offset, limit });
        }
    }

    toPreviousPage() {
        this.pageNumber.patchValue(this.pageNumber.value - 1);
    }

    toNextPage() {
        this.pageNumber.patchValue(this.pageNumber.value + 1);
    }

    get prevPageButtonEnabled(): boolean {
        return this.pageNumber.value > 1;
    }
}
