import { ThemeProvider } from "@material-ui/core/styles";
import React from "react";
import {indexStyles} from "./index-styles";
import { vaticleMuiTheme } from "./styles/theme";
import { StudioRouter } from "./router";

export const StudioApp: React.FC = () => {
    const classes = indexStyles();

    return (
        <ThemeProvider theme={vaticleMuiTheme}>
            <div className={classes.root}>
                <StudioRouter/>
            </div>
        </ThemeProvider>
    );
};
