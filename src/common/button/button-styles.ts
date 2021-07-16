import { StudioButtonProps } from "./button";
import makeStyles from "@material-ui/core/styles/makeStyles";
import { StudioTheme, vaticleTheme } from "../../styles/theme";

type StyleProps = {theme: StudioTheme} & Pick<StudioButtonProps, 'size' | 'type'>;

export const buttonStyles = makeStyles({
    disable: {
        border: (props: StyleProps) => `${props.theme.button[props.type].disabled.border} !important`,
        background: (props: StyleProps) => `${props.theme.button[props.type].disabled.background} !important`,
        color: (props: StyleProps) => `${props.theme.button[props.type].disabled.color} !important`,
        pointerEvents: "none",
    },

    root: {
        height: 36,
        border: (props: StyleProps) => props.theme.button[props.type].border,
        borderRadius: 4,
        background: (props: StyleProps) => props.theme.button[props.type].background,
        transition: "background-color 250ms cubic-bezier(0.4, 0, 0.2, 1) 0ms,border 250ms cubic-bezier(0.4, 0, 0.2, 1) 0ms;",
        display: "inline-block",
        color: (props: StyleProps) => props.theme.button[props.type].color,
        fontFamily: "Titillium Web",
        fontSize: (props: StyleProps) => vaticleTheme.typography.fontSize[props.size],
        fontWeight: 600,
        lineHeight: "22px",
        padding: "4px 12px",
        position: "relative",

        "&:hover:not($disable)": {
            color: (props: StyleProps) => props.theme.button[props.type].hover.color,
            background: (props: StyleProps) => props.theme.button[props.type].hover.background,
            border: (props: StyleProps) => props.theme.button[props.type].hover.border,
        },
    },
});
