const red = 0xF66B65;
const gold = 0xEBC53D;
const yellow = 0xFFE4A7;
const green = 0x02DAC9;
const skyBlue = 0x92E4FC;
const blue = 0x7BA0FF;
const pink = 0xFFA9E8;
const purple = 0xE69CFF;
const deepPurple = 0x0E053F;
const black = 0x09022F;

export const defaultColors = {
    background: deepPurple,
    entityType: pink,
    relationType: yellow,
    attributeType: blue,
    entity: purple,
    relation: gold,
    attribute: skyBlue,
    edge: blue,
    inferred: green,
    error: red,
    vertexLabel: black,
};

export const defaultStyles = {
    fontFamily: "Ubuntu Mono",
    fontFamilyFallback: "monospace",

    vertexLabel: {
        fontSize: 16,
    },

    edgeLabel: {
        fontSize: 14,
    },
};
