import makeStyles from "@material-ui/core/styles/makeStyles";
import { ThemeProps } from "../../styles/theme";

export const tableStyles = makeStyles({
    wrapper: {
        overflow: "hidden", /* Clips any scrollbars that appear */
    },

    table: {
        width: "100%",
        overflow: "auto", /* Allow scrolling within the table */
        display: "grid",
        userSelect: "none", // NOTE: This is because of weird behaviour when the cursor is over the resizer

        "& thead, & tbody, & tr": {
            display: "contents",
        },

        "& th": {
            position: "relative",
        },

        "& th, & td": {
            padding: "0 8px",

            "& span": {
                whiteSpace: "nowrap",
                textOverflow: "ellipsis",
                overflow: "hidden",
                display: "block",
            },
        },

        "& thead th": {
            background: (props: ThemeProps) => props.theme.table.oddRowBackground,
            color: (props: ThemeProps) => props.theme.table.headerColor,
        },

        "& tbody tr": {
            color: (props: ThemeProps) => props.theme.table.color,

            "&:nth-child(odd) td": {
                background: (props: ThemeProps) => props.theme.table.evenRowBackground, // yes, including the header row, this is an 'even row'
            },

            "&:nth-child(even) td": {
                background: (props: ThemeProps) => props.theme.table.oddRowBackground,
            },
        },
    },

    resizeHandle: {
        display: "block",
        position: "absolute",
        cursor: "col-resize",
        width: 7,
        right: 0,
        top: 0,
        zIndex: 1,
        borderRight: "2px solid transparent",

        "&:hover": {
            borderColor: "#CCC",
        },

        "&$active": {
            borderColor: "#517EA5",
        }
    },

    active: {},
});
