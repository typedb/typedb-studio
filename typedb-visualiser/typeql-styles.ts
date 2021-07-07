import { makeStyles } from "@material-ui/core";

export const typeQLVisualiserStyles = makeStyles({
    graphPane: {
        position: "absolute",
        top: 0,
        left: 100,
        bottom: 0,
        right: 0,
        backgroundColor: "#0E053F",
        zIndex: 25,

        "& canvas": {
            "@media(max-width: 767px)": {
                pointerEvents: "none",
            },
        },
    },
});

const green = 0x02DAC9;
const pink = 0xFFA9E8;
const yellow = 0xFFE4A7;
const red = 0xF66B65;
const blue = 0x7BA0FF;
const black = 0x09022F;

export const typeQLGraphColours = {
    entity: pink,
    relation: yellow,
    attribute: blue,
    edge: blue,
    inferred: green,
    error: red,
    vertexLabel: black,
};

export const typeQLGraphStyles = {
    fontFamily: "Ubuntu Mono",
    fontFamilyFallback: "monospace",

    vertexLabel: {
        fontSize: 16,
    },

    edgeLabel: {
        fontSize: 14,
    },
};
