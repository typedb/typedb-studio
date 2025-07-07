/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { AfterViewInit, Component, DestroyRef, ElementRef, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSortModule } from "@angular/material/sort";
import { MatTooltipModule } from "@angular/material/tooltip";
import { RouterLink } from "@angular/router";
import { ResizableDirective } from "@hhangular/resizable";
import { filter, map, Observable, startWith } from "rxjs";
import { AppData } from "../../service/app-data.service";
import { DriverState } from "../../service/driver-state.service";
import { SchemaState } from "../../service/schema-state.service";
import { SnackbarService } from "../../service/snackbar.service";
import { PageScaffoldComponent } from "../scaffold/page/page-scaffold.component";

@Component({
    selector: "ts-schema-tool",
    templateUrl: "schema-tool.component.html",
    styleUrls: ["schema-tool.component.scss"],
    imports: [
        RouterLink, AsyncPipe, PageScaffoldComponent, MatDividerModule, MatFormFieldModule,
        MatInputModule, FormsModule, ReactiveFormsModule, MatButtonToggleModule,
        MatSortModule, MatTooltipModule, MatButtonModule,
    ]
})
export class SchemaToolComponent implements OnInit, AfterViewInit, OnDestroy {

    @ViewChild("articleRef") articleRef!: ElementRef<HTMLElement>;
    @ViewChildren("graphViewRef") graphViewRef!: QueryList<ElementRef<HTMLElement>>;
    @ViewChildren(ResizableDirective) resizables!: QueryList<ResizableDirective>;
    private canvasEl$!: Observable<HTMLElement>;

    constructor(
        protected state: SchemaState, public driver: DriverState, private appData: AppData,
        private snackbar: SnackbarService, private destroyRef: DestroyRef) {
    }

    ngOnInit() {
        this.appData.viewState.setLastUsedTool("schema");
    }

    ngAfterViewInit() {
        if (this.resizables.length) {
            const articleWidth = this.articleRef.nativeElement.clientWidth;
            this.resizables.first.percent = (articleWidth * 0.15 + 100) / articleWidth * 100;
        }
        this.canvasEl$ = this.graphViewRef.changes.pipe(
            map(x => x as QueryList<ElementRef<HTMLElement>>),
            startWith(this.graphViewRef),
            filter(queryList => queryList.length > 0),
            map(x => x.first.nativeElement),
        );
        this.canvasEl$.subscribe(canvasEl => {
            this.state.visualiser.canvasEl$.next(canvasEl);
            if (this.state.visualiser.visualiser?.graph.nodes().length) {
                this.state.visualiser.visualiser.sigma.scheduleRender();
            }
        });
        this.state.queryResponses$.pipe(
            takeUntilDestroyed(this.destroyRef),
            filter(x => !!x),
            map(x => x!)
        ).subscribe((queryResponses) => {
            queryResponses.forEach(x => this.state.visualiser.push(x));
        });
    }

    ngOnDestroy() {
        this.state.visualiser.destroy();
    }

    readonly JSON = JSON;
}
