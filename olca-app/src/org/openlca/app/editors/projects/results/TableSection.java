package org.openlca.app.editors.projects.results;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.ContributionImage;

interface TableSection extends ITableLabelProvider {

	default ContributionImage contributionImage(TableViewer table) {
		var image = new ContributionImage();
		table.getControl().addDisposeListener($ -> image.dispose());
		return image;
	}

	void renderOn(Composite body, FormToolkit tk);
}
