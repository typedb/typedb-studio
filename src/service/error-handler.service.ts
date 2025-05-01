import { ErrorHandler, forwardRef, Inject, Injectable, Injector, NgZone } from '@angular/core';
import { SnackbarService } from './snackbar.service';

@Injectable()
export class StudioErrorHandler implements ErrorHandler {
    constructor(
        @Inject(forwardRef(() => SnackbarService)) private snackbar: SnackbarService,
        @Inject(forwardRef(() => NgZone)) private ngZone: NgZone
    ) {}

    handleError(err: any): void {
        console.error(err);

        const msg = err?.message || err?.toString() || `Unknown error`;
        // See: https://github.com/angular/components/issues/13181#issuecomment-423471381
        this.ngZone.run(() => {
            this.snackbar.errorPersistent(`Error: ${msg}`);
        });
    }
}
