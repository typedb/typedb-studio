import * as React from "react";
import Autocomplete from "@material-ui/lab/Autocomplete";
import TextField from "@material-ui/core/TextField";

export interface StudioAutocompleteProps<TItem extends string> {
    label: string;
    value: TItem;
    setValue: (value: TItem) => void;
    options: string[];
}

export function StudioAutocomplete<TItem extends string>({label, value, setValue, options}: React.PropsWithChildren<StudioAutocompleteProps<TItem>>): JSX.Element {
    return (
        <Autocomplete freeSolo options={options} value={value} onChange={(_, val) => setValue(val as TItem)}
            renderInput={(params) => (
                <TextField {...params} label={label} margin="normal" variant="outlined" />
            )}
        />
    );
}
