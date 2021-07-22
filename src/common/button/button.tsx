import { SizeIndicator } from "../../styles/theme";
import React from "react";
import { ClassProps } from "../class-props";
import { buttonStyles } from "./button-styles";
import clsx from "clsx";
import { themeState } from "../../state/state";

export interface StudioButtonProps extends ClassProps {
    type: "primary" | "secondary";
    size: SizeIndicator;
    disabled?: boolean;
    onClick?: React.MouseEventHandler;
}

export const StudioButton: React.FC<StudioButtonProps> = ({ children, className, size, type, onClick, disabled }) => {
    const classes = buttonStyles({ size, type, theme: themeState.use()[0] });

    return (
        <button className={clsx(classes.root, disabled && classes.disable, className)} onClick={onClick}>
            {children}
        </button>
    );
};
