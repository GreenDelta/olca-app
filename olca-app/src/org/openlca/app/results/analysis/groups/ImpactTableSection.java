package org.openlca.app.results.analysis.groups;

import java.util.List;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.AnalysisGroup;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;

class ImpactTableSection {

	private final ResultEditor editor;
	private final ProductSystem system;
	private final List<AnalysisGroup> groups;

	private TableViewer table;
	private List<ImpactGroupResult> results;

	ImpactTableSection(ResultEditor editor, ProductSystem system) {
		this.editor = editor;
		this.system = system;
		this.groups = system.analysisGroups;
	}

	void render(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, M.ImpactAssessmentResults);
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);

		var headers = new String[3 + groups.size()];
		var widths = new double[3 + groups.size()];
		headers[0] = M.ImpactCategory;
		headers[1] = M.Unit;
		widths[0] = 0.4;
		widths[1] = 0.1;
		headers[2 + groups.size()] = "Rest";
		widths[2 + groups.size()] =  0.5 / (groups.size() + 1);
		for (int i = 0; i < groups.size(); i++) {
			headers[2 + i] = groups.get(i).name;
			widths[2 + i] = 0.5 / (groups.size() + 1);
		}

		table = Tables.createViewer(comp, headers);
		Tables.bindColumnWidths2(table, widths);
		table.setLabelProvider(new LabelProvider(groups));

		var epdExp = Actions.create(
				"Save as EPD",
				Images.descriptor(ModelType.EPD),
				() -> {
					if (results == null)
						return;
					EpdDialog.open(editor, system, results);
				});
		Actions.bind(section, epdExp);
		Actions.bind(table, TableClipboard.onCopySelected(table));
	}

	void setInput(List<ImpactGroupResult> results) {
		this.results = results;
		table.setInput(results);
		table.getTable().getParent().layout();
	}

	private static final class LabelProvider extends ColumnLabelProvider
			implements ITableLabelProvider {

		private final List<AnalysisGroup> groups;
		private final ContributionImage bar;

		private LabelProvider(List<AnalysisGroup> groups) {
			this.groups = groups;
			this.bar = new ContributionImage();
		}

		@Override
		public void dispose() {
			bar.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof ImpactGroupResult r))
				return null;
			if (col == 0)
				return Images.get(r.impact());
			if (col == 1)
				return null;
			double v = getValue(col, r);
			double max = r.max();
			double share = max != 0 ? v / max : 0;
			return bar.get(share);
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ImpactGroupResult r))
				return null;
			return switch (col) {
				case 0 -> Labels.name(r.impact());
				case 1 -> r.impact().referenceUnit;
				default -> Numbers.format(getValue(col, r));
			};
		}

		private double getValue(int col, ImpactGroupResult r) {
			int i = col - 2;
			if (i < 0 || i > groups.size())
				return 0;
			if (i == groups.size())
				return r.restOf(groups);
			var group = groups.get(i);
			var val = r.values().get(group.name);
			return val != null ? val : 0;
		}
	}

}
