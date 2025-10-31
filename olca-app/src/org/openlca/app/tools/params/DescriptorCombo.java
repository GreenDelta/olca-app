package org.openlca.app.tools.params;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Strings;

class DescriptorCombo {

	private DescriptorCombo() {
	}

	static TableComboViewer of(
			Composite comp, FormToolkit tk, List<? extends Descriptor> descriptors
	) {
		var combo = UI.tableCombo(comp, tk, SWT.BORDER | SWT.READ_ONLY);
		UI.fillHorizontal(combo);
		var viewer = new TableComboViewer(combo);
		viewer.setLabelProvider(new ComboLabel());
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object o1, Object o2) {
				if (!(o1 instanceof Descriptor d1) || !(o2 instanceof Descriptor d2))
					return super.compare(viewer, o1, o2);
				var n1 = Labels.name(d1);
				var n2 = Labels.name(d2);
				return Strings.compareIgnoreCase(n1, n2);
			}
		});
		viewer.setInput(descriptors);
		if (!descriptors.isEmpty()) {
			viewer.setSelection(new StructuredSelection(descriptors.get(0)));
		}
		return viewer;
	}

	private static class ComboLabel extends BaseLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return col == 0 && obj instanceof Descriptor d
					? Images.get(d)
					: null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			return col == 0 && obj instanceof Descriptor d
					? Labels.name(d)
					: null;
		}
	}
}
