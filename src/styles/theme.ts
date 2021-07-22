import createSpacing from '@material-ui/core/styles/createSpacing';
import { defaultTypeDBVisualiserTheme, typeDBVisualiserPalette, TypeDBVisualiserTheme } from "../typedb-visualiser";
import createTheme from "@material-ui/core/styles/createTheme";

export type SizeIndicator = 'smallest' | 'smaller' | 'small' | 'medium' | 'large' | 'larger' | 'largest';

declare module '@material-ui/core/styles/createTheme' {
    interface Theme {
        vaticle: any;
    }

    interface ThemeOptions {
        vaticle?: any;
    }
}

export const vaticleTheme = {
    spacing: createSpacing(5),
    palette: {
        purple: {
            1: '#0E053F',
            2: '#180F49',
            3: '#1D1354',
            4: '#261C5E',
            5: '#372E6A',
            6: '#392D7F',
            7: '#544899',
            8: '#A488CA',
        },
        green: {
            1: '#02DAC9',
        },
        red: {
            1: '#F66B65',
            2: '#FFA187',
        },
        yellow: {
            1: '#F6C94C',
            2: '#FFE4A7',
        },
        pink: {
            1: '#F28DD7',
            2: '#FFA9E8',
        },
    },
    shape: {
        borderRadius: {
            smallest: 1,
            smaller: 2,
            small: 3,
            large: 6,
            larger: 8,
            largest: 10,
        },
    },
    typography: {
        fontFamily: {
            main: 'Titillium Web',
            fixedWidth: 'Ubuntu Mono',
        },
        htmlFontSize: 12,
        fontSize: {
            smallest: 12,
            smaller: 14,
            small: 16,
            medium: 18,
            large: 24,
            larger: 28,
            largest: 32,
        },
    },
    h1: {
        fontSize: 32,
        lineHeight: "56px",
        color: "#02DAC9",
        fontWeight: 400,

        "@media(max-width: 767px)": {
            fontSize: 24,
            lineHeight: "36px",
        },
    },
    h2: {
        fontSize: 32,
        lineHeight: "56px",
        color: '#FFF',
        fontWeight: 400,

        "@media(max-width: 767px)": {
            fontSize: 24,
            lineHeight: "36px",
        },
    },
    h3: {
        fontSize: 28,
        lineHeight: "43px",
        color: "#FFF",
        fontWeight: 400,
    },
    h4: {
        fontSize: 24,
        lineHeight: "36px",
        color: "#FFF",
        fontWeight: 400,

        "@media(max-width: 767px)": {
            fontSize: 20,
            lineHeight: "34px",
        },
    },
    h5: {
        fontSize: 20,
        lineHeight: "34px",
        fontWeight: 600,
    },
    h6: {
        fontSize: 16,
        lineHeight: "24px",
        fontWeight: 600,
    },
    p: {
        large: {
            color: '#FFF',
            fontSize: 20,
        }
    }
};

export const vaticleMuiTheme = createTheme({
    vaticle: vaticleTheme,
    typography: {
        fontFamily: "'Titillium Web', Geneva, Tahoma, sans-serif",
        fontSize: 16,
        body1: {
            fontSize: 16,
        },
    },
});

interface TextFieldTheme {
    background: string;
    borderColor: string;
    hover: { borderColor: string; };
    focus: { borderColor: string; };
}

interface SelectOptionTheme {
    background: string;
    hover: { background: string; };
}

interface ButtonTheme {
    border: string;
    background: string;
    color: string;

    hover: {
        border: string;
        background: string;
        color: string;
    }

    disabled: {
        background: string;
        border: string;
        color: string;
    }
}

interface ButtonThemes {
    primary: ButtonTheme;
    secondary: ButtonTheme;
}

const vaticleButtonThemes: ButtonThemes = {
    primary: {
        background: vaticleTheme.palette.green["1"],
        border: "1px solid transparent",
        color: vaticleTheme.palette.purple["3"],
        hover: {
            background: "#0B939F",
            border: "1px solid transparent",
            color: vaticleTheme.palette.purple["3"],
        },
        disabled: {
            background: vaticleTheme.palette.purple["7"],
            border: `1px solid ${vaticleTheme.palette.purple["7"]}`,
            color: "rgba(0,0,0,.38)",
        },
    },
    secondary: {
        background: "transparent",
        border: `1px solid ${vaticleTheme.palette.green["1"]}`,
        color: vaticleTheme.palette.green["1"],
        hover: {
            background: vaticleTheme.palette.green["1"],
            border: `1px solid ${vaticleTheme.palette.green["1"]}`,
            color: vaticleTheme.palette.purple["3"],
        },
        disabled: {
            background: "transparent",
            border: `1px solid ${vaticleTheme.palette.purple["7"]}`,
            color: vaticleTheme.palette.purple["7"],
        },
    },
}

interface TabsTheme {
    background: string;
    color: string;
    separatorColor: string;
    hover: { background: string },
    selected: {
        background: string,
        color: string,
        indicatorColor: string,
    },
}

export interface StudioTheme {
    id: string;
    background: string;
    panelSeparatorColor: string;
    windowBackdrop: string;
    textField: TextFieldTheme;
    selectOption: SelectOptionTheme;
    textColor: string;
    button: ButtonThemes;
    tabs: TabsTheme;
    visualiser: TypeDBVisualiserTheme;
}

export type ThemeProps = { theme: StudioTheme };

export const studioDarkTheme: StudioTheme = {
    id: "studioDark",
    background: vaticleTheme.palette.purple["4"],
    panelSeparatorColor: vaticleTheme.palette.purple["1"],
    windowBackdrop: vaticleTheme.palette.purple["1"],
    textField: {
        background: vaticleTheme.palette.purple["3"],
        borderColor: "transparent",
        hover: { borderColor: "rgba(255,255,255,.2)" },
        focus: { borderColor: vaticleTheme.palette.green["1"] },
    },
    selectOption: {
        background: vaticleTheme.palette.purple["2"],
        hover: { background: vaticleTheme.palette.purple["4"] },
    },
    textColor: "#FFF",
    button: vaticleButtonThemes,
    tabs: {
        background: vaticleTheme.palette.purple["3"],
        color: "#888DCA",
        separatorColor: vaticleTheme.palette.purple["1"],
        hover: { background: vaticleTheme.palette.purple["2"] },
        selected: {
            background: vaticleTheme.palette.purple["6"],
            color: "#FFF",
            indicatorColor: vaticleTheme.palette.purple["8"],
        },
    },
    visualiser: defaultTypeDBVisualiserTheme,
}

export const studioLightTheme: StudioTheme = {
    id: "studioLight",
    background: "#E4E4E4",
    panelSeparatorColor: "#BBB",
    windowBackdrop: "#F0F0F0",
    textField: {
        background: "#FBFBFB",
        borderColor: "transparent",
        hover: { borderColor: "rgba(255,255,255,.2)" },
        focus: { borderColor: vaticleTheme.palette.purple["7"] },
    },
    selectOption: {
        background: "#F0F0F0",
        hover: { background: "#E4E4E4" },
    },
    textColor: "#222",
    button: vaticleButtonThemes,
    tabs: {
        background: "#E4E4E4",
        color: "#222",
        separatorColor: "#DDD",
        hover: { background: "#DDD" },
        selected: {
            background: "#FBFBFB",
            color: "#111",
            indicatorColor: "#AAA",
        },
    },
    visualiser: {
        colors: {
            numeric: {
                ...defaultTypeDBVisualiserTheme.colors.numeric,
                background: Number(`0x${typeDBVisualiserPalette.white.slice(1)}`),
            },
            hex: {
                ...defaultTypeDBVisualiserTheme.colors.hex,
                background: typeDBVisualiserPalette.white,
            },
        },
    },
}
