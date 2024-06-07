package org.openlca.app.editors.lcia.geo;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.components.mapview.MapDialog;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.app.viewers.tables.modify.TextCellModifier;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.geo.calc.IntersectionCalculator;
import org.openlca.geo.calc.IntersectionShare;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.GeoJSON;
import org.openlca.geo.lcia.GeoAggregation;
import org.openlca.geo.lcia.GeoProperty;
import org.openlca.util.Strings;

class GeoPropertySection {

	private final GeoPage page;
	private TableViewer table;

	GeoPropertySection(GeoPage page) {
		this.page = page;
	}

	void drawOn(Composite body, FormToolkit tk) {

		// create the section
		Section section = UI.section(body, tk, M.GeoJsonParameters);
		Composite comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);

		// create the table
		table = Tables.createViewer(comp,
				M.Parameter,
				M.Identifier,
				M.DefaultValue,
				M.Range,
				M.AggregationType);
		table.setLabelProvider(new Label());
		Tables.bindColumnWidths(
				table, 0.2, 0.2, 0.2, 0.2, 0.2);
		bindModifiers();

		// bind the "open map" action
		var openMap = Actions.create(M.OpenMap,
				Icon.MAP.descriptor(), this::showMap);
		var calcIntersections = Actions.create(
				M.TestIntersections, this::showIntersections);
		Actions.bind(table, openMap, calcIntersections);
		Actions.bind(section, openMap);

		update();
	}

	private void showMap() {
		var setup = page.setup;
		if (setup == null || setup.features.isEmpty())
			return;
		GeoProperty gp = Viewers.getFirstSelected(table);
		var param = gp != null ? gp.name : null;
		var title = param != null ? param : M.Features;
		MapDialog.show(title, map -> {
			if (param == null) {
				map.addLayer(setup.features).center();
			} else {
				map.addLayer(setup.features)
						.fillScale(param)
						.center();
			}
		});
	}

	private void showIntersections() {
		var setup = page.setup;
		if (setup == null || setup.features.isEmpty())
			return;
		var d = ModelSelector.select(ModelType.LOCATION);
		if (d == null)
			return;
		var loc = Database.get().get(Location.class, d.id);
		var geoData = GeoJSON.unpack(loc.geodata);
		if (geoData == null || geoData.isEmpty()) {
			MsgBox.info(M.NoGeographicData, M.NoGeographicDataInfo);
			return;
		}

		var shares = App.exec(M.CalculateIntersectionsDots,
				() -> IntersectionCalculator.on(setup.features).shares(loc));
		var coll = new FeatureCollection();
		shares.stream()
				.map(IntersectionShare::intersection)
				.forEach(coll.features::add);
		if (coll.isEmpty()) {
			MsgBox.info(M.NoIntersections, M.NoIntersectionsInfo);
			return;
		}

		GeoProperty gp = Viewers.getFirstSelected(table);
		var param = gp != null ? gp.name : null;
		MapDialog.show(M.Intersections, map -> {
			if (param == null) {
				map.addLayer(coll).center();
			} else {
				map.addLayer(coll)
						.fillScale(param)
						.center();
			}
		});
	}

	private void bindModifiers() {
		ModifySupport<GeoProperty> ms = new ModifySupport<>(table);
		ms.bind(M.AggregationType, new AggTypeCell());
		ms.bind(M.DefaultValue, new TextCellModifier<>() {
			@Override
			protected String getText(GeoProperty param) {
				return param == null ? ""
						: Double.toString(param.defaultValue);
			}

			@Override
			protected void setText(GeoProperty param, String s) {
				if (param == null || Strings.nullOrEmpty(s))
					return;
				try {
					param.defaultValue = Double.parseDouble(s);
					table.refresh();
				} catch (Exception e) {
					MsgBox.error(M.NotANumber, M.NotValidNumber + " - " + s);
				}
			}
		});
	}

	void update() {
		if (page.setup == null)
			return;
		table.setInput(page.setup.properties);
	}

	private static class AggTypeCell
			extends ComboBoxCellModifier<GeoProperty, GeoAggregation> {

		@Override
		protected GeoAggregation[] getItems(GeoProperty param) {
			return GeoAggregation.values();
		}

		@Override
		protected GeoAggregation getItem(GeoProperty param) {
			return param == null || param.aggregation == null
					? GeoAggregation.WEIGHTED_AVERAGE
					: param.aggregation;
		}

		@Override
		protected String getText(GeoAggregation aggType) {
			return aggType == null
					? GeoAggregation.WEIGHTED_AVERAGE.toString()
					: aggType.toString();
		}

		@Override
		protected void setItem(GeoProperty param, GeoAggregation aggType) {
			if (param == null)
				return;
			param.aggregation = aggType;
		}
	}

	private static class Label extends LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col == 1)
				return Icon.FORMULA.get();
			if (col == 2 || col == 4)
				return Icon.EDIT.get();
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof GeoProperty p))
				return null;
			return switch (col) {
				case 0 -> p.name;
				case 1 -> p.identifier;
				case 2 -> Numbers.format(p.defaultValue);
				case 3 -> "[" + Numbers.format(p.min)
						+ ", " + Numbers.format(p.max) + "]";
				case 4 -> p.aggregation == null
						? GeoAggregation.WEIGHTED_AVERAGE.toString()
						: p.aggregation.toString();
				default -> null;
			};
		}
	}
}
