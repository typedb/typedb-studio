import { ThemeProvider } from "@material-ui/core/styles";
import React, { useState } from "react";
import { StudioSnackbar } from "./common/snackbar/snackbar";
import { SnackbarState } from "./common/snackbar/snackbar-state";
import {indexStyles} from "./index-styles";
import { vaticleMuiTheme } from "./styles/theme";
import { StudioRouter } from "./router";

export const StudioApp: React.FC = () => {
    const classes = indexStyles();

    const [snackbar, setSnackbar] = useState<SnackbarState>({
        message: "",
        variant: "error",
        open: false
    });

    return (
        <ThemeProvider theme={vaticleMuiTheme}>
            <SnackbarContext.Provider value={{snackbar, setSnackbar}}>
                <div className={classes.root}>
                    <StudioRouter/>
                    <StudioSnackbar {...snackbar} setOpen={(value) => setSnackbar({...snackbar, open: value})}/>
                </div>
            </SnackbarContext.Provider>
        </ThemeProvider>
    );
};

export const SnackbarContext = React.createContext<{snackbar?: SnackbarState, setSnackbar?: (value: SnackbarState) => void}>({});
