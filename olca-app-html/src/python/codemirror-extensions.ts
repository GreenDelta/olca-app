import {
    EditorView,
    lineNumbers,
    highlightSpecialChars,
    drawSelection,
    dropCursor,
    highlightActiveLine,
    highlightActiveLineGutter,
} from '@codemirror/view';
import { defaultKeymap, history } from '@codemirror/commands';
import { Extension } from '@codemirror/state';
import {
    globalCompletion,
    localCompletionSource,
    python,
} from '@codemirror/lang-python';
import { highlightSelectionMatches } from '@codemirror/search';
import {
    bracketMatching,
    foldGutter,
    indentOnInput,
    indentUnit,
    syntaxTree,
} from '@codemirror/language';
import { oneDark } from '@codemirror/theme-one-dark';
import { keymap } from '@codemirror/view';
import {
    autocompletion,
    closeBrackets,
    CompletionContext,
} from '@codemirror/autocomplete';
import olcaCompletions from '../../olca-completions.json';
import { oneLight } from './one-light';

export type Theme = 'light' | 'dark';

const indent = () => [
    indentUnit.of('    '),
    EditorView.theme({
        '.cm-content': {
            tabSize: '4',
        },
    }),
];

const removeBorder = EditorView.theme({
    '&.cm-focused': {
        outline: 'none',
    },
});

const keyMap = keymap.of([
    ...defaultKeymap,
    {
        key: 'Ctrl-s',
        run() {
            if (window._onSave) {
                window._onSave();
                return true;
            }
            return false;
        },
    },
    {
        key: 'Ctrl-Shift-Enter',
        run: () => {
            if (window._onRun && window.getContent) {
                const script = window.getContent();
                window._onRun(script);
                return true;
            }
            return false;
        },
    },
]);

const onChange = () => {
    return EditorView.updateListener.of((update) => {
        if (update.docChanged && window._onChange) {
            const value = update.state.doc.toString();
            window._onChange(value);
        }
    });
};

// restrict completion inside strings/comments
const dontComplete = ['String', 'FormatString', 'Comment', 'PropertyName'];

const olcaCompletionSource = (context: CompletionContext) => {
    const { state, pos, explicit } = context;
    const tree = syntaxTree(state);
    const inner = tree.resolveInner(pos, -1);
    if (dontComplete.includes(inner.name)) return null;

    const word = state.sliceDoc(inner.from, inner.to);
    const isWord = /^[\w\xa1-\uffff][\w\d\xa1-\uffff]*$/.test(word);
    if (!isWord && !explicit) return null;

    return {
        from: isWord ? inner.from : pos,
        options: olcaCompletions,
        validFor: /^[\w\xa1-\uffff][\w\d\xa1-\uffff]*$/,
    };
};

const baseExtensions: Extension[] = [
    lineNumbers(),
    highlightSpecialChars(),
    history(),
    foldGutter(),
    drawSelection(),
    dropCursor(),
    indentOnInput(),
    bracketMatching(),
    closeBrackets(),
    highlightActiveLine(),
    highlightActiveLineGutter(),
    highlightSelectionMatches(),
    python(),
    autocompletion({
        override: [
            globalCompletion,
            localCompletionSource,
            olcaCompletionSource,
        ],
    }),
    indent(),
    onChange(),
    keyMap,
    removeBorder,
];

export const extensionsOf = (theme: Theme) => {
    return theme === 'dark'
        ? [...baseExtensions, oneDark]
        : [...baseExtensions, oneLight];
};
