import { EditorView } from '@codemirror/view';
import { Extension } from '@codemirror/state';
import { HighlightStyle, syntaxHighlighting } from '@codemirror/language';
import { tags as t } from '@lezer/highlight';

// This theme is a conversion of
// [oneDark theme](https://github.com/codemirror/theme-one-dark/blob/main/src/one-dark.ts).

const chalky = '#c18401',
    coral = '#a62626',
    cyan = '#0184bc',
    invalid = '#ff0000',
    ivory = '#383a42',
    stone = '#a0a1a7',
    malibu = '#4078f2',
    sage = '#50a14f',
    whiskey = '#986801',
    violet = '#a626a4',
    lightBackground = '#fafafa',
    highlightBackground = '#eaeaea',
    tooltipBackground = '#f0f0f0',
    selection = '#d0d0d0',
    cursor = '#526fff';

export const color = {
    chalky,
    coral,
    cyan,
    invalid,
    ivory,
    stone,
    malibu,
    sage,
    whiskey,
    violet,
    lightBackground,
    highlightBackground,
    tooltipBackground,
    selection,
    cursor,
};

export const oneLightTheme = EditorView.theme(
    {
        '&': {
            color: ivory,
            backgroundColor: lightBackground,
        },

        '.cm-content': {
            caretColor: cursor,
        },

        '.cm-cursor, .cm-dropCursor': { borderLeftColor: cursor },
        '&.cm-focused > .cm-scroller > .cm-selectionLayer .cm-selectionBackground, .cm-selectionBackground, .cm-content ::selection':
            {
                backgroundColor: selection,
            },

        '.cm-panels': { backgroundColor: '#f5f5f5', color: ivory },
        '.cm-panels.cm-panels-top': { borderBottom: '2px solid #ccc' },
        '.cm-panels.cm-panels-bottom': { borderTop: '2px solid #ccc' },

        '.cm-searchMatch': {
            backgroundColor: '#ffea00a0',
            outline: '1px solid #f5a623',
        },
        '.cm-searchMatch.cm-searchMatch-selected': {
            backgroundColor: '#f5d76e80',
        },

        '.cm-activeLine': { backgroundColor: '#f0f0f0' },
        '.cm-selectionMatch': { backgroundColor: '#d0f0c0' },

        '&.cm-focused .cm-matchingBracket, &.cm-focused .cm-nonmatchingBracket':
            {
                backgroundColor: '#d6f0ff',
            },

        '.cm-gutters': {
            backgroundColor: lightBackground,
            color: stone,
            border: 'none',
        },

        '.cm-activeLineGutter': {
            backgroundColor: highlightBackground,
        },

        '.cm-foldPlaceholder': {
            backgroundColor: 'transparent',
            border: 'none',
            color: '#999',
        },

        '.cm-tooltip': {
            border: '1px solid #ccc',
            backgroundColor: tooltipBackground,
        },
        '.cm-tooltip .cm-tooltip-arrow:before': {
            borderTopColor: 'transparent',
            borderBottomColor: 'transparent',
        },
        '.cm-tooltip .cm-tooltip-arrow:after': {
            borderTopColor: tooltipBackground,
            borderBottomColor: tooltipBackground,
        },
        '.cm-tooltip-autocomplete': {
            '& > ul > li[aria-selected]': {
                backgroundColor: highlightBackground,
                color: ivory,
            },
        },
    },
    { dark: false }
);

export const oneLightHighlightStyle = HighlightStyle.define([
    { tag: t.keyword, color: violet },
    {
        tag: [t.name, t.deleted, t.character, t.propertyName, t.macroName],
        color: coral,
    },
    { tag: [t.function(t.variableName), t.labelName], color: malibu },
    { tag: [t.color, t.constant(t.name), t.standard(t.name)], color: whiskey },
    { tag: [t.definition(t.name), t.separator], color: ivory },
    {
        tag: [
            t.typeName,
            t.className,
            t.number,
            t.changed,
            t.annotation,
            t.modifier,
            t.self,
            t.namespace,
        ],
        color: chalky,
    },
    {
        tag: [
            t.operator,
            t.operatorKeyword,
            t.url,
            t.escape,
            t.regexp,
            t.link,
            t.special(t.string),
        ],
        color: cyan,
    },
    { tag: [t.meta, t.comment], color: stone },
    { tag: t.strong, fontWeight: 'bold' },
    { tag: t.emphasis, fontStyle: 'italic' },
    { tag: t.strikethrough, textDecoration: 'line-through' },
    { tag: t.link, color: stone, textDecoration: 'underline' },
    { tag: t.heading, fontWeight: 'bold', color: coral },
    { tag: [t.atom, t.bool, t.special(t.variableName)], color: whiskey },
    { tag: [t.processingInstruction, t.string, t.inserted], color: sage },
    { tag: t.invalid, color: invalid },
]);

export const oneLight: Extension = [
    oneLightTheme,
    syntaxHighlighting(oneLightHighlightStyle),
];
