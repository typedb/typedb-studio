import { SnackbarVariant } from "./snackbar";

export interface SnackbarState {
    open: boolean;
    message: string;
    variant: SnackbarVariant;
}
