const palette: {[key: string]: string} = {
    red: "#F66B65",
    gold: "#EBC53D",
    yellow: "#FFE4A7",
    green: "#02DAC9",
    skyBlue: "#92E4FC",
    blue: "#7BA0FF",
    pink: "#FFA9E8",
    purple: "#E69CFF",
    deepPurple: "#0E053F",
    black: "#09022F",
    white: "#FFFFFF",
}
export const typeDBVisualiserPalette = palette;

type ColorKey = "background" | "thingType" | "entityType" | "relationType" | "attributeType" | "entity" | "relation"
    | "attribute" | "edge" | "inferred" | "error" | "vertexLabel";

export interface TypeDBVisualiserTheme {
    colors: {
        numeric: {[key in ColorKey]: number};
        hex: {[key in ColorKey]: string};
    }
}

type ColorMapping = {[key in ColorKey]: string};

const defaultColorMapping: ColorMapping = {
    background: palette.deepPurple,
    thingType: palette.pink,
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

const defaultTheme: TypeDBVisualiserTheme = {
    colors: {
        numeric: Object.entries(defaultColorMapping).reduce((current, [nextKey, nextValue]) => {
            current[nextKey as ColorKey] = Number(`0x${nextValue.slice(1)}`);
            return current;
        }, {} as {[key in ColorKey]: number}),

        hex: Object.entries(defaultColorMapping).reduce((current, [nextKey, nextValue]) => {
            current[nextKey as ColorKey] = nextValue;
            return current;
        }, {} as {[key in ColorKey]: string}),
    }
}

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
