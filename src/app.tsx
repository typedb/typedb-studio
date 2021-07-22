import { ThemeProvider } from "@material-ui/core/styles";
import React from "react";
import { StudioSnackbar } from "./common/snackbar/snackbar";
import { SnackbarState } from "./common/snackbar/snackbar-state";
import { indexStyles } from "./index-styles";
import { StudioRouter } from "./router";
import { themeState } from "./state/state";
import { vaticleMuiTheme } from "./styles/theme";

export const StudioApp: React.FC = () => {
    const classes = indexStyles({ theme: themeState.use()[0] });

    const [snackbar, setSnackbar] = React.useState<SnackbarState>({
        message: "",
        variant: "error",
        open: false
    });

    return (
        <ThemeProvider theme={vaticleMuiTheme}>
            <SnackbarContext.Provider value={{ snackbar, setSnackbar }}>
                <div className={classes.root}>
                    <StudioRouter/>
                    <StudioSnackbar {...snackbar} setOpen={(value) => setSnackbar({ ...snackbar, open: value })}/>
                </div>
            </SnackbarContext.Provider>
        </ThemeProvider>
    );
};

export const SnackbarContext = React.createContext<{ snackbar?: SnackbarState, setSnackbar?: (value: SnackbarState) => void }>({});
