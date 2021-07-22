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
        background: (props: ThemeProps) => props.theme.background,
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

            "&:not(:first-child)": {
                marginLeft: 30,
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

    tabs: {
        width: 500,
        marginLeft: "auto",
        marginRight: "auto",
        paddingTop: "25vh",
        alignSelf: "center",
    },

    tabGroup: {
        height: 34,
    },

    tab: {
        height: 34,
    },

    buttonBesideTextField: {
        position: "absolute",
        width: 42,
        marginTop: 1,
        marginLeft: "360px !important",
        padding: "4px 12px 8px",
    },
});
