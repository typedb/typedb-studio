import InputBase from "@material-ui/core/InputBase";
import { ClassProps } from "../class-props";
import React from "react";
import FormControl from "@material-ui/core/FormControl";
import Select from "@material-ui/core/Select";
import ExpandMoreIcon from "@material-ui/icons/ExpandMore";
import withStyles from "@material-ui/core/styles/withStyles";
import { Theme } from "@material-ui/core/styles/createTheme";
import createStyles from "@material-ui/core/styles/createStyles";
import { selectStyles } from "./select-styles";
import { StudioTheme } from "../../styles/theme";
import { themeState } from "../../state/typedb-client";

interface StudioSelectProps<TItem extends string | number> extends ClassProps {
    label: string;
    value: TItem;
    setValue: (value: TItem) => void;
    variant: "outlined" | "filled";
}

export function StudioSelect<TItem extends string | number>({children, className, label, value, setValue, variant}: React.PropsWithChildren<StudioSelectProps<TItem>>): JSX.Element {
    const classes = selectStyles();

    const inputElement = React.createElement(studioSelectInput(variant, themeState.use()[0]));

    return (
        <FormControl variant="outlined" className={className}>
            <Select native label={label} value={value} onChange={(e) => setValue(e.target.value as TItem)}
                    className={classes.select} input={inputElement}
                    IconComponent={() => <ExpandMoreIcon style={{fontSize: 14, fill: "#FFF", position: "absolute", right: 8, pointerEvents: "none"}}/>}>
                {children}
            </Select>
        </FormControl>
    );
}

const studioSelectInput: (variant: "outlined" | "filled", theme: StudioTheme) => any = (variant, theme) => withStyles((muiTheme: Theme) =>
    createStyles({
        root: {
            fontFamily: "inherit",
        },

        input: {
            borderRadius: 5,
            position: 'relative',
            border: `1px solid ${variant === "outlined" ? "rgba(255,255,255,.2)" : "transparent"}`,
            backgroundColor: variant === "outlined" ? "transparent" : theme.textFieldBackground,
            color: theme.textColor,
            fontSize: 16,
            padding: '10px 26px 10px 12px',
            transition: muiTheme.transitions.create(['border-color', 'box-shadow']),

            '&:focus': {
                borderRadius: 5,
                borderColor: theme.textFieldFocusOutline,
            },

            "& option": {
                backgroundColor: `${theme.textFieldBackground} !important`,
                color: theme.textColor,

                "&[disabled]": {
                    color: "rgba(255,255,255,.3)",
                }
            },
        },
    }),
)(InputBase);

interface StudioSelectOptionProps {
    value: string;
}

export const StudioSelectOption: React.FC<StudioSelectOptionProps> = ({value}) => <option value={value}>{value}</option>;
