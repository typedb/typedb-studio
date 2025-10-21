import { ErrorHandler, forwardRef, Inject, Injectable, Injector, NgZone } from '@angular/core';
import { SnackbarService } from './snackbar.service';
import { isApiErrorResponse } from 'typedb-driver-http';

@Injectable()
export class StudioErrorHandler implements ErrorHandler {
    constructor(
        @Inject(forwardRef(() => SnackbarService)) private snackbar: SnackbarService,
        @Inject(forwardRef(() => NgZone)) private ngZone: NgZone
    ) {}

    handleError(err: any): void {
        console.error(err);

        let msg = ``;
        // TODO: delete object check once we upgrade typedb-driver-http
        if (typeof err === "object" && isApiErrorResponse(err)) {
            msg = err.err.message;
        } else {
            msg = err?.message ?? err?.toString() ?? `Unknown error`;
        }
        // See: https://github.com/angular/components/issues/13181#issuecomment-423471381
        this.ngZone.run(() => {
            this.snackbar.errorPersistent(`Error: ${msg}`);
        });
    }
}
