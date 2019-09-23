import React, { useState } from "react";
import { render } from "react-dom";
import { MapComponent } from "./map";
import { TextComponent } from "./text";

const Page = ({ initialKml }: { initialKml: string | null }) => {

    const [kml, setKml] = useState<string | null>(initialKml);
    const [showMap, setShowMap] = useState(true);

    const doSave = () => {
        if (window.onSave) {
            window.onSave(kml);
        }
    };

    return (
        <div style={{ margin: 15 }}>
            <div className="row">
                <div className="column column-25">
                    <button className="button button-blue button-outline"
                        onClick={() => setShowMap(true)}>
                        Map
                    </button>
                    <button className="button button-blue button-outline"
                        onClick={() => setShowMap(false)}>
                        Text
                    </button>
                </div>
                <div className="column column-25 column-offset-50"
                    style={{ textAlign: "right" }}>
                    <button className="button button-blue button-outline"
                        onClick={() => setKml(null)}>
                        Clear
                    </button>
                    <button className="button button-blue"
                        onClick={() => doSave()}>
                        Save
                    </button>
                </div>
            </div>
            {showMap
                ? <MapComponent kml={kml} onChange={(newKml) => setKml(newKml)} />
                : <TextComponent kml={kml} onChange={(newKml) => setKml(newKml)} />}
        </div>
    );
};

declare global {
    interface Window {
        onSave: (kml: string | null) => void;
        openEditor: (kml?: string) => void;
    }
}

function openEditor(kml?: string) {
    const initialKml = kml ? kml : null;
    render(
        <Page initialKml={initialKml} />,
        document.getElementById("react-root"));
}
window.openEditor = openEditor;
