package org.openlca.app.editors.lcia.geo;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.components.mapview.MapDialog;
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
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.util.Strings;

class GeoParamSection {

	private final GeoPage page;
	private TableViewer table;

	GeoParamSection(GeoPage page) {
		this.page = page;
	}

	void drawOn(Composite body, FormToolkit tk) {

		// create the section
		Section section = UI.section(body, tk, "GeoJSON Parameters");
		Composite comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);

		// create the table
		table = Tables.createViewer(comp,
				"Parameter",
				"Identifier",
				"Default value",
				"Range",
				"Aggregation type");
		table.setLabelProvider(new Label());
		Tables.bindColumnWidths(
				table, 0.2, 0.2, 0.2, 0.2, 0.2);
		bindModifiers();

		// bind the "open map" action
		Action openMap = Actions.create("Open map",
				Icon.MAP.descriptor(), this::showMap);
		Actions.bind(table, openMap);
		Actions.bind(section, openMap);

		update();
	}

	private void showMap() {
		Setup setup = page.setup;
		if (setup == null || setup.file == null)
			return;
		FeatureCollection coll = setup.getFeatures();
		if (coll == null)
			return;
		GeoParam gp = Viewers.getFirstSelected(table);
		String param = gp != null ? gp.name : null;
		MapDialog.show(setup.file, map -> {
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
		ModifySupport<GeoParam> ms = new ModifySupport<>(table);
		ms.bind("Aggregation type", new AggTypeCell());
		ms.bind("Default value", new TextCellModifier<GeoParam>() {
			@Override
			protected String getText(GeoParam param) {
				return param == null ? ""
						: Double.toString(param.defaultValue);
			}

			@Override
			protected void setText(GeoParam param, String s) {
				if (param == null || Strings.nullOrEmpty(s))
					return;
				try {
					param.defaultValue = Double.parseDouble(s);
					table.refresh();
				} catch (Exception e) {
					MsgBox.error("Not a number",
							"The string " + s + " is not a valid number");
				}
			}
		});
	}

	void update() {
		if (page.setup == null)
			return;
		table.setInput(page.setup.params);
	}

	private class AggTypeCell
			extends ComboBoxCellModifier<GeoParam, GeoAggType> {

		@Override
		protected GeoAggType[] getItems(GeoParam param) {
			return GeoAggType.values();
		}

		@Override
		protected GeoAggType getItem(GeoParam param) {
			return param == null || param.aggType == null
					? GeoAggType.WEIGHTED_AVERAGE
					: param.aggType;
		}

		@Override
		protected String getText(GeoAggType aggType) {
			return aggType == null
					? GeoAggType.WEIGHTED_AVERAGE.toString()
					: aggType.toString();
		}

		@Override
		protected void setItem(GeoParam param, GeoAggType aggType) {
			if (param == null)
				return;
			param.aggType = aggType;
		}
	}

	private class Label extends LabelProvider
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
			if (!(obj instanceof GeoParam))
				return null;
			GeoParam p = (GeoParam) obj;
			switch (col) {
			case 0:
				return p.name;
			case 1:
				return p.identifier;
			case 2:
				return Numbers.format(p.defaultValue);
			case 3:
				return "[" + Numbers.format(p.min)
						+ ", " + Numbers.format(p.max) + "]";
			case 4:
				return p.aggType == null
						? GeoAggType.WEIGHTED_AVERAGE.toString()
						: p.aggType.toString();
			default:
				return null;
			}
		}
	}

}
