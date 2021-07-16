import makeStyles from "@material-ui/core/styles/makeStyles";
import { ThemeProps } from "../../styles/theme";

export const autocompleteStyles = makeStyles({
    paper: {
        background: (props: ThemeProps) => props.theme.textField.background,
        color: (props: ThemeProps) => props.theme.textColor,
    },

    clearIndicator: {
        color: (props: ThemeProps) => props.theme.textColor,
    },
});
