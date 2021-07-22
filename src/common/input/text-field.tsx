import clsx from "clsx";
import React from "react";
import TextField from "@material-ui/core/TextField";
import { themeState } from "../../state/state";
import { textFieldStyles } from "./input-styles";

interface StudioTextFieldProps {
    name?: string;
    autocomplete?: string;
    type?: string;
    value: string;
    setValue?: (value: string) => void;
    label: string;
    multiline?: boolean;
    required?: boolean;
    InputProps?: any;
    invalid?: boolean;
}

export const StudioTextField: React.FC<StudioTextFieldProps> = ({name, autocomplete, type, value, setValue, label, multiline, required, invalid, ...rest}) => {
    const classes = textFieldStyles({ theme: themeState.use()[0] });

    return <TextField label={label} variant="outlined" type={type} multiline={multiline} rows={10}
                      name={name} value={value} autoComplete={autocomplete} {...rest}
                      onChange={setValue && ((e) => setValue(e.target.value))} required={required}
                      classes={{root: clsx(classes.textField, invalid && classes.invalid)}}
                      InputProps={{ ...rest.InputProps, classes: {root: clsx(classes.inputRoot, invalid && classes.invalid), multiline: classes.inputMultiline, focused: classes.inputFocused}}}
                      InputLabelProps={{classes: {shrink: classes.inputLabelShrink, outlined: classes.inputLabelOutlined}}}/>
}
