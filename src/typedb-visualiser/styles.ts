const palette = {
    red: 0xF66B65,
    gold: 0xEBC53D,
    yellow: 0xFFE4A7,
    green: 0x02DAC9,
    skyBlue: 0x92E4FC,
    blue: 0x7BA0FF,
    pink: 0xFFA9E8,
    purple: 0xE69CFF,
    deepPurple: 0x0E053F,
    black: 0x09022F,
    white: 0xFFFFFF,
}
export const typeDBVisualiserPalette = palette;

export interface TypeDBVisualiserTheme {
    colors: {
        background: number;
        entityType: number;
        relationType: number;
        attributeType: number;
        entity: number;
        relation: number;
        attribute: number;
        edge: number;
        inferred: number;
        error: number;
        vertexLabel: number;
    }
}

const defaultTheme: TypeDBVisualiserTheme = {
    colors: {
        background: palette.deepPurple,
        entityType: palette.pink,
        relationType: palette.yellow,
        attributeType: palette.blue,
        entity: palette.purple,
        relation: palette.gold,
        attribute: palette.skyBlue,
        edge: palette.blue,
        inferred: palette.green,
        error: palette.red,
        vertexLabel: palette.black,
    }
};
export const defaultTypeDBVisualiserTheme = defaultTheme;

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
