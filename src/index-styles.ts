import makeStyles from "@material-ui/core/styles/makeStyles";
import { ThemeProps } from "./styles/theme";

export const indexStyles = makeStyles({
    root: {
        height: "100%",
        background: (props: ThemeProps) => props.theme.background,
    },
});
