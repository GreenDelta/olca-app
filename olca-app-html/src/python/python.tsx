import React, { useEffect, useRef } from "react";
import { render } from "react-dom";
import { EditorView } from "@codemirror/view";
import { EditorState } from "@codemirror/state";
import { extensionsOf, Theme } from "./codemirror-extensions";

/// Extend the Window object; these methods are used for the communication with
/// the Java side.
declare global {
  interface Window {

    /// Returns the current editor content, the script.
    getContent?: () => string;

    /// Set the editor content.
    setContent?: (code: string) => void;

    /// A listener that is called when the content changed.
    _onChange?: (code: string) => void;

    /// A listener that is called when the script should be executed.
    _onRun?: (script: string) => void;

    /// A listener that is called when the script should be saved.
    _onSave?: () => void;
  }
}

/// `setContent` could be called before React is ready. Thus, we register
/// the content methods temporarily here to capture the initial content. They
/// are then replaced later when the EditorView was initialized.
let _content: string | null = null;
window.setContent = (code: string) => {
  _content = code;
};
window.getContent = () => _content || "";

const initBrowserAPI = (view: EditorView) => {
  const initialContent = window.getContent ? window.getContent() : null;
  window.getContent = () => view.state.doc.toString();
  window.setContent = (code: string) =>
    view.dispatch({
      changes: { from: 0, to: view.state.doc.length, insert: code },
    });
  if (initialContent) {
    window.setContent(initialContent);
  }
};


const PythonEditor = ({ theme }: { theme: Theme }) => {
  const editorRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!editorRef.current) return;

    const view = new EditorView({
      state: EditorState.create({
        extensions: extensionsOf(theme),
      }),
      parent: editorRef.current,
    });

    initBrowserAPI(view);

    return () => view.destroy();
  }, []);

  return <div ref={editorRef} />;
};

const container = document.getElementById("editor");
const theme = container?.getAttribute("data-theme");
const editor = <PythonEditor theme={theme === "dark" ? "dark" : "light"} />
render(editor, document.getElementById("editor"));
