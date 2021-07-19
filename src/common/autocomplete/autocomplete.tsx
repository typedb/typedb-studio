import * as React from "react";
import Autocomplete from "@material-ui/lab/Autocomplete";
import { themeState } from "../../state/typedb-client";
import { StudioTextField } from "../input/text-field";
import { autocompleteStyles } from "./autocomplete-styles";

export interface StudioAutocompleteProps<TItem extends string> {
    label: string;
    value: TItem;
    onChange: (e: object, value: string) => void;
    onBlur: (e: React.FocusEvent<HTMLElement>) => void;
    invalid?: boolean;
    options: string[];
}

const StudioPopper = (props: any) => {
    const { className, anchorEl, style, ...rest } = props
    const bound = anchorEl.getBoundingClientRect()
    return <div {...rest} style={{
        position: 'absolute',
        zIndex: 9999,
        width: bound.width,
        marginTop: 40,
    }} />
}

export function StudioAutocomplete<TItem extends string>({label, value, onChange, onBlur, invalid, options}: React.PropsWithChildren<StudioAutocompleteProps<TItem>>): JSX.Element {
    const classes = autocompleteStyles({ theme: themeState.use()[0] });

    return (
        <Autocomplete freeSolo options={options} value={value} blurOnSelect={true} onChange={onChange} onBlur={onBlur}
                      renderInput={(params) => (
                <StudioTextField {...params} label={label} value={value} invalid={invalid} />
            )} PopperComponent={StudioPopper} classes={{clearIndicator: classes.clearIndicator, paper: classes.paper}}
        />
    );
}
