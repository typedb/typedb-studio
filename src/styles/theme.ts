import { createMuiTheme } from '@material-ui/core/styles';
import createSpacing from '@material-ui/core/styles/createSpacing';

// TODO: Copied from web-main
export type SizeIndicator = 'smallest' | 'smaller' | 'small' | 'medium' | 'large' | 'larger' | 'largest';

declare module '@material-ui/core/styles/createMuiTheme' {
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

export const vaticleMuiTheme = createMuiTheme({
    vaticle: vaticleTheme,
    typography: {
        fontFamily: "'Titillium Web', Geneva, Tahoma, sans-serif",
        fontSize: 16,
        body1: {
            fontSize: 16,
        },
    },
});
