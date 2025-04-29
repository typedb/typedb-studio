import { ErrorHandler, Injectable, Injector } from '@angular/core';
import { SnackbarService } from './snackbar.service';

@Injectable()
export class StudioErrorHandler implements ErrorHandler {
    constructor(private injector: Injector) {}

    handleError(err: any): void {
        // We use injector to get the service instead of direct injection to avoid circular dependency issues
        const snackbar = this.injector.get(SnackbarService);

        console.error(err);

        const msg = err?.message || err?.toString() || `Unknown error`;
        snackbar.errorPersistent(`Error: ${msg}`);
    }
}
