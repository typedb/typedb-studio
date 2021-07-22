import { StudioButtonProps } from "./button";
import makeStyles from "@material-ui/core/styles/makeStyles";
import { SizeIndicator, ThemeProps, vaticleTheme } from "../../styles/theme";
import { StudioIconButtonProps } from "./icon-button";

type ButtonStyleProps = ThemeProps & Pick<StudioButtonProps, 'size' | 'type'>;

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

type IconButtonStyleProps = ThemeProps & Pick<StudioIconButtonProps, "size">;

// TODO: merge these constants into one
const iconButtonFontSizes: {[key in SizeIndicator]?: number} = {
    medium: 21,
    small: 18,
}

const iconButtonSizes: {[key in SizeIndicator]?: number} = {
    medium: 42,
    small: 28,
}

const iconButtonBorderRadii: {[key in SizeIndicator]?: number} = {
    small: 4,
    medium: 5,
}

const iconButtonPaddings: {[key in SizeIndicator]?: string} = {
    small: "0 8px 2px",
    medium: "4px 12px 8px",
}

export const iconButtonStyles = makeStyles({
    disable: commonButtonStyles.disable,

    root: {
        ...commonButtonStyles.root,
        height: (props: IconButtonStyleProps) => iconButtonSizes[props.size],
        width: (props: IconButtonStyleProps) => iconButtonSizes[props.size],
        borderRadius: (props: IconButtonStyleProps) => iconButtonBorderRadii[props.size],
        fontSize: (props: IconButtonStyleProps) => iconButtonFontSizes[props.size],
        fontWeight: 400,
        padding: (props: IconButtonStyleProps) => iconButtonPaddings[props.size],
    },
});
