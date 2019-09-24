import React, { Component } from "react";
import CodeMirror, { Editor } from "codemirror";

type Props = {
    kml?: string;
    onChange: (kml: string) => void;
};

export class TextComponent extends Component<Props, {}> {

    code: Editor;
    kml: string;

    constructor(props: Props) {
        super(props);
        this.kml = props.kml ? props.kml : "";
    }

    render() {
        return (
            <div id="editor-text" style={{ width: "100%", height: "100%" }}>
            </div>
        );
    }

    componentDidMount() {
        this.code = CodeMirror(document.getElementById("editor-text"), {
            mode: "xml",
            lineWrapping: true,
            lineNumbers: true,
        });
        if (this.kml) {
            this.code.setValue(this.kml);
        }
        this.code.on("change", () => {
            this.kml = this.code.getValue();
            this.props.onChange(this.kml);
        });
    }

    shouldComponentUpdate(props: Props): boolean {
        if (props.kml !== this.kml) {
            this.kml = props.kml ? props.kml : "";
            this.code.setValue(this.kml);
        }
        return false;
    }

}
