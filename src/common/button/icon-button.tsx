import clsx from "clsx";
import React from "react";
import { themeState } from "../../state/state";
import { SizeIndicator } from "../../styles/theme";
import { iconButtonStyles } from "./button-styles";

export interface StudioIconButtonClasses {
    root?: string;
}

export interface StudioIconButtonProps {
    size: SizeIndicator;
    disabled?: boolean;
    onClick: React.MouseEventHandler;
    classes?: StudioIconButtonClasses;
}

export const StudioIconButton: React.FC<StudioIconButtonProps> = ({ children, classes, size, onClick, disabled }) => {
    const ownClasses = iconButtonStyles({ size, type: "secondary", theme: themeState.use()[0] });

    return (
        <button className={clsx(ownClasses.root, classes?.root, disabled && ownClasses.disable)} onClick={onClick}>
            {children}
        </button>
    );
};
