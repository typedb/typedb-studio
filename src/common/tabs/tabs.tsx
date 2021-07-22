import Tab from "@material-ui/core/Tab";
import Tabs from "@material-ui/core/Tabs";
import React from "react";
import clsx from "clsx";
import { themeState } from "../../state/state";
import { tabsStyles } from "./tabs-styles";

export interface StudioTabItem {
    key: string;
    name: string;
    content: React.FC;
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
    selectedIndex: number,
    setSelectedIndex: (value: number) => void;
    classes?: StudioTabsClasses;
    items: StudioTabItem[];
}

export const StudioTabs: React.FC<StudioTabsProps> = ({classes, items, selectedIndex, setSelectedIndex, children}) => {
    const ownClasses = tabsStyles({ theme: themeState.use()[0] });
    // const [selectedItem, setSelectedItem] = useState(items[0]);
    // const [selectedIndex, setSelectedIndex] = React.useState(0);

    const handleChange = (_event: React.ChangeEvent<{}>, newIndex: number) => {
        setSelectedIndex(newIndex);
    };

    return (
        <div className={clsx(ownClasses.root, classes.root)}>
            <Tabs value={selectedIndex} onChange={handleChange} classes={{root: clsx(ownClasses.tabGroup, classes.tabGroup), indicator: ownClasses.indicator}}>
                {items.map((item) => <StudioTab ownClasses={ownClasses} label={item.name} classes={classes}/>)}
            </Tabs>
            {children}
            {/*{items.map((item, idx) =>*/}
            {/*<TabPanel selectedIndex={selectedIndex} index={idx} className={clsx(ownClasses.tabPanel, classes.tabPanel)}>*/}
            {/*    <item.content/>*/}
            {/*</TabPanel>)}*/}
        </div>
    );
}

interface StudioTabProps {
    label: string;
    selected?: boolean;
    first?: boolean;
    last?: boolean;
    ownClasses: any;
    classes?: StudioTabsClasses;
    setSelectedItem?: (value: StudioTabItem) => void;
}

export const StudioTab: React.FC<StudioTabProps> = ({label, selected, first, last, ownClasses, classes, ...props}) => {
    // const selectTab = () => {
    //     setSelectedItem(item);
    // }

    return (
        <Tab {...props} label={label} classes={{root: clsx(ownClasses.tab, classes.tab)}} className={clsx(first && classes.first,
            last && classes.last, selected && ownClasses.tabSelected, selected && classes.tabSelected)}/>
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
