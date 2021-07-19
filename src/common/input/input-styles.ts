import makeStyles from "@material-ui/core/styles/makeStyles";
import { ThemeProps, vaticleTheme } from "../../styles/theme";

export const textFieldStyles = makeStyles({
    invalid: {},

    textField: {
        borderRadius: 5,
        background: (props: ThemeProps) => props.theme.textField.background,
        color: (props: ThemeProps) => props.theme.textColor,

        "& input, & textarea": {
            color: (props: ThemeProps) => props.theme.textColor,
            lineHeight: "24px",
        },

        "& input": {
            padding: "12px 6px !important",
            height: 20,
        },

        "& input + fieldset, & textarea + fieldset": {
            borderColor: (props: ThemeProps) => props.theme.textField.borderColor,
            transition: "border-color 100ms ease",
        },

        "& input:hover + fieldset": {
            borderColor: (props: ThemeProps) => `${props.theme.textField.hover.borderColor} !important`,
        },

        "& input:hover:focus + fieldset": {
            borderColor: (props: ThemeProps) => `${props.theme.textField.focus.borderColor} !important`,
        },

        "& label.Mui-focused": {
            color: (props: ThemeProps) => `${props.theme.textField.focus.borderColor} !important`,
        },

        "&$invalid label, &$invalid label.Mui-focused": {
            color: vaticleTheme.palette.red["1"],
        },
    },

    inputRoot: {
        paddingTop: "0 !important",
        paddingBottom: "0 !important",

        "&$invalid fieldset, &$invalid:hover fieldset": {
            borderColor: vaticleTheme.palette.red["1"],
        },
    },

    inputMultiline: {
        padding: "12px 14px",

        "&:hover fieldset": {
            borderColor: (props: ThemeProps) => `${props.theme.textField.hover.borderColor} !important`,
        },

        "&:hover:focus fieldset": {
            borderColor: (props: ThemeProps) => `${props.theme.textField.focus.borderColor} !important`,
        },
    },

    inputFocused: {
        "& fieldset, &:hover fieldset": {
            borderColor: (props: ThemeProps) => `${props.theme.textField.focus.borderColor} !important`,
            borderWidth: "1px !important",
        },

        "&$invalid fieldset, &$invalid:hover fieldset": {
            borderColor: vaticleTheme.palette.red["1"],
        },
    },

    inputLabelOutlined: {
        color: (props: ThemeProps) => props.theme.textColor,
        transform: "translate(14px, 14px) scale(1)",
    },

    inputLabelShrink: {
        transform: "translate(14px, -7px) scale(0.75) !important",
    },
});
