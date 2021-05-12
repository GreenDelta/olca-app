package org.openlca.app.editors.projects.results;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.core.results.ProjectResult;

class ContributionSection extends LabelProvider implements TableSection {

	private final ProjectResult result;

	private ContributionSection(ProjectResult result) {
		this.result = result;
	}

	static ContributionSection of(ProjectResult result) {
		return new ContributionSection(result);
	}

	@Override
	public void renderOn(Composite body, FormToolkit tk) {
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		return null;
	}
}
