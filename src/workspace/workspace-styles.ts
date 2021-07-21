import makeStyles from "@material-ui/core/styles/makeStyles";
import { ThemeProps } from "../styles/theme";

export const workspaceStyles = makeStyles({
    filler: {
        flex: 1,
    },

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

    querySplitPane: {
        position: "relative",
        height: "calc(100vh - 48px)",
    },

    editorPane: {
        height: "100%",
    },

    actionsBar: {
        height: 48,
        display: "flex",
        alignItems: "center",
        padding: "0 24px",
    },

    resultsPane: {
        height: "100%",
    },

    visualiser: {
        height: "calc(100% - 28px)",
        width: "100%",
        background: (props: ThemeProps) => props.theme.visualiser.colors.hex.background,
    },

    statusBar: {
        height: 28,
        display: "flex",
        alignItems: "center",
        padding: "0 8px",
        fontSize: 14,
        userSelect: "none",
    },
});
