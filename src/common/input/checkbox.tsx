import Checkbox, { CheckboxProps } from "@material-ui/core/Checkbox";
import FormControlLabel, { FormControlLabelProps } from "@material-ui/core/FormControlLabel";
import withStyles from "@material-ui/core/styles/withStyles";
import React from "react";
import { vaticleTheme } from "../../styles/theme";

export const StudioCheckbox = withStyles({
    root: {
        color: "transparent",
		paddingTop: 0,
		paddingBottom: 0,

        "& svg": {
            borderRadius: 5,
            backgroundColor: vaticleTheme.palette.purple["3"],
        },

        '&$checked': {
            color: vaticleTheme.palette.purple["8"],
        },
    },
    checked: {},
})((props: CheckboxProps) => <Checkbox size="small" color="default" {...props} />);

export const StudioFormControlLabel = withStyles({
	root: {
		marginRight: "0 !important",
		textAlign: "start",
	},

	label: {
		fontSize: 14,
	},
})((props: FormControlLabelProps) => <FormControlLabel {...props}/>);
