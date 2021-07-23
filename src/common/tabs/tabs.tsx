import IconButton from "@material-ui/core/IconButton";
import Tab from "@material-ui/core/Tab";
import Tabs from "@material-ui/core/Tabs";
import CloseIcon from "@material-ui/icons/Close";
import React from "react";
import clsx from "clsx";
import { themeState } from "../../state/state";
import { StudioIconButton } from "../button/icon-button";
import { StudioTabsOrientation, tabsStyles } from "./tabs-styles";

export interface StudioTabItem {
    key: string;
    label: React.ReactNode;
    icon?: React.ReactNode;
}

export interface StudioTabsClasses {
    root?: string;
    tabGroup?: string;
    tab?: string;
    first?: string;
    last?: string;
    tabSelected?: string;
    tabPanel?: string;
}

export interface StudioTabsProps {
    selectedIndex?: number,
    setSelectedIndex?: (value: number) => void;
    classes?: StudioTabsClasses;
    items: StudioTabItem[];
    showCloseButton?: boolean;
    showAddButton?: boolean;
    orientation?: StudioTabsOrientation;
}

export const StudioTabs: React.FC<StudioTabsProps> = ({classes, items, selectedIndex, setSelectedIndex, showCloseButton, showAddButton, orientation, children}) => {
    if (!orientation) orientation = "normal";
    const ownClasses = tabsStyles({ theme: themeState.use()[0], orientation });

    const handleChange = (_event: React.ChangeEvent<{}>, newIndex: number) => {
        if (setSelectedIndex) setSelectedIndex(newIndex);
    };

    const muiSelectedIndex = selectedIndex != null ? selectedIndex : false;

    return (
        <div className={clsx(ownClasses.root, classes?.root)}>
            <Tabs value={muiSelectedIndex} onChange={handleChange}
                  classes={{root: clsx(ownClasses.tabGroup, classes?.tabGroup), flexContainer: ownClasses.flexContainer, indicator: ownClasses.indicator}}>
                {items.map((item) => <StudioTab ownClasses={ownClasses} item={item} classes={classes} showCloseButton={showCloseButton} />)}
                {showAddButton && <StudioIconButton size="smaller" onClick={(e) => e.preventDefault()}
                                                    classes={{root: ownClasses.addButton}}>+</StudioIconButton>}
            </Tabs>
            {children}
        </div>
    );
}

interface StudioTabProps {
    item: StudioTabItem;
    selected?: boolean;
    first?: boolean;
    last?: boolean;
    ownClasses: any;
    classes?: StudioTabsClasses;
    showCloseButton?: boolean;
    onClose?: () => any;
}

export const StudioTab: React.FC<StudioTabProps> = ({item, selected, first, last, ownClasses, classes, showCloseButton, onClose, ...props}) => {
    const labelElement = (
        <div className={ownClasses.tabContent}>
            <div className={ownClasses.tabIcon}>{item.icon}</div>
            <div className={ownClasses.tabLabel}>{item.label}</div>
            {showCloseButton && <IconButton aria-label="close" color="inherit" onClick={onClose} className={ownClasses.close}>
                <CloseIcon className={ownClasses.closeIcon}/>
            </IconButton>}
        </div>
    );

    return (
        <Tab {...props} label={labelElement} classes={{root: clsx(ownClasses.tab, classes?.tab)}} className={clsx(first && classes?.first,
            last && classes?.last, selected && ownClasses.tabSelected, selected && classes?.tabSelected, showCloseButton && ownClasses.withCloseButton)}/>
    );
}

export interface StudioTabPanelProps {
    children?: React.ReactNode;
    index: number;
    selectedIndex: number;
    className?: string;
}

export const StudioTabPanel: React.FC<StudioTabPanelProps> = ({children, selectedIndex, index, className}) => {
    return (
        <div role="tabpanel" hidden={selectedIndex !== index} className={className}>
            {children}
        </div>
    );
}
