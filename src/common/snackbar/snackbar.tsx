import React from "react";
import Snackbar from "@material-ui/core/Snackbar";
import IconButton from "@material-ui/core/IconButton";
import CloseIcon from "@material-ui/icons/Close";
import { snackbarStyles } from "./snackbar-styles";
import clsx from "clsx";

export type SnackbarVariant = "success" | "error";

interface StudioSnackbarProps {
    variant: SnackbarVariant;
    message: string;
    // TODO: We're seeing this pattern quite a lot - maybe we should create a standard for bidirectional state binding?
    open: boolean;
    setOpen: (open: boolean) => void;
}

export const StudioSnackbar: React.FC<StudioSnackbarProps> = ({variant, message, open, setOpen}) => {
    const classes = snackbarStyles();
    const handleClose = (_e: any, reason: string) => {
        if (reason !== "clickaway") setOpen(false);
    }

    const paragraphs = message.split("\n\n").map(paragraph => paragraph.split("\n"));
    const prefixMatches = paragraphs[0][0].match(/^\[[A-Z]{3}[0-9]+][^:]*:/);
    const messageElem = (
        <>
            {prefixMatches ? (
                <>
                    <h5>{prefixMatches[0].slice(0, prefixMatches[0].length - 1)}</h5>
                    <p>
                        {paragraphs[0][0].slice(prefixMatches[0].length)}
                        {paragraphs[0].slice(1).map(line => <><br/>{line}</>)}
                    </p>
                </>
            ) : <p>{paragraphs[0].map(line => <><br/>{line}</>)}</p>}

            {paragraphs.slice(1).map(paragraph => (
                <p>
                    {paragraph[0]}
                    {paragraph.slice(1).map(line => <><br/>{line}</>)}
                </p>
            ))}
        </>
    );

    return (
        <Snackbar anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }} open={open} autoHideDuration={8000}
                  onClose={handleClose}
                  classes={{ root: clsx(classes.root, variant === "success" && classes.success, variant === "error" && classes.error) }}
                  ContentProps={{ classes: { root: classes.content, message: classes.message } }}
                  message={messageElem}
                  action={
                  <IconButton size="small" aria-label="close" color="inherit" onClick={() => setOpen(false)} className={classes.close}>
                      <CloseIcon fontSize="small" />
                  </IconButton>}
        />
    );
}
