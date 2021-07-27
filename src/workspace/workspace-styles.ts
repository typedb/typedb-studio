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
        borderBottom: (props: ThemeProps) => `1px solid ${props.theme.panelSeparatorColor}`,
        display: "flex",
        alignItems: "center",
        padding: "0 16px",

        "& > * + *": {
            marginLeft: 8,
        },
    },

    workspaceView: {
        width: "100vw",
        height: "calc(100% - 76px)",
        display: "flex",
    },

    workspaceSplitPane: {
        flex: 1,
        position: "relative",
    },

    leftSidebar: {
        borderRight: (props: ThemeProps) => `1px solid ${props.theme.panelSeparatorColor}`,
    },

    rightSidebar: {
        borderLeft: (props: ThemeProps) => `1px solid ${props.theme.panelSeparatorColor}`,
    },

    sidebarTab: {
        fontSize: 14,
        background: (props: ThemeProps) => `${props.theme.tabs.selected.background} !important`,
        color: (props: ThemeProps) => `${props.theme.tabs.selected.color} !important`,
        borderBottom: (props: ThemeProps) => `1px solid ${props.theme.panelSeparatorColor}`,

        "&:hover": {
            background: (props: ThemeProps) => `${props.theme.tabs.hover.background} !important`,
        },
    },

    querySplitPane: {
        position: "relative",
        height: "calc(100vh - 76px)",
    },

    editorPane: {
        height: "100%",
    },

    editorTabs: {
        height: "100%", // TODO: replace these annoying calcs with flexbox where possible
    },

    editorTabGroup: {
        height: 30,
        borderBottom: (props: ThemeProps) => `1px solid ${props.theme.panelSeparatorColor}`,
    },

    editorTab: {
        flex: "0 !important",
        padding: "2px 8px 4px",
        fontSize: 14,
        lineHeight: "20px",
        height: 30,
    },

    editorTabPanel: {
        height: "calc(100% - 30px)",
    },

    actionsBar: {
        height: 48,
        display: "flex",
        alignItems: "center",
        padding: "0 8px",
        borderBottom: (props: ThemeProps) => `1px solid ${props.theme.panelSeparatorColor}`,

        "& > * + *": {
            marginLeft: 8,
        },
    },

    resultsPane: {
        height: "100%",
    },

    resultsTabs: {
        height: "100%",
    },

    resultsTabGroup: {
        height: 34,
        borderBottom: (props: ThemeProps) => `1px solid ${props.theme.panelSeparatorColor}`,
    },

    resultsTab: {
        height: 34,
    },

    resultsTabPanel: {
        height: "calc(100% - 34px)",
        background: (props: ThemeProps) => props.theme.visualiser.colors.hex.background,
    },

    resultsLogPanel: {
        padding: 8,
    },

    resultsTablePanel: {
        padding: 8,
    },

    visualiser: {
        height: "100%",
    },

    sidebarWindowGroup: {
        height: "100%",
        position: "relative",
        fontSize: 14,
    },

    sidebarWindowHeader: {
        height: 30,
        lineHeight: "30px",
        userSelect: "none",
        background: (props: ThemeProps) => props.theme.tabs.selected.background,
        color: (props: ThemeProps) => props.theme.tabs.selected.color,
        borderBottom: (props: ThemeProps) => `1px solid ${props.theme.panelSeparatorColor}`,
        padding: "0 8px",
    },

    querySettingsBody: {
        padding: 8,
        lineHeight: "16px",
    },

    graphExplorerBody: {
        lineHeight: "16px",
    },

    graphExplorerTable: {
        lineHeight: "20px",
        userSelect: "none",

        "& tr td:first-child": {
            fontWeight: 600,
        },
    },

    statusBar: {
        borderTop: (props: ThemeProps) => `1px solid ${props.theme.panelSeparatorColor}`,
        height: 28,
        display: "flex",
        alignItems: "center",
        padding: "0 8px",
        fontSize: 14,
        userSelect: "none",
    },
});
