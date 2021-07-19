import { StudioButtonProps } from "./button";
import makeStyles from "@material-ui/core/styles/makeStyles";
import { StudioTheme, vaticleTheme } from "../../styles/theme";

type ButtonStyleProps = {theme: StudioTheme} & Pick<StudioButtonProps, 'size' | 'type'>;

const commonButtonStyles = {
    disable: {
        border: (props: ButtonStyleProps) => `${props.theme.button[props.type].disabled.border} !important`,
        background: (props: ButtonStyleProps) => `${props.theme.button[props.type].disabled.background} !important`,
        color: (props: ButtonStyleProps) => `${props.theme.button[props.type].disabled.color} !important`,
        pointerEvents: "none",
    },

    root: {
        border: (props: ButtonStyleProps) => props.theme.button[props.type].border,
        background: (props: ButtonStyleProps) => props.theme.button[props.type].background,
        transition: "background-color 250ms cubic-bezier(0.4, 0, 0.2, 1) 0ms,border 250ms cubic-bezier(0.4, 0, 0.2, 1) 0ms;",
        display: "inline-block",
        color: (props: ButtonStyleProps) => props.theme.button[props.type].color,
        fontFamily: "Titillium Web",
        lineHeight: "22px",
        position: "relative",

        "&:hover:not($disable)": {
            color: (props: ButtonStyleProps) => props.theme.button[props.type].hover.color,
            background: (props: ButtonStyleProps) => props.theme.button[props.type].hover.background,
            border: (props: ButtonStyleProps) => props.theme.button[props.type].hover.border,
        },
    },
} as const;

export const buttonStyles = makeStyles({
    disable: commonButtonStyles.disable,

    root: {
        ...commonButtonStyles.root,
        height: 36,
        borderRadius: 4,
        fontSize: (props: ButtonStyleProps) => vaticleTheme.typography.fontSize[props.size],
        fontWeight: 600,
        padding: "4px 12px",
    },
});

export const iconButtonStyles = makeStyles({
    disable: commonButtonStyles.disable,

    root: {
        ...commonButtonStyles.root,
        height: 42,
        width: 42,
        marginTop: 1,
        borderRadius: 5,
        fontSize: (props: ButtonStyleProps) => vaticleTheme.typography.fontSize[props.size] * 1.5,
        fontWeight: 400,
        padding: "4px 12px 8px",
    },
});
