import React, { useState } from "react";
import { render } from "react-dom";
import { MapComponent } from "./map";
import { TextComponent } from "./text";

type PageProps = {
    kml: string;
    type: "map" | "text";
};

const Page = (props: PageProps) => {
    const [kml, setKml] = useState<string | null>(props.kml);
    const component = props.type === "map"
        ? <MapComponent kml={kml} onChange={(newKml) => setKml(newKml)} />
        : <TextComponent kml={kml} onChange={(newKml) => setKml(newKml)} />;
    return component;
};

declare global {
    interface Window {
        onSave: (kml: string | null) => void;
        openMap: (kml?: string) => void;
        openText: (kml?: string) => void;
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
