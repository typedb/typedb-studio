import makeStyles from "@material-ui/core/styles/makeStyles";
import { contentMargin } from "../styles/studio-styles";
import { ThemeProps } from "../styles/theme";

export const loginStyles = makeStyles({
    backdrop: {
        height: "100%",
        width: "100%",
        background: (props: ThemeProps) => props.theme.windowBackdrop,
    },

    form: {
        background: (props: ThemeProps) => props.theme.windowBackground,
        color: (props: ThemeProps) => props.theme.textColor,
        display: "flex",
        flexDirection: "column",
        width: "100%",
        padding: "32px 24px",
    },

    formRow: {
        display: "flex",
        justifyContent: "space-around",
        alignItems: "flex-start",
        position: "relative",

        "&:not(:first-child)": {
            marginTop: contentMargin,
        },

        "& > *": {
            width: 270,

            "@media(min-width: 768px)": {
                "&:not(:first-child)": {
                    marginLeft: 30,
                },
            },
        },
    },

    formCell: {
        display: "flex",
        flexDirection: "column",

        "& > * + *": {
            marginTop: contentMargin,
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
        marginTop: contentMargin,
        gap: `${contentMargin} 32px`,
    },

    loginTabs: {
        width: 500,
        marginLeft: "auto",
        marginRight: "auto",
        paddingTop: "25vh",
        alignSelf: "center",
    },

    tabs: {
        display: "flex",
        flexDirection: "column",
    },

    tabGroup: {
        height: 32,
        border: (props: ThemeProps) => `1px solid ${props.theme.tabs.outerBorderColor}`,
    },

    tab: {
        background: (props: ThemeProps) => props.theme.tabs.background,
        color: (props: ThemeProps) => props.theme.tabs.color,

        "&:not(:last-child)": {
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
        width: "100%",
        borderLeft: (props: ThemeProps) => `1px solid ${props.theme.tabs.outerBorderColor}`,
        borderRight: (props: ThemeProps) => `1px solid ${props.theme.tabs.outerBorderColor}`,
    },

    buttonBesideTextField: {
        position: "absolute",
        width: 42,
        marginLeft: 360,
    },
});
