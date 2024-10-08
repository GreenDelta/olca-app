package org.openlca.app.results.analysis.groups;

import java.util.List;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;

public class AnalysisGroupsPage extends FormPage {

	private final ResultEditor editor;
	private final List<AnalysisGroup> groups;

	public AnalysisGroupsPage(ResultEditor editor, List<AnalysisGroup> groups) {
		super(editor, "AnalysisGroupsPage", "Analysis groups");
		this.editor = editor;
		this.groups = groups;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.header(mform,
				Labels.name(editor.setup().target()),
				Icon.ANALYSIS_RESULT.get());
		var tk = mform.getToolkit();
		var body = UI.body(form, tk);

		var headers = new String[2 + groups.size()];
		var widths = new double[2 + groups.size()];
		headers[0] = M.ImpactCategory;
		headers[1] = M.Unit;
		widths[0] = 0.4;
		widths[1] = 0.1;
		for (int i = 0; i < groups.size(); i++) {
			headers[2 + i] = groups.get(i).name();
			widths[2 + i] = 0.5 / groups.size();
		}

		var table = Tables.createViewer(body, headers);
		Tables.bindColumnWidths(table, widths);
		table.setLabelProvider(new LabelProvider(groups));

		var results = App.exec(
				"Calculate group results...",
				() -> AnalysisGroupResult.calculate(editor, groups));
		table.setInput(results);
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
			if (!(obj instanceof AnalysisGroupResult r))
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
			if (!(obj instanceof AnalysisGroupResult r))
				return null;
			return switch (col) {
				case 0 -> Labels.name(r.impact());
				case 1 -> r.impact().referenceUnit;
				default -> Numbers.format(getValue(col, r));
			};
		}

		private double getValue(int col, AnalysisGroupResult r) {
			int i = col - 2;
			if (i < 0 || i >= groups.size())
				return 0;
			var group = groups.get(i);
			var val = r.values().get(group.name());
			return val != null ? val : 0;
		}
	}
}
