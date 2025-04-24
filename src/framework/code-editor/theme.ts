import { EditorView } from '@codemirror/view'
import { Extension } from '@codemirror/state'
import { HighlightStyle, syntaxHighlighting } from '@codemirror/language'
import { tags as t } from '@lezer/highlight'

const base00 = '#0e0d17',
    base01 = '#232135',
    base02 = '#2d2a46',
    base03 = '#383649',
    base04 = '#4e4b63',
    base05 = '#63607c',
    base06 = '#958fa8',
    base07 = '#d7d4e5',
    base08 = '#A54543',
    base09 = '#fc6d24',
    base0A = '#fda331',
    base0B = '#8abeb7',
    base0C = '#b5bd68',
    base0D = '#02dac9',
    base0E = '#ff87dc',
    base0F = '#6987AF'

const invalid = base09,
    darkBackground = base00,
    highlightBackground = base02 + '90',
    background = base00,
    tooltipBackground = base01,
    selection = base06,
    cursor = base07

/// The editor theme styles for Basic Dark.
export const basicDarkTheme = EditorView.theme(
    {
        '&': {
            color: base0E,
            backgroundColor: background
        },

        '.cm-content': {
            caretColor: cursor
        },

        '.cm-scroller': {
            fontSize: '14px',
            lineHeight: '21px',
            fontFamily: '"Monaco", monospace',
        },

        '.cm-cursor, .cm-dropCursor': { borderLeftColor: cursor },
        '&.cm-focused > .cm-scroller > .cm-selectionLayer .cm-selectionBackground, .cm-selectionBackground, .cm-content ::selection':
            { backgroundColor: selection },

        '.cm-panels': { backgroundColor: darkBackground, color: base03 },
        '.cm-panels.cm-panels-top': { borderBottom: '2px solid black' },
        '.cm-panels.cm-panels-bottom': { borderTop: '2px solid black' },

        '.cm-searchMatch': {
            backgroundColor: base02,
            outline: `1px solid ${base03}`,
            color: base07
        },
        '.cm-searchMatch.cm-searchMatch-selected': {
            backgroundColor: base05,
            color: base07
        },

        '.cm-activeLine': { backgroundColor: highlightBackground },
        '.cm-selectionMatch': { backgroundColor: highlightBackground },

        '&.cm-focused .cm-matchingBracket, &.cm-focused .cm-nonmatchingBracket': {
            outline: `1px solid ${base03}`
        },

        '&.cm-focused .cm-matchingBracket': {
            backgroundColor: base02,
            color: base07
        },

        '.cm-gutters': {
            borderRight: `1px solid #ffffff10`,
            color: base06,
            backgroundColor: darkBackground
        },

        '.cm-activeLineGutter': {
            backgroundColor: highlightBackground
        },

        '.cm-foldPlaceholder': {
            backgroundColor: 'transparent',
            border: 'none',
            color: base02
        },

        '.cm-tooltip': {
            border: 'none',
            backgroundColor: tooltipBackground
        },
        '.cm-tooltip .cm-tooltip-arrow:before': {
            borderTopColor: 'transparent',
            borderBottomColor: 'transparent'
        },
        '.cm-tooltip .cm-tooltip-arrow:after': {
            borderTopColor: tooltipBackground,
            borderBottomColor: tooltipBackground
        },
        '.cm-tooltip-autocomplete': {
            '& > ul > li[aria-selected]': {
                backgroundColor: highlightBackground,
                color: base03
            }
        }
    },
    { dark: true }
)

/// The highlighting style for code in the Basic Light theme.
export const basicDarkHighlightStyle = HighlightStyle.define([
    { tag: t.keyword, color: base0A },
    {
        tag: [t.name, t.deleted, t.character, t.propertyName, t.macroName],
        color: base0C
    },
    { tag: [t.variableName], color: base0D },
    { tag: [t.function(t.variableName)], color: base0A },
    { tag: [t.labelName], color: base09 },
    {
        tag: [t.color, t.constant(t.name), t.standard(t.name)],
        color: base0A
    },
    { tag: [t.definition(t.name), t.separator], color: base0E },
    { tag: [t.brace], color: base0E },
    {
        tag: [t.annotation],
        color: invalid
    },
    {
        tag: [t.number, t.changed, t.annotation, t.modifier, t.self, t.namespace],
        color: base0A
    },
    {
        tag: [t.typeName, t.className],
        color: base0D
    },
    {
        tag: [t.operator, t.operatorKeyword],
        color: base0E
    },
    {
        tag: [t.tagName],
        color: base0A
    },
    {
        tag: [t.squareBracket],
        color: base0E
    },
    {
        tag: [t.angleBracket],
        color: base0E
    },
    {
        tag: [t.attributeName],
        color: base0D
    },
    {
        tag: [t.regexp],
        color: base0A
    },
    {
        tag: [t.quote],
        color: base01
    },
    { tag: [t.string], color: base0C },
    {
        tag: t.link,
        color: base0F,
        textDecoration: 'underline',
        textUnderlinePosition: 'under'
    },
    {
        tag: [t.url, t.escape, t.special(t.string)],
        color: base0B
    },
    { tag: [t.meta], color: base08 },
    { tag: [t.comment], color: base06, fontStyle: 'italic' },
    {
        tag: t.monospace,
        color: base01,
    },
    { tag: t.strong, fontWeight: 'bold', color: base0A },
    { tag: t.emphasis, fontStyle: 'italic', color: base0D },
    { tag: t.strikethrough, textDecoration: 'line-through' },
    { tag: t.heading, fontWeight: 'bold', color: base01 },
    { tag: t.special(t.heading1), fontWeight: 'bold', color: base01 },
    { tag: t.heading1, fontWeight: 'bold', color: base01 },
    {
        tag: [t.heading2, t.heading3, t.heading4],
        fontWeight: 'bold',
        color: base01
    },
    {
        tag: [t.heading5, t.heading6],
        color: base01
    },
    { tag: [t.atom, t.bool, t.special(t.variableName)], color: base0B },
    {
        tag: [t.processingInstruction, t.inserted],
        color: base0B
    },
    {
        tag: [t.contentSeparator],
        color: base0D
    },
    { tag: t.invalid, color: base02, borderBottom: `1px dotted ${invalid}` }
])

/// Extension to enable the Basic Dark theme (both the editor theme and
/// the highlight style).
export const basicDark: Extension = [
    basicDarkTheme,
    syntaxHighlighting(basicDarkHighlightStyle)
];
