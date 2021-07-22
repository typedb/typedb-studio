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
        outline: (props: ThemeProps) => `1px solid ${props.theme.tabs.outerBorderColor}`,
    },

    tab: {
        flex: 1,
        display: "flex",
        flexDirection: "column",
        justifyContent: "center",
        alignItems: "center",
        transition: "background-color 150ms ease",
        userSelect: "none",
        padding: "0 10px",
        minHeight: "unset",
        minWidth: "unset",
        overflow: "visible",
        opacity: 1,
        textTransform: "none",
        position: "relative",
        background: (props: ThemeProps) => props.theme.tabs.background,
        color: (props: ThemeProps) => props.theme.tabs.color,

        "&:not(:last-child)": {
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
        },

        "&:hover": {
            background: (props: ThemeProps) => props.theme.tabs.hover.background,
        },
    },

    tabSelected: {
        background: (props: ThemeProps) => `${props.theme.tabs.selected.background} !important`,
        color: (props: ThemeProps) => `${props.theme.tabs.selected.color} !important`,
    },

    indicator: {
        background: (props: ThemeProps) => props.theme.tabs.selected.indicatorColor,
    },

    tabPanel: {
        flex: 1,
        width: "100%",
        borderLeft: (props: ThemeProps) => `1px solid ${props.theme.tabs.outerBorderColor}`,
        borderRight: (props: ThemeProps) => `1px solid ${props.theme.tabs.outerBorderColor}`,
    },
});
