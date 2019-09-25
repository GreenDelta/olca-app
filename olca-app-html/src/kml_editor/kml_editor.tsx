import React from "react";
import ReactDOM from "react-dom";
import { MapComponent } from "./map";
import { TextComponent } from "./text";

type Tool = "map" | "text";
let _kml: string | null;

const Page = ({ type }: { type: Tool }) => {
    const elem = type === "map"
        ? <MapComponent kml={_kml} onChange={(newKml) => _kml = newKml} />
        : <TextComponent kml={_kml} onChange={(newKml) => _kml = newKml} />;
    return elem;
};

declare global {
    interface Window {
        openMap: (kml?: string) => void;
        openText: (kml?: string) => void;
        getKml: () => string | null;
    }
}

window.openMap = (kml?: string) => render("map", kml);
window.openText = (kml?: string) => render("text", kml);

function render(tool: Tool, kml?: string) {
    _kml = kml ? kml : null;
    // KML handling between map and editor instances
    // and external calls is hard, thus we force 
    // these tool to mount as new instances here
    const elem = document.getElementById("react-root");
    ReactDOM.unmountComponentAtNode(elem)
    ReactDOM.render(<Page type={tool} />, elem);
}

window.getKml = () => _kml;
