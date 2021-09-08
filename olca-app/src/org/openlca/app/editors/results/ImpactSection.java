package org.openlca.app.editors.results;

import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ResultImpact;
import org.openlca.util.Strings;

public class ImpactSection {

	private final ResultEditor editor;

	ImpactSection(ResultEditor editor) {
		this.editor = editor;
	}

	void render(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, M.ImpactAssessmentResults);
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);
		var table = Tables.createViewer(
			comp, M.ImpactCategory, M.Amount, M.Unit);
		table.setLabelProvider(new ImpactLabel());
		Tables.bindColumnWidths(table, 0.35, 0.35, 0.3);
		var impacts = editor.getModel().impacts.stream()
			.sorted((i1, i2) -> Strings.compare(
				Labels.name(i1.indicator), Labels.name(i2.indicator))
			).collect(Collectors.toList());
		table.setInput(impacts);
	}

	private static class ImpactLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return col != 0 || !(obj instanceof ResultImpact)
				? null
				: Images.get(ModelType.IMPACT_CATEGORY);
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ResultImpact))
				return null;
			var impact = (ResultImpact) obj;
			return switch (col) {
				case 0 -> Labels.name(impact.indicator);
				case 1 -> Numbers.format(impact.amount);
				case 2 -> impact.indicator == null
					? null
					: impact.indicator.referenceUnit;
				default -> null;
			};
		}
	}

}
