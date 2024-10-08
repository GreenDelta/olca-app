package org.openlca.app.results.analysis.groups;

import java.util.List;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.descriptors.ImpactDescriptor;

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
		table.setLabelProvider(new LabelProvider());
		table.setInput(Item.allOf(editor));
	}

	private record Item(ImpactDescriptor impact) {

		static List<Item> allOf(ResultEditor editor) {
			return editor.items()
					.impacts()
					.stream()
					.map(Item::new)
					.toList();
		}
	}

	private static final class LabelProvider extends ColumnLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Item item))
				return null;
			return col == 0
					? Images.get(item.impact)
					: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Item item))
				return null;
			return switch (col) {
				case 0 -> Labels.name(item.impact);
				case 1 -> item.impact.referenceUnit;
				default -> Numbers.format(0);
			};
		}
	}
}
