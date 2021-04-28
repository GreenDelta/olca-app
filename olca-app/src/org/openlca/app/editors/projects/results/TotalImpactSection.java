package org.openlca.app.editors.projects.results;

import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.ProjectResult;
import org.openlca.util.Strings;

class TotalImpactSection {

	private final ProjectResult result;
	private final List<ProjectVariant> variants;

	private TotalImpactSection(ProjectResult result) {
		this.result = Objects.requireNonNull(result);
		variants = result.getVariants();
		variants.sort((v1, v2) -> Strings.compare(v1.name, v2.name));
	}

	static TotalImpactSection of(ProjectResult result) {
		return new TotalImpactSection(result);
	}

	void renderOn(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, M.ImpactAssessmentResults);
		var comp = UI.sectionClient(section, tk, 1);
		var variants = result.getVariants();
		var columnHeaders = new String[variants.size() + 1];
		columnHeaders[0] = M.ImpactCategories;
		for (int i = 1; i < columnHeaders.length; i++) {
			columnHeaders[i] = variants.get(i - 1).name;
		}
		var table = Tables.createViewer(comp, columnHeaders);
		table.setLabelProvider(new TableLabel());
		table.setInput(result.getImpacts());
	}

	private class TableLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ImpactDescriptor))
				return null;
			var impact = (ImpactDescriptor) obj;
			if (col == 0) {
				var name = Labels.name(impact);
				var unit = impact.referenceUnit;
				return Strings.nullOrEmpty(unit)
					? name
					: name + " [" + unit + "]";
			}
			var idx = col - 1;
			if (idx < 0 || idx >= variants.size())
				return null;
			var variant = variants.get(idx);
			var value = result.getTotalImpactResult(variant, impact);
			return Numbers.format(value);
		}
	}

}
