import { EditorView } from '@codemirror/view';

/**
 * Extension of the Window type:
 * + `_onChange`: a handler that is called when the Python code is changed;
 * + `getContent`: returns the current content;
 * + `setContent`: sets the content;
 * + `_onSave`: a handler that is called for saving;
 * + `_onRun`: a handler that is called for running the script;
 */
declare global {
    interface Window {
        _onChange?: (code: string) => void;
        getContent?: () => string;
        setContent?: (code: string) => void;
        _onSave?: () => void;
        _onRun?: (script: string) => void;
    }
}

export const initBrowserAPI = (view: EditorView) => {
    window.getContent = () => view.state.doc.toString();
    window.setContent = (code: string) =>
        view.dispatch({
            changes: { from: 0, to: view.state.doc.length, insert: code },
        });
};
