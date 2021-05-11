package org.openlca.app.editors.projects.results;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.ContributionImage;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.results.ProjectResult;
import org.openlca.util.Strings;

interface TableSection extends ITableLabelProvider {

	default ProjectVariant[] variantsOf(ProjectResult result) {
		return result.getVariants()
			.stream()
			.sorted((v1, v2) -> Strings.compare(v1.name, v2.name))
			.toArray(ProjectVariant[]::new);
	}

	default ContributionImage contributionImage(TableViewer table) {
		var image = new ContributionImage();
		table.getControl().addDisposeListener($ -> image.dispose());
		return image;
	}

	void renderOn(Composite body, FormToolkit tk);
}
