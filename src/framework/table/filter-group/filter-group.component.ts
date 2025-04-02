/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from "@angular/core";
import { AbstractControl, FormControl, FormGroup, FormBuilder, ValidationErrors, ValidatorFn, Validators, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { Filter, filterOf, FilterOperator, FilterValue } from "../../../concept/common";
import { isBlank } from "../../../util";
import { FilterOperatorPipe } from "./filter-operator.pipe";
import { FilterSpec } from "./filter-spec";
import { ActivatedRoute, ParamMap, Router } from "@angular/router";
import { Subscription, debounceTime, first } from "rxjs";
import { NgClass } from "@angular/common";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { MatInputModule } from "@angular/material/input";
import { FilterValuePipe } from "./filter-value.pipe";
import { MatButtonModule } from "@angular/material/button";

interface URLEncodedFilter {
    prop: string;
    op: string;
    val: string;
}
const urlEncodedFilterProps = ["prop", "op", "val"];

interface FormEntry {
    filterSpec: FilterSpec | null;
    operator: FilterOperator | null;
    value: string | null;
}

type FormControlOf<T extends object> = {
    [key in keyof T]: FormControl<T[key]>;
};

@Component({
    selector: "tp-filter-group",
    templateUrl: "filter-group.component.html",
    styleUrls: ["filter-group.component.scss"],
    standalone: true,
    imports: [
        FormsModule, ReactiveFormsModule, NgClass, MatFormFieldModule, MatSelectModule, MatInputModule,
        FilterOperatorPipe, FilterValuePipe, MatButtonModule
    ],
})
export class FilterGroupComponent implements OnInit, OnDestroy {
    @Input() filterSpecs: FilterSpec[] = [];
    @Input() identifier!: string;
    @Output() filtersChange = new EventEmitter<Filter[]>();

    formGroups = this.formBuilder.array<FormGroup<FormControlOf<FormEntry>>>([]);
    private subs: Subscription[] = [];
    private readonly maxFilters = 12;

    constructor(private formBuilder: FormBuilder, private route: ActivatedRoute, private router: Router) {}

    get canAddFilter(): boolean {
        return this.allFiltersAreComplete && this.formGroups.controls.length < this.maxFilters;
    }

    addFilter() {
        this.formGroups.push(this.newFormGroup());
    }

    private emitCompletedFilters(formEntries: Partial<FormEntry>[]) {
        const filters: Filter[] = [];
        for (const entry of formEntries) {
            if (entry.filterSpec
                && entry.operator && this.isValidOperator(entry.operator, entry.filterSpec)
                && entry.value && this.isValidValue(entry.value, entry.filterSpec)
            ) {
                filters.push(filterOf(entry.filterSpec, entry.operator, entry.value));
            }
        }
        this.filtersChange.emit(filters);
    }

    private pushCompletedFiltersToUrl(formEntries: Partial<FormEntry>[]) {
        const params = new URL(window.location.href).searchParams;
        for (const [searchKey] of new URL(window.location.href).searchParams) {
            if (searchKey.startsWith(this.filterPrefix())) {
                params.delete(searchKey);
            }
        }
        for (const [index, entry] of formEntries.entries()) {
            if (entry.filterSpec
                && entry.operator && this.isValidOperator(entry.operator, entry.filterSpec)
                && entry.value && this.isValidValue(entry.value, entry.filterSpec)
            ) {
                const filter = filterOf(entry.filterSpec, entry.operator, entry.value);
                params.set(
                    `${this.filterPrefix()}.${index}`,
                    JSON.stringify(this.encodeFilterToURL(filter)),
                );
            }
        }
        if (params != new URL(window.location.href).searchParams) {
            this.router.navigate([], {
                relativeTo: this.route,
                queryParams: Object.fromEntries(params.entries()),
                replaceUrl: true,
            });
        }
    }

    private isValidValue(value: string, spec: FilterSpec) {
        if (isBlank(value)) return false;
        if (spec.validValues && !spec.validValues.includes(value)) return false;
        switch (spec.valueType) {
            case "long":
            case "double":
                if (Number.isNaN(Number(value))) return false;
                break;
            default:
                break;
        }
        return true;
    }

    private loadCompletedFiltersFromUrl(queryParams: ParamMap) {
        const url = new URL(window.location.href);
        const queryParamsWithoutFilters = Object.fromEntries(
            url.searchParams.entries()
        );

        const filterQueryParams = FilterGroupComponent.paramMapToKeyValues(
            queryParams
        ).filter(({ key }) => key.startsWith(this.filterPrefix()));
        for (const { key, value } of filterQueryParams) {
            const formGroup = this.loadFilterFromQueryParameter(value);
            if (formGroup) {
                this.formGroups.push(formGroup);
            } else {
                delete queryParamsWithoutFilters[key];
            }
        }

        this.router.navigate([url.pathname], {
            queryParams: queryParamsWithoutFilters,
        });
    }

    private loadFilterFromQueryParameter(parameter: string): FormGroup<FormControlOf<FormEntry>> | null {
        const urlEncodedFilter: URLEncodedFilter = JSON.parse(parameter);
        if (!urlEncodedFilter || urlEncodedFilterProps.some((attribute) => !(attribute in urlEncodedFilter))) return null;

        const filterSpec = this.filterSpecs.find((spec) => spec.property.id === urlEncodedFilter.prop);
        if (!filterSpec) return null;

        const filter = this.decodeFilterFromURL(urlEncodedFilter, filterSpec);
        if (!filter) return null;

        return this.newFormGroup({
            filterSpec,
            operator: filter.operator,
            value: filter.rawValue,
        });
    }

    private encodeFilterToURL(filter: Filter): URLEncodedFilter {
        return {
            prop: filter.property.id,
            op: filter.operator,
            val: this.encodeFilterValueToURL(filter.value),
        };
    }

    private encodeFilterValueToURL(value: FilterValue): string {
        if ("boolean" in value) return value.boolean.toString();
        else if ("long" in value) return value.long.toString();
        else if ("double" in value) return value.double.toString();
        else if ("string" in value) return value.string;
        else return value.date.toISOString();
    }

    private decodeFilterFromURL(filter: URLEncodedFilter, spec: FilterSpec): Filter & { rawValue: string } | null {
        if (!spec.operators.includes(filter.op as FilterOperator)) return null;
        const partialFilter = { property: spec.property, operator: filter.op as FilterOperator } as const;
        switch (spec.valueType) {
            case "boolean":
                if (!["true", "false"].includes(filter.val)) return null;
                return { ...partialFilter, value: { boolean: filter.val === "true" }, rawValue: filter.val === "true" ? "True" : "False" };
            case "long":
                if (isNaN(Number(filter.val))) return null;
                return { ...partialFilter, value: { long: Number(filter.val) }, rawValue: filter.val };
            case "double":
                if (isNaN(Number(filter.val))) return null;
                return { ...partialFilter, value: { double: Number(filter.val) }, rawValue: filter.val };
            case "string":
                if (isBlank(filter.val)) return null;
                return { ...partialFilter, value: { string: filter.val }, rawValue: filter.val };
            case "date":
                // Date validation is hard.
                return { ...partialFilter, value: { date: new Date(filter.val)}, rawValue: new Date(filter.val).toISOString() };
        }
    }

    private static paramMapToKeyValues(paramMap: ParamMap) {
        return paramMap.keys.filterMap((key) => {
            const value = paramMap.get(key);
            return value ? { key, value } : undefined;
        });
    }

    private filterPrefix() {
        return `filter.${this.identifier}`;
    }

    private isValidOperator(operator: FilterOperator, spec: FilterSpec) {
        return spec.operators.includes(operator);
    }

    get allFiltersAreComplete(): boolean {
        return this.formGroups.controls.every((formGroup) => {
            return (
                formGroup.controls.filterSpec.value &&
                formGroup.controls.operator.value &&
                formGroup.controls.value.value
            );
        });
    }

    ngOnInit() {
        this.subs.push(
            this.formGroups.valueChanges.pipe(debounceTime(300)).subscribe(
                (formEntries: Partial<FormEntry>[]) => {
                    this.emitCompletedFilters(formEntries);
                    this.pushCompletedFiltersToUrl(formEntries);
                }
            )
        );
        this.subs.push(
            this.route.queryParamMap
                .pipe(first())
                .subscribe((queryParamsMap) => {
                    this.loadCompletedFiltersFromUrl(queryParamsMap);
                    if (this.formGroups.controls.length === 0)
                        this.addFilter();
                })
        );
    }

    ngOnDestroy() {
        for (const sub of this.subs) {
            sub.unsubscribe();
        }
    }

    formEntry(index: number): FormEntry | undefined {
        const entry = this.formGroups.at(index)?.value;
        if (entry === undefined) return undefined;
        return {
            filterSpec: entry.filterSpec ?? null,
            operator: entry.operator ?? null,
            value: entry.value ?? null,
        };
    }

    private newFormGroup(formEntry: FormEntry | undefined = undefined): FormGroup<FormControlOf<FormEntry>> {
        const validators: ValidatorFn = (
            formGroup: AbstractControl<FormEntry>
        ) => this.entryValidationErrors(formGroup.value);

        const formGroup = new FormGroup(
            {
                filterSpec: new FormControl(null as FilterSpec | null),
                operator: new FormControl(null as FilterOperator | null, [Validators.required]),
                value: new FormControl(null as string | null, [Validators.required]),
            },
            { validators }
        );
        if (formEntry) formGroup.patchValue({ ...formEntry });
        return formGroup;
    }

    private entryValidationErrors(entry: FormEntry): ValidationErrors | null {
        if (!entry.filterSpec) return null;
        const filterSpec = this.filterSpecs.find(
            (spec) => spec.property.id === entry.filterSpec?.property.id
        );
        if (!filterSpec || (entry.value != null && this.isValidValue(entry.value, filterSpec))) {
            return null;
        }
        return {
            invalidValue: entry.value,
        };
    }
}
