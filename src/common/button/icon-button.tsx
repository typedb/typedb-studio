import { SizeIndicator } from "../../styles/theme";
import React from "react";
import { ClassProps } from "../class-props";
import { iconButtonStyles } from "./button-styles";
import clsx from "clsx";
import { themeState } from "../../state/state";

export interface StudioIconButtonProps extends ClassProps {
    size: SizeIndicator;
    disabled?: boolean;
    onClick: React.MouseEventHandler;
}

export const StudioIconButton: React.FC<StudioIconButtonProps> = ({ children, className, size, onClick, disabled }) => {
    const classes = iconButtonStyles({ size, type: "secondary", theme: themeState.use()[0] });

    return (
        <button className={clsx(classes.root, disabled && classes.disable, className)} onClick={onClick}>
            {children}
        </button>
    );
};
