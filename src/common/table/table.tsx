import clsx from "clsx";
import React from 'react';
import { themeState } from "../../state/state";
import { ClassProps } from "../class-props";
import { tableStyles } from "./table-styles";

export interface StudioTableProps extends ClassProps {
    headings: string[];
    minCellWidth: number;
}

const createHeaders = (headers: string[]) => {
    return headers.map((item) => ({
        text: item,
        ref: React.useRef<HTMLTableHeaderCellElement>(),
    }));
}

export const StudioTable: React.FC<StudioTableProps> = ({ headings, minCellWidth, children, className }) => {
    const theme = themeState.use()[0];
    const classes = tableStyles({ theme });
    const [tableHeight, setTableHeight] = React.useState('auto');
    const [activeIndex, setActiveIndex] = React.useState(null);
    const tableRef = React.useRef(null);
    const columns = createHeaders(headings);
    const wrapperRef = React.useRef<HTMLDivElement>(null);

    React.useEffect(() => {
        setTableHeight(tableRef.current.offsetHeight);
    }, []);

    const mouseDown = (index: number) => {
        setActiveIndex(index);
    }

    const mouseMove = React.useCallback((e) => {
        const maxCellWidth = tableRef.current.offsetWidth - minCellWidth;
        const requestedWidth0 = e.clientX - columns[0].ref.current.getBoundingClientRect().x;
        const width0 = Math.min(maxCellWidth, Math.max(minCellWidth, requestedWidth0));
        const width1 = tableRef.current.offsetWidth - width0;

        // Assign the px values to the table
        tableRef.current.style.gridTemplateColumns = `${width0}px ${width1}px`;
    }, [activeIndex, columns, minCellWidth]);

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
            <table className={clsx(classes.table, className)} ref={tableRef}>
                <thead>
                    <tr>
                        {columns.map(({ ref, text }, i) => (
                            <th ref={ref} key={text}>
                                <span>{text}</span>
                                {i !== columns.length - 1 && (
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
