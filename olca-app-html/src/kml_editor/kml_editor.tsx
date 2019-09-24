import React, { useState } from "react";
import { render } from "react-dom";
import { MapComponent } from "./map";
import { TextComponent } from "./text";

let _kml: string | null;

const Page = ({ type }: { type: "map" | "text" }) => {
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

window.openMap = (kml?: string) => {
    _kml = kml ? kml : null;
    render(<Page type="map" />,
        document.getElementById("react-root"));
};

window.openText = (kml?: string) => {
    _kml = kml ? kml : null;
    render(<Page type="text" />,
        document.getElementById("react-root"));
};

window.getKml = () => _kml;
