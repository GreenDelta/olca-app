package org.openlca.app.viewers.tables;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.core.model.descriptors.Descriptor;

public class DescriptorViewer extends AbstractTableViewer<Descriptor> {

	protected DescriptorViewer(Composite parent) {
		super(parent);
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new DescriptorLabelProvider();
	}

	@Override
	protected String[] getColumnHeaders() {
		return new String[] { M.Name, M.Description };
	}

	private class DescriptorLabelProvider
			extends BaseLabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Descriptor))
				return null;
			if (col != 0)
				return null;
			return Images.get((Descriptor) obj);
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Descriptor))
				return null;
			var d = (Descriptor) obj;
			switch (col) {
			case 0:
				return Labels.name(d);
			case 1:
				return d.description;
			}
			return null;
		}

	}

}
