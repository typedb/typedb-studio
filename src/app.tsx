import React from "react";
import {indexStyles} from "./index-styles";
import { vaticleMuiTheme } from "./styles/theme";

export const StudioApp: React.FC = () => {
    const classes = indexStyles();

    return (
        <ThemeProvider theme={vaticleMuiTheme}>
            <div className={classes.root}>
                <VaticleRouter/>
            </div>
        </ThemeProvider>
    );
};
