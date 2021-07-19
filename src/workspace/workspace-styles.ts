import makeStyles from "@material-ui/core/styles/makeStyles";
import { ThemeProps } from "../styles/theme";

export const workspaceStyles = makeStyles({
    root: {
        height: "100vh",
        width: "100vw",
        display: "flex",
        flexDirection: "column",
    },

    appBar: {
        height: 48,
        background: (props: ThemeProps) => props.theme.windowBackground,
    },

    codeEditor: {
        background: (props: ThemeProps) => props.theme.textField.background,
    },

    visualiser: {
        height: "100%",
        width: "100%",
    },
});
