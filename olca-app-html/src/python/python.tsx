import React, { useEffect, useRef } from "react";
import { render } from "react-dom";
import { EditorView } from "@codemirror/view";
import { EditorState } from "@codemirror/state";
import { extensionsOf, Theme } from "./codemirror-extensions";
import { initBrowserAPI } from "./browser-api";

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
