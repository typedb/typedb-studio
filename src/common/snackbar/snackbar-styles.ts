import makeStyles from "@material-ui/core/styles/makeStyles";
import { vaticleTheme } from "../../styles/theme";

export const snackbarStyles = makeStyles({
    root: {
        "& > *": {
            color: vaticleTheme.palette.purple["3"],
            fontWeight: 400,
        },

        "& .MuiSnackbarContent-action": {
            marginLeft: 0,
            marginRight: 0,
            paddingLeft: 0,
        },
    },

    topRight: {
        marginTop: 80,
    },

    success: {
        "& > *": {
            backgroundColor: vaticleTheme.palette.green["1"],
        },
    },

    error: {
        "& > *": {
            backgroundColor: vaticleTheme.palette.red["1"],
        },
    },

    status: {
        fontWeight: 600,
    },

    close: {
        marginLeft: "1em",
        marginRight: -8,
    },
});
