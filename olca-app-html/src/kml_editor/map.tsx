import React, { Component } from "react";
import { Map, View } from "ol";
import { Draw, Modify, Snap } from "ol/interaction";
import { Tile, Vector as VectorLayer } from "ol/layer";
import { OSM, Vector as VectorSource } from "ol/source";
import { Style, Fill, Stroke } from "ol/style";
import CircleStyle from "ol/style/Circle";
import GeometryType from "ol/geom/GeometryType";
import { KML } from "ol/format";

type Props = {
    kml?: string;
    onChange: (kml: string) => void;
};

type State = {
    featureType: GeometryType;
};

export class MapComponent extends Component<Props, State> {

    map: Map;
    features: VectorSource;
    draw: Draw;
    snap: Snap;
    kml: string;
    format = new KML({
        extractStyles: false,
        writeStyles: false,
    });

    constructor(props: Props) {
        super(props);
        this.state = {
            featureType: GeometryType.POLYGON,
        };
        this.kml = props.kml;
    }

    render() {
        return (
            <>
                <div>
                    <select id="type" value={this.state.featureType}
                        style={{ width: 150, margin: "10px 0px", fontSize: "1em" }}
                        onChange={(e) => this.setType(
                            e.target.value as GeometryType)}>
                        <option value="Point">Point</option>
                        <option value="LineString">Line string</option>
                        <option value="Polygon">Polygon</option>
                        <option value="MultiPolygon">Multi-polygon</option>
                    </select>
                </div>
                <div id="map" />
            </>
        );
    }

    componentDidUpdate() {
        if (this.props.kml !== this.kml) {
            this.setKml(this.props.kml);
        }
    }

    componentDidMount() {
        const raster = new Tile({
            source: new OSM(),
        });
        this.features = new VectorSource();
        this.setKml(this.props.kml);
        this.features.on("change", () => {
            this.kml = this.format.writeFeatures(
                this.features.getFeatures(), {
                dataProjection: "EPSG:4326",
                featureProjection: "EPSG:3857",
            });
            this.props.onChange(this.kml);
        });

        const vectorLayer = new VectorLayer({
            source: this.features,
            style: new Style({
                fill: new Fill({
                    color: "rgba(255, 255, 255, 0.2)",
                }),
                stroke: new Stroke({
                    color: "#ffcc33",
                    width: 2,
                }),
                image: new CircleStyle({
                    radius: 7,
                    fill: new Fill({
                        color: "#ffcc33",
                    }),
                }),
            }),
        });

        this.map = new Map({
            layers: [raster, vectorLayer],
            target: "map",
            view: new View({
                center: [0, 0],
                zoom: 2,
            }),
        });
        this.map.addInteraction(new Modify({
            source: this.features,
        }));
        this.setType(GeometryType.POLYGON);
    }

    componentWillUnmount() {
        if (this.map) {
            this.map.dispose();
        }
    }

    private setType(featureType: GeometryType) {
        if (featureType !== this.state.featureType) {
            this.setState({ featureType });
        }
        if (!this.map) {
            return;
        }
        if (this.draw) {
            this.map.removeInteraction(this.draw);
            this.draw.dispose();
        }
        if (this.snap) {
            this.map.removeInteraction(this.snap);
            this.snap.dispose();
        }
        this.draw = new Draw({
            source: this.features,
            type: featureType,
        });
        this.map.addInteraction(this.draw);
        this.snap = new Snap({
            source: this.features,
        });
        this.map.addInteraction(this.snap);
    }

    private setKml(kml: string) {
        this.kml = kml;
        if (!this.features) {
            return;
        }
        this.features.clear();
        if (!kml) {
            return;
        }
        const features = this.format.readFeatures(kml, {
            dataProjection: "EPSG:4326",
            featureProjection: "EPSG:3857",
        });
        // we need to un-register the event listeners in order to not
        // run into infinite loops here
        const fns = this.features.getListeners("change");
        if (fns) {
            fns.forEach((f) => this.features.removeEventListener("change", f));
        }
        features.forEach((feature) => this.features.addFeature(feature));
        if (fns) {
            fns.forEach((f) => this.features.addEventListener("change", f));
        }
    }
}
