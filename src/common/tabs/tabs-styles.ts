import makeStyles from "@material-ui/core/styles/makeStyles";
import { ThemeProps } from "../../styles/theme";

export const tabsStyles = makeStyles({
    root: {
        display: "flex",
        flexDirection: "column",
    },

    tabGroup: {
        minHeight: "0 !important",
        display: "flex",
        borderBottom: (props: ThemeProps) => `1px solid ${props.theme.panelSeparatorColor}`,
    },

    tab: {
        flex: 1,
        display: "flex",
        flexDirection: "column",
        justifyContent: "center",
        alignItems: "center",
        transition: "background-color 150ms ease",
        userSelect: "none",
        padding: "0 10px 2px",
        minHeight: "unset",
        minWidth: "unset",
        overflow: "visible",
        opacity: 1,
        textTransform: "none",
        position: "relative",
        background: (props: ThemeProps) => props.theme.tabs.background,
        color: (props: ThemeProps) => props.theme.tabs.color,

        // This structure is used to avoid modifying the width of the tab
        "&:after": {
            content: "''",
            display: "block",
            position: "absolute",
            top: 0,
            right: -1,
            bottom: 0,
            left: 0,
            borderRight: (props: ThemeProps) => `1px solid ${props.theme.tabs.separatorColor}`,
            pointerEvents: "none",
        },

        "&:hover": {
            background: (props: ThemeProps) => props.theme.tabs.hover.background,
        },

        "&$withCloseButton": {
            paddingRight: 4,
        },
    },

    withCloseButton: {},

    tabLabel: {
        display: "flex",
        alignItems: "center",
    },

    tabSelected: {
        background: (props: ThemeProps) => `${props.theme.tabs.selected.background} !important`,
        color: (props: ThemeProps) => `${props.theme.tabs.selected.color} !important`,
    },

    indicator: {
        background: (props: ThemeProps) => props.theme.tabs.selected.indicatorColor,
    },

    close: {
        marginTop: 1,
        marginLeft: 2,
        fontSize: 14,
        padding: 2,
    },

    closeIcon: {
        fontSize: 14,
    },

    addButton: {
        background: (props: ThemeProps) => props.theme.tabs.background,
        color: (props: ThemeProps) => `${props.theme.tabs.color}`,
        borderColor: (props: ThemeProps) => props.theme.tabs.separatorColor,
        borderRadius: 0,
        borderBottomColor: (_props: ThemeProps) => "transparent",
        height: 29,
        width: 29,
        paddingBottom: 4,

        "&:hover": {
            background: (props: ThemeProps) => `${props.theme.tabs.hover.background} !important`,
            color: (props: ThemeProps) => `${props.theme.button.secondary.color} !important`,
            borderBottomStyle: "solid",
        },
    },

    tabPanel: {
        flex: 1,
        width: "100%",
        borderLeft: (props: ThemeProps) => `1px solid ${props.theme.panelSeparatorColor}`,
        borderRight: (props: ThemeProps) => `1px solid ${props.theme.panelSeparatorColor}`,
    },
});
