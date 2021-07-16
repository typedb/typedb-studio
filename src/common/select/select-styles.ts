import makeStyles from "@material-ui/core/styles/makeStyles";
import { ThemeProps } from "../../styles/theme";

export const selectStyles = makeStyles({
    select: {
        background: (props: ThemeProps) => props.theme.textField.background,
        lineHeight: "20px", // Prevent clipping of letters such as "g"
        borderRadius: 5,
    },

    paper: {
        background: (props: ThemeProps) => props.theme.selectOption.background,
        color: (props: ThemeProps) => props.theme.textColor,

        "& option": {
            padding: "10px 14px",
        },

        "& option[disabled]": {
            userSelect: "none",
        },

        "& option:not([disabled])": {
            cursor: "pointer",

            "&:hover": {
                background: (props: ThemeProps) => props.theme.selectOption.hover.background,
            },
        },
    },
});
