import React, {useState} from "react";
import clsx from "clsx";
import { tabsStyles } from "./tabs-styles";

export interface StudioTabItem {
    key: string;
    name: string;
    content: React.FC;
}

export interface StudioTabsClasses {
    root?: string;
    tabGroup?: string;
    tabItem?: string;
    first?: string;
    last?: string;
    selected?: string;
    tabContent?: string;
}

export interface StudioTabsProps {
    classes?: StudioTabsClasses;
    items: StudioTabItem[];
}

export const StudioTabs: React.FC<StudioTabsProps> = ({classes, items}) => {
    const ownClasses = tabsStyles();
    const [selectedItem, setSelectedItem] = useState(items[0]);

    return (
        <div className={classes.root}>
            <div className={clsx(ownClasses.tabGroup, classes.tabGroup)}>
                {items.map((item, idx) => <StudioTab classes={classes} item={item} setSelectedItem={setSelectedItem}
                                                      selected={item.key === selectedItem.key}
                                                      first={idx === 0} last={idx === items.length - 1}/>)}
            </div>
            {items.map((item) =>
                <div hidden={item.key !== selectedItem.key} className={classes.tabContent}>
                    <item.content/>
                </div>)}
        </div>
    );
}

interface StudioTabProps {
    item: StudioTabItem;
    selected: boolean;
    first: boolean;
    last: boolean;
    classes?: StudioTabsClasses;
    setSelectedItem: (value: StudioTabItem) => void;
}

const StudioTab: React.FC<StudioTabProps> = ({item, selected, first, last, classes, setSelectedItem}) => {
    const ownClasses = tabsStyles();

    const selectTab = () => {
        setSelectedItem(item);
    }
    return (
        <a className={clsx(ownClasses.tabItem, classes.tabItem, first && classes.first,
            last && classes.last, selected && classes.selected)} onClick={selectTab}>
            {item.name}
        </a>
    );
}
