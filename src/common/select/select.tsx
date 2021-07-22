import MenuItem from "@material-ui/core/MenuItem";
import InputBase from "@material-ui/core/InputBase";
import { ClassProps } from "../class-props";
import React, { ChangeEvent } from "react";
import FormControl from "@material-ui/core/FormControl";
import Select from "@material-ui/core/Select";
import ExpandMoreIcon from "@material-ui/icons/ExpandMore";
import withStyles from "@material-ui/core/styles/withStyles";
import { Theme } from "@material-ui/core/styles/createTheme";
import createStyles from "@material-ui/core/styles/createStyles";
import { selectStyles } from "./select-styles";
import { StudioTheme } from "../../styles/theme";
import { themeState } from "../../state/state";

interface StudioSelectProps<TItem extends string | number> extends ClassProps {
    label?: string;
    value: TItem;
    setValue: (value: TItem) => void;
    variant: "outlined" | "filled";
    onOpen?: (event: ChangeEvent<{}>) => void;
}

export function StudioSelect<TItem extends string | number>({children, className, label, value, setValue, variant, onOpen}: React.PropsWithChildren<StudioSelectProps<TItem>>): JSX.Element {
    const classes = selectStyles({ theme: themeState.use()[0] });

    const inputElement = React.createElement(studioSelectInput(variant, themeState.use()[0]));

    return (
        <FormControl variant="outlined" className={className}>
            <Select label={label} value={value} onChange={(e) => setValue(e.target.value as TItem)}
                    className={classes.select} input={inputElement} onOpen={onOpen}
                    MenuProps={{classes: {paper: classes.paper}}}
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
            backgroundColor: variant === "outlined" ? "transparent" : theme.textField.background,
            color: theme.textColor,
            fontSize: 16,
            padding: '11px 26px 11px 12px',
            transition: muiTheme.transitions.create(['border-color', 'box-shadow']),

            '&:focus': {
                borderRadius: 5,
                borderColor: theme.textField.focus.borderColor,
            },

            "& option": {
                backgroundColor: `${theme.textField.background} !important`,
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

export const StudioSelectOption: React.FC<StudioSelectOptionProps> = ({value}) => <MenuItem value={value}>{value}</MenuItem>;
