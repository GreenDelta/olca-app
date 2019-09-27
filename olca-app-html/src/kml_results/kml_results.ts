import { Map, View } from "ol";
import { KML } from "ol/format";
import { Tile, Vector as VectorLayer } from "ol/layer";
import { OSM, Vector } from "ol/source";
import { Fill, Stroke, Style } from "ol/style";

let _refAmount: number;
let _kml: KML;
let _features: Vector;

document.addEventListener("DOMContentLoaded", () => {
    _kml = new KML({ extractStyles: false });
    const tileLayer = new Tile({
        source: new OSM(),
    });
    _features = new Vector({ features: [] });
    const vectorLayer = new VectorLayer({
        source: _features,
    });
    new Map({
        target: "map", // ID of the div
        layers: [tileLayer, vectorLayer],
        view: new View({
            center: [0, 0],
            zoom: 2,
        }),
    });
});

/**
 * This function needs to be called when features are added in
 * a sequence of `addFeature` calls.
 */
function init(refAmount: number) {
    _refAmount = refAmount;
    _features.clear();
}

type FeatureData = {
    amount: number;
    kml: string;
};

/**
 * Adds the given features. If the reference amount was not yet
 * initialized it is calculated from the amount values of the
 * given features.
 */
function addFeatures(data: FeatureData[]) {
    if (!data) {
        return;
    }
    if (!_refAmount) {
        for (const feature of data) {
            const amount = Math.abs(feature.amount);
            if (!_refAmount) {
                _refAmount = amount;
            } else {
                _refAmount = Math.max(_refAmount, amount);
            }
        }
    }
    for (const feature of data) {
        addFeature(feature);
    }
}

function addFeature({ amount, kml }: FeatureData) {
    if (!kml || !amount) {
        return;
    }
    if (!_refAmount) {
        _refAmount = Math.abs(amount);
    }
    const feature = _kml.readFeature(kml, {
        dataProjection: "EPSG:4326",
        featureProjection: "EPSG:3857",
    });
    feature.setStyle(new Style({
        stroke: new Stroke({
            color: color(amount, _refAmount, 0.7),
            width: 2,
        }),
        fill: new Fill({
            color: color(amount, _refAmount, 0.3),
        }),
    }));
    _features.addFeature(feature);
}

function color(amount: number, refAmount: number, alpha?: number): string {
    if (!refAmount || !amount) {
        return "rgba(0, 0, 0, 0)";
    }
    let share = Math.abs(amount) / refAmount;
    if (share > 1) {
        share = 1;
    }
    let r: number, g: number, b: number;
    if (amount >= 0) {
        r = Math.round(255 * share);
        g = Math.round(Math.sqrt(r * (255 - r)));
        b = Math.round(255 - r);
    } else {
        g = Math.round(255 * share);
        r = Math.round(Math.sqrt(g * (255 - g)));
        b = Math.round(255 - g);
    }
    if (!alpha) {
        return `rgb(${r}, ${g}, ${b})`;
    } else {
        return `rgba(${r}, ${g}, ${b}, ${alpha})`;
    }
}

declare global {
    interface Window {
        init: any;
        addFeature: any;
        addFeatures: any;
    }
}
window.init = init;
window.addFeature = addFeature;
window.addFeatures = addFeatures;
