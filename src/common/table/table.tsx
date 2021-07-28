import clsx from "clsx";
import React from 'react';
import { themeState } from "../../state/state";
import { ClassProps } from "../class-props";
import { tableStyles } from "./table-styles";
import CSS from "csstype";

export type StudioTableSizing = "resizable" | "fixed";

export interface StudioTableProps extends ClassProps {
    headings: string[];
    minCellWidth: number;
    sizing: StudioTableSizing;
    initialGridTemplateColumns: CSS.Property.GridTemplateColumns;
}

export const StudioTable: React.FC<StudioTableProps> = ({ headings, minCellWidth, sizing, initialGridTemplateColumns, children, className }) => {
    const theme = themeState.use()[0];
    const classes = tableStyles({ theme });
    const [tableHeight, setTableHeight] = React.useState('auto');
    const [activeIndex, setActiveIndex] = React.useState(null);
    const tableRef = React.useRef(null);
    const headerRowRef = React.useRef<HTMLTableRowElement>(null);
    const wrapperRef = React.useRef<HTMLDivElement>(null);

    React.useEffect(() => {
        setTableHeight(tableRef.current.offsetHeight);
    }, []);

    const mouseDown = (index: number) => {
        setActiveIndex(index);
    }

    const mouseMove = React.useCallback((e) => {
        if (sizing === "resizable") {
            const gridColumns = headings.map((_heading, idx) => {
                const columnElement = headerRowRef.current.children[idx] as HTMLElement;
                if (idx === activeIndex) {
                    const requestedWidth = e.clientX - columnElement.getBoundingClientRect().x;
                    const width = Math.max(minCellWidth, requestedWidth);
                    return `${width}px`;
                } else return `${columnElement.offsetWidth}px`;
            });
            tableRef.current.style.gridTemplateColumns = gridColumns.join(" ");
        } else { // if (sizing === "fixed")
            // TODO: Support having more than 2 columns in fixed sizing mode
            const maxCellWidth = tableRef.current.offsetWidth - minCellWidth;
            const requestedWidth0 = e.clientX - headerRowRef.current.children[0].getBoundingClientRect().x;
            const width0 = Math.min(maxCellWidth, Math.max(minCellWidth, requestedWidth0));
            const width1 = tableRef.current.offsetWidth - width0;
            tableRef.current.style.gridTemplateColumns = `${width0}px ${width1}px`;
        }
    }, [activeIndex, headings, minCellWidth, sizing]);

    const removeListeners = React.useCallback(() => {
        if (wrapperRef.current) {
            wrapperRef.current.removeEventListener('mousemove', mouseMove);
            wrapperRef.current.removeEventListener('mouseup', removeListeners);
        }
    }, [mouseMove]);

    const mouseUp = React.useCallback(() => {
        setActiveIndex(null);
        removeListeners();
    }, [setActiveIndex, removeListeners]);

    React.useEffect(() => {
        if (activeIndex !== null) {
            wrapperRef.current.addEventListener('mousemove', mouseMove);
            wrapperRef.current.addEventListener('mouseup', mouseUp);
        }

        return () => {
            removeListeners();
        }
    }, [activeIndex, mouseMove, mouseUp, removeListeners]);

    return (
        <div ref={wrapperRef} className={classes.wrapper}>
            <table className={clsx(classes.table, className)} ref={tableRef} style={{
                userSelect: activeIndex != null ? "none" : undefined,
                cursor: activeIndex != null ? "col-resize" : undefined,
                gridTemplateColumns: initialGridTemplateColumns,
            }}>
                <thead>
                    <tr ref={headerRowRef}>
                    {headings.map((heading, i) => (
                        <th key={heading}>
                            <span>{heading}</span>
                            {(sizing === "resizable" || i !== headings.length - 1) && (
                                <div style={{ height: tableHeight }} onMouseDown={() => mouseDown(i)}
                                className={clsx(classes.resizeHandle, activeIndex === i && classes.active)}/>)}
                        </th>
                    ))}
                    </tr>
                </thead>
                <tbody>
                    {children}
                </tbody>
            </table>
        </div>
    );
}
