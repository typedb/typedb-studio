import makeStyles from "@material-ui/core/styles/makeStyles";
import { StudioTheme } from "../styles/theme";

type ThemeProps = { theme: StudioTheme };

export const loginStyles = makeStyles({
    form: {
        background: (props: ThemeProps) => props.theme.windowBackground,
        color: (props: ThemeProps) => props.theme.textColor,
        display: "flex",
        flexDirection: "column",

        "@media(min-width: 1200px)": {
            width: 880,
        },

        "@media(max-width: 767px)": {
            alignItems: "center",
        },
    },

    formRow: {
        display: "flex",
        justifyContent: "space-around",
        alignItems: "flex-start",

        "&:not(:first-child)": {
            marginTop: 30,
        },

        "& > *": {
            width: 375,

            "@media(min-width: 768px)": {
                "&:not(:first-child)": {
                    marginLeft: 30,
                },
            },

            "@media (min-width: 768px) and (max-width: 1199px)": {
                maxWidth: "40vw",
            },

            "@media(max-width: 767px)": {
                maxWidth: "calc(100vw - 100px)",
            },
        },

        "@media(max-width: 767px)": {
            flexDirection: "column",

            "& > *:not(:first-child)": {
                marginTop: 30,
            },
        },
    },

    formCell: {
        display: "flex",
        flexDirection: "column",

        "& > * + *": {
            marginTop: 30,
        },
    },

    formControlLabel: {
        marginRight: "0 !important",
        textAlign: "start",
    },

    actionList: {
        display: "grid",
        justifyContent: "center",
        gridTemplateColumns: "repeat(auto-fit, 160px)",
        gap: "24px 32px",
    },

    tabs: {
        flex: 1,
        display: "flex",
        flexDirection: "column",
        border: (props: ThemeProps) => `1px solid ${props.theme.tabs.outerBorderColor}`,
    },

    tabGroup: {
        height: 32,
    },

    tab: {
        background: (props: ThemeProps) => props.theme.tabs.background,
        color: (props: ThemeProps) => props.theme.tabs.color,

        "&:not(:first-child)": {
            borderRight: (props: ThemeProps) => `1px solid ${props.theme.tabs.separatorColor}`,
        },

        "&:hover": {
            background: (props: ThemeProps) => props.theme.tabs.hover.background,
        },
    },

    tabSelected: {
        background: (props: ThemeProps) => `${props.theme.tabs.selected.background} !important`,
        color: (props: ThemeProps) => `${props.theme.tabs.selected.color} !important`,
    },

    tabContent: {
        flex: 1,
        display: "flex",
        flexDirection: "column",
    },
});
