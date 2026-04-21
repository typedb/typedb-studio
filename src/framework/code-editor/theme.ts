/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { EditorView } from "@codemirror/view"
import { Extension } from "@codemirror/state"
import { HighlightStyle, syntaxHighlighting } from "@codemirror/language"
import { tags as t } from "@lezer/highlight"

// ============================================================
// DARK THEME
// ============================================================

const darkBase00 = "#0e0e0e",
    darkBase01 = "#232135",
    darkBase02 = "#2d2a46",
    darkBase03 = "#383649",
    darkBase04 = "#4e4b63",
    darkBase05 = "#63607c",
    darkBase06 = "#958fa8",
    darkBase07 = "#d7d4e5",
    darkBase08 = "#A54543",
    darkBase09 = "#fc6d24",
    darkConstraint = "#c099f3",
    darkBase0B = "#8abeb7",
    darkString = "#ffe4a7",
    darkVariable = "#55eae2",
    darkKeyword = "#ff7abd",
    darkBase0F = "#6987AF"

const darkInvalid = darkBase09,
    darkDarkBackground = darkBase00,
    darkHighlightBackground = darkBase02 + "90",
    darkBackground = darkBase00,
    darkTooltipBackground = darkBase03 + "A0",
    darkSelection = darkBase06,
    darkCursor = darkBase07

export const basicDarkTheme = EditorView.theme(
    {
        "&": {
            color: darkBase07,
            backgroundColor: darkBackground
        },

        ".cm-content": {
            caretColor: darkCursor
        },

        ".cm-scroller": {
            fontSize: "14px",
            lineHeight: "22px",
            fontFamily: "'Monaco', monospace",
        },

        ".cm-cursor, .cm-dropCursor": { borderLeftColor: darkCursor },
        "&.cm-focused > .cm-scroller > .cm-selectionLayer .cm-selectionBackground, .cm-selectionBackground, .cm-content ::selection":
            { backgroundColor: darkSelection },

        ".cm-panels": { backgroundColor: darkDarkBackground, color: "#c4c4c4" },
        ".cm-panels.cm-panels-top": { borderBottom: "2px solid black" },
        ".cm-panels.cm-panels-bottom": { borderTop: "2px solid black" },

        ".cm-textfield": { fontSize: "13px", borderRadius: "4px", fontFamily: "inherit" },
        ".cm-button": { fontSize: "13px", borderRadius: "4px", backgroundColor: "var(--theme-deep-purple)", backgroundImage: "none", borderColor: "var(--theme-light-purple)", transition: "background-color 100ms" },
        ".cm-button:hover": { backgroundColor: "color-mix(in srgb, var(--theme-deep-purple), var(--theme-white) 8%)" },
        ".cm-panel.cm-search label": { fontSize: "13px", display: "inline-flex", alignItems: "center", gap: "4px", verticalAlign: "middle" },
        ".cm-panel.cm-search [name=close]": { fontSize: "20px", color: "var(--theme-white)" },

        ".cm-searchMatch": {
            backgroundColor: darkBase02,
            outline: `1px solid ${darkBase03}`,
            color: darkBase07
        },
        ".cm-searchMatch.cm-searchMatch-selected": {
            backgroundColor: darkBase05,
            color: darkBase07
        },

        ".cm-activeLine": { backgroundColor: darkHighlightBackground },
        ".cm-selectionMatch": { backgroundColor: darkHighlightBackground },

        "&.cm-focused .cm-matchingBracket, &.cm-focused .cm-nonmatchingBracket": {
            outline: `1px solid ${darkBase03}`
        },

        "&.cm-focused .cm-matchingBracket": {
            backgroundColor: darkBase02,
            color: darkBase07
        },

        ".cm-gutters": {
            borderRight: `1px solid #ffffff10`,
            color: darkBase06,
            backgroundColor: darkDarkBackground
        },

        ".cm-activeLineGutter": {
            backgroundColor: darkHighlightBackground
        },

        ".cm-foldPlaceholder": {
            backgroundColor: "transparent",
            border: "none",
            color: darkBase02
        },

        ".cm-tooltip": {
            border: "none",
            backgroundColor: darkTooltipBackground,
        },
        ".cm-tooltip .cm-tooltip-arrow:before": {
            borderTopColor: "transparent",
            borderBottomColor: "transparent"
        },
        ".cm-tooltip .cm-tooltip-arrow:after": {
            borderTopColor: darkTooltipBackground,
            borderBottomColor: darkTooltipBackground
        },
        ".cm-tooltip-autocomplete": {
            "& > ul > li[aria-selected]": {
                backgroundColor: darkTooltipBackground,
                color: darkBase07
            }
        },
        ".cm-tooltip.cm-completionInfo": {
            fontSize: "12px",
        },
    },
    { dark: true }
)

export const basicDarkHighlightStyle = HighlightStyle.define([
    { tag: t.keyword, color: darkConstraint },
    { tag: t.className, color: "#02dac9", fontStyle: "italic" },
    { tag: [t.variableName], color: darkVariable },
    { tag: [t.function(t.variableName)], color: darkConstraint },
    { tag: [t.labelName], color: darkBase09 },
    {
        tag: [t.color, t.constant(t.name), t.standard(t.name)],
        color: darkConstraint
    },
    { tag: [t.definition(t.name), t.separator], color: darkKeyword },
    { tag: [t.brace], color: darkKeyword },
    {
        tag: [t.annotation],
        color: darkInvalid
    },
    {
        tag: [t.number, t.changed, t.annotation, t.modifier, t.self, t.namespace],
        color: darkConstraint
    },
    {
        tag: [t.operator, t.operatorKeyword],
        color: darkKeyword
    },
    {
        tag: [t.tagName],
        color: darkConstraint
    },
    {
        tag: [t.squareBracket],
        color: darkKeyword
    },
    {
        tag: [t.angleBracket],
        color: darkKeyword
    },
    {
        tag: [t.attributeName],
        color: darkVariable
    },
    {
        tag: [t.regexp],
        color: darkConstraint
    },
    {
        tag: [t.quote],
        color: darkBase01
    },
    { tag: [t.string], color: darkString },
    {
        tag: t.link,
        color: darkBase0F,
        textDecoration: "underline",
        textUnderlinePosition: "under"
    },
    {
        tag: [t.url, t.escape, t.special(t.string)],
        color: darkBase0B
    },
    { tag: [t.meta], color: "#ffa187" },
    { tag: [t.comment], color: darkBase06, fontStyle: "italic" },
    {
        tag: t.monospace,
        color: darkBase01,
    },
    { tag: t.strong, fontWeight: "bold", color: darkConstraint },
    { tag: t.emphasis, fontStyle: "italic", color: darkVariable },
    { tag: t.strikethrough, textDecoration: "line-through" },
    { tag: t.heading, color: darkKeyword },
    { tag: [t.atom, t.bool, t.special(t.variableName)], color: darkBase0B },
    {
        tag: [t.processingInstruction, t.inserted],
        color: darkBase0B
    },
    {
        tag: [t.contentSeparator],
        color: darkVariable
    },
    { tag: t.invalid, color: darkBase02, borderBottom: `1px dotted ${darkInvalid}` }
])

export const basicDark: Extension = [
    basicDarkTheme,
    syntaxHighlighting(basicDarkHighlightStyle)
];

// ============================================================
// LIGHT THEME
// ============================================================

const lightBase00 = "#fafafa",
    lightBase01 = "#e8e5f0",
    lightBase02 = "#d0cde4",
    lightBase03 = "#b0adc0",
    lightBase04 = "#8a86a0",
    lightBase05 = "#6b6580",
    lightBase06 = "#4a4a5a",
    lightBase07 = "#1a1a2e",
    lightBase08 = "#A54543",
    lightBase09 = "#cc5530",
    lightConstraint = "#7b44c0",
    lightBase0B = "#007a6e",
    lightString = "#8a6d10",
    lightVariable = "#007a6e",
    lightKeyword = "#c040a0",
    lightBase0F = "#3366aa"

const lightInvalid = lightBase09,
    lightDarkBackground = lightBase00,
    lightHighlightBackground = lightBase02 + "60",
    lightBackground = lightBase00,
    lightTooltipBackground = lightBase01 + "E0",
    lightSelection = lightBase02,
    lightCursor = lightBase07

export const basicLightTheme = EditorView.theme(
    {
        "&": {
            color: lightBase07,
            backgroundColor: lightBackground
        },

        ".cm-content": {
            caretColor: lightCursor
        },

        ".cm-scroller": {
            fontSize: "14px",
            lineHeight: "22px",
            fontFamily: "'Monaco', monospace",
        },

        ".cm-cursor, .cm-dropCursor": { borderLeftColor: lightCursor },
        "&.cm-focused > .cm-scroller > .cm-selectionLayer .cm-selectionBackground, .cm-selectionBackground, .cm-content ::selection":
            { backgroundColor: lightSelection },

        ".cm-panels": { backgroundColor: lightDarkBackground, color: lightBase05 },
        ".cm-panels.cm-panels-top": { borderBottom: `1px solid ${lightBase02}` },
        ".cm-panels.cm-panels-bottom": { borderTop: `1px solid ${lightBase02}` },

        ".cm-textfield": { fontSize: "13px", borderRadius: "4px", fontFamily: "inherit" },
        ".cm-button": { fontSize: "13px", borderRadius: "4px", backgroundColor: "var(--theme-deep-purple)", backgroundImage: "none", borderColor: "var(--theme-light-purple)", transition: "background-color 100ms" },
        ".cm-button:hover": { backgroundColor: "color-mix(in srgb, var(--theme-deep-purple), var(--theme-white) 8%)" },
        ".cm-panel.cm-search label": { fontSize: "13px", display: "inline-flex", alignItems: "center", gap: "4px", verticalAlign: "middle" },
        ".cm-panel.cm-search [name=close]": { fontSize: "20px", color: "var(--theme-white)" },

        ".cm-searchMatch": {
            backgroundColor: "#e8e0ff",
            outline: `1px solid ${lightBase03}`,
            color: lightBase07
        },
        ".cm-searchMatch.cm-searchMatch-selected": {
            backgroundColor: lightBase02,
            color: lightBase07
        },

        ".cm-activeLine": { backgroundColor: lightHighlightBackground },
        ".cm-selectionMatch": { backgroundColor: lightHighlightBackground },

        "&.cm-focused .cm-matchingBracket, &.cm-focused .cm-nonmatchingBracket": {
            outline: `1px solid ${lightBase03}`
        },

        "&.cm-focused .cm-matchingBracket": {
            backgroundColor: lightBase02,
            color: lightBase07
        },

        ".cm-gutters": {
            borderRight: `1px solid ${lightBase02}`,
            color: lightBase04,
            backgroundColor: lightDarkBackground
        },

        ".cm-activeLineGutter": {
            backgroundColor: lightHighlightBackground
        },

        ".cm-foldPlaceholder": {
            backgroundColor: "transparent",
            border: "none",
            color: lightBase03
        },

        ".cm-tooltip": {
            border: `1px solid ${lightBase02}`,
            backgroundColor: lightTooltipBackground,
        },
        ".cm-tooltip .cm-tooltip-arrow:before": {
            borderTopColor: "transparent",
            borderBottomColor: "transparent"
        },
        ".cm-tooltip .cm-tooltip-arrow:after": {
            borderTopColor: lightTooltipBackground,
            borderBottomColor: lightTooltipBackground
        },
        ".cm-tooltip-autocomplete": {
            "& > ul > li[aria-selected]": {
                backgroundColor: lightBase02,
                color: lightBase07
            }
        },
        ".cm-tooltip.cm-completionInfo": {
            fontSize: "12px",
        },
    },
    { dark: false }
)

export const basicLightHighlightStyle = HighlightStyle.define([
    { tag: t.keyword, color: lightConstraint },
    { tag: t.className, color: "#008f80", fontStyle: "italic" },
    { tag: [t.variableName], color: lightVariable },
    { tag: [t.function(t.variableName)], color: lightConstraint },
    { tag: [t.labelName], color: lightBase09 },
    {
        tag: [t.color, t.constant(t.name), t.standard(t.name)],
        color: lightConstraint
    },
    { tag: [t.definition(t.name), t.separator], color: lightKeyword },
    { tag: [t.brace], color: lightKeyword },
    {
        tag: [t.annotation],
        color: lightInvalid
    },
    {
        tag: [t.number, t.changed, t.annotation, t.modifier, t.self, t.namespace],
        color: lightConstraint
    },
    {
        tag: [t.operator, t.operatorKeyword],
        color: lightKeyword
    },
    {
        tag: [t.tagName],
        color: lightConstraint
    },
    {
        tag: [t.squareBracket],
        color: lightKeyword
    },
    {
        tag: [t.angleBracket],
        color: lightKeyword
    },
    {
        tag: [t.attributeName],
        color: lightVariable
    },
    {
        tag: [t.regexp],
        color: lightConstraint
    },
    {
        tag: [t.quote],
        color: lightBase05
    },
    { tag: [t.string], color: lightString },
    {
        tag: t.link,
        color: lightBase0F,
        textDecoration: "underline",
        textUnderlinePosition: "under"
    },
    {
        tag: [t.url, t.escape, t.special(t.string)],
        color: lightBase0B
    },
    { tag: [t.meta], color: "#cc5530" },
    { tag: [t.comment], color: lightBase04, fontStyle: "italic" },
    {
        tag: t.monospace,
        color: lightBase05,
    },
    { tag: t.strong, fontWeight: "bold", color: lightConstraint },
    { tag: t.emphasis, fontStyle: "italic", color: lightVariable },
    { tag: t.strikethrough, textDecoration: "line-through" },
    { tag: t.heading, color: lightKeyword },
    { tag: [t.atom, t.bool, t.special(t.variableName)], color: lightBase0B },
    {
        tag: [t.processingInstruction, t.inserted],
        color: lightBase0B
    },
    {
        tag: [t.contentSeparator],
        color: lightVariable
    },
    { tag: t.invalid, color: lightBase02, borderBottom: `1px dotted ${lightInvalid}` }
])

export const basicLight: Extension = [
    basicLightTheme,
    syntaxHighlighting(basicLightHighlightStyle)
];
