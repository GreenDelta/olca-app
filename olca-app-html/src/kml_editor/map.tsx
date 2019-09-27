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
    cancel: (e: KeyboardEvent) => void;
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
                        style={{ width: 150, margin: "10px 0px" }}
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

    componentDidMount() {
        const raster = new Tile({
            source: new OSM(),
        });
        this.features = new VectorSource();
        this.initKml(this.props.kml);
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
                    color: "rgba(0, 0, 81, 0.3)",
                }),
                stroke: new Stroke({
                    color: "#000051",
                    width: 2,
                }),
                image: new CircleStyle({
                    radius: 7,
                    fill: new Fill({
                        color: "#000051",
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

        // cancel drawing when `Escape` is pressed
        this.cancel = (e) => {
            if (e.code !== "Escape") {
                return;
            }
            if (!this.map || !this.draw) {
                return;
            }
            this.map.removeInteraction(this.draw);
            this.map.addInteraction(this.draw);
        };
        document.addEventListener("keydown", this.cancel);
    }

    componentWillUnmount() {
        if (this.cancel) {
            document.removeEventListener("keydown", this.cancel);
            this.cancel = null;
        }
        if (this.map) {
            this.map.dispose();
            this.map = null;
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

    /**
     * This should be called before after the feature vector was created but
     * before listeners are added to the vector.
     */
    private initKml(kml: string) {
        this.kml = kml;
        if (!this.features) {
            return;
        }
        if (!kml) {
            return;
        }
        try {
            const features = this.format.readFeatures(kml, {
                dataProjection: "EPSG:4326",
                featureProjection: "EPSG:3857",
            });
            features.forEach((feature) => this.features.addFeature(feature));
        } catch (e) {
            /* tslint:disable */
            if (console && console.log) {
                console.log("failed to parse KML features", e);
            }
            /* tslint:enable */
        }
    }
}
