import React, { useState } from "react";
import { render } from "react-dom";
import { MapComponent } from "./map";
import { TextComponent } from "./text";

type PageProps = {
    kml: string;
    type: "map" | "text";
};

let kml: string | null;

const Page = (props: PageProps) => {
    const elem = props.type === "map"
        ? <MapComponent kml={kml} onChange={(newKml) => kml = newKml} />
        : <TextComponent kml={kml} onChange={(newKml) => kml = newKml} />;
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
    render(
        <Page type="map" kml={kml ? kml : null} />,
        document.getElementById("react-root"));
};

window.openText = (kml?: string) => {
    render(
        <Page type="text" kml={kml ? kml : null} />,
        document.getElementById("react-root"));
};

window.getKml = () => kml;
