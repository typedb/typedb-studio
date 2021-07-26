import makeStyles from "@material-ui/core/styles/makeStyles";
import { ThemeProps } from "../../styles/theme";

export type StudioTabsOrientation = "normal" | "bottomToTop" | "topToBottom";

export interface TabsStyleProps extends ThemeProps {
    orientation: StudioTabsOrientation;
}

const tabPaddings: {[orientation in StudioTabsOrientation]: string} = {
    normal: "0 8px 2px",
    bottomToTop: "10px 2px 6px 0",
    topToBottom: "10px 0 6px 2px",
};

export const tabsStyles = makeStyles({
    root: {
        display: "flex",
        flexDirection: "column",
    },

    tabGroup: {
        minHeight: "0 !important",
        display: "flex",
    },

    tab: {
        flex: 1,
        display: "flex",
        flexDirection: "column",
        justifyContent: "center",
        alignItems: "center",
        transition: "background-color 150ms ease",
        userSelect: "none",
        padding: (props: TabsStyleProps) => tabPaddings[props.orientation],
        minHeight: "unset",
        minWidth: "unset",
        maxWidth: "unset",
        overflow: "visible",
        opacity: 1,
        textTransform: "none",
        position: "relative",
        background: (props: TabsStyleProps) => props.theme.tabs.background,
        color: (props: TabsStyleProps) => props.theme.tabs.color,

        // This structure is used to avoid modifying the width of the tab
        "&:after": {
            content: "''",
            display: "block",
            position: "absolute",
            top: 0,
            right: -1,
            bottom: 0,
            left: 0,
            borderRight: (props: TabsStyleProps) => `1px solid ${props.theme.tabs.separatorColor}`,
            pointerEvents: "none",
        },

        "&:hover": {
            background: (props: TabsStyleProps) => props.theme.tabs.hover.background,
        },

        "&$withCloseButton": {
            paddingRight: 4,
        },
    },

    withCloseButton: {},

    tabContent: {
        display: "flex",
        alignItems: "center",
        flexDirection: (props: TabsStyleProps) => props.orientation === "normal" ? "row" : "column-reverse",

        "& > * + *": {
            marginLeft: (props: TabsStyleProps) => props.orientation === "normal" ? 2 : undefined,
        },

        "& > :not(:last-child)": {
            marginTop: (props: TabsStyleProps) => props.orientation !== "normal" ? 6 : undefined,
        },
    },

    tabLabel: {
        writingMode: (props: TabsStyleProps) => props.orientation === "normal" ? "horizontal-tb" : "vertical-rl",
        transform: (props: TabsStyleProps) => props.orientation === "bottomToTop" ? "rotate(180deg)" : undefined,
    },

    tabSelected: {
        background: (props: TabsStyleProps) => `${props.theme.tabs.selected.background} !important`,
        color: (props: TabsStyleProps) => `${props.theme.tabs.selected.color} !important`,
    },

    flexContainer: {
        flexDirection: (props: TabsStyleProps) => props.orientation === "normal" ? "row" : "column",
    },

    indicator: {
        background: (props: TabsStyleProps) => props.theme.tabs.selected.indicatorColor,
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
        background: (props: TabsStyleProps) => props.theme.tabs.background,
        color: (props: TabsStyleProps) => `${props.theme.tabs.color}`,
        borderColor: (props: TabsStyleProps) => props.theme.tabs.separatorColor,
        borderRadius: 2,
        borderBottomColor: (_props: TabsStyleProps) => "transparent",
        height: 29,
        width: 29,
        padding: "0 8px 4px",

        "&:hover": {
            background: (props: TabsStyleProps) => `${props.theme.tabs.hover.background} !important`,
            color: (props: TabsStyleProps) => `${props.theme.button.secondary.color} !important`,
            borderBottomStyle: "solid",
        },
    },

    tabPanel: {
        flex: 1,
        width: "100%",
        borderLeft: (props: TabsStyleProps) => `1px solid ${props.theme.panelSeparatorColor}`,
        borderRight: (props: TabsStyleProps) => `1px solid ${props.theme.panelSeparatorColor}`,
    },
});
