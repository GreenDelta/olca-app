package org.openlca.app.viewers.combo;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.LocationDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ProcessCombo extends AbstractComboViewer<ProcessDescriptor> {

	public ProcessCombo(Composite parent) {
		super(parent);
		setInput(new ProcessDescriptor[0]);
	}

	@Override
	protected int getDisplayColumn() {
		return 2;
	}

	@Override
	protected String[] getColumnHeaders() {
		return new String[] { M.Name, M.Location, " " };
	}

	@Override
	protected int[] getColumnBoundsPercentages() {
		return new int[] { 90, 10, 0 };
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new ProcessLabelProvider();
	}

	@Override
	public Class<ProcessDescriptor> getType() {
		return ProcessDescriptor.class;
	}

	private static class ProcessLabelProvider extends BaseLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof ProcessDescriptor p))
				return null;
			if (col == 1)
				return Images.get(ModelType.LOCATION);
			return Images.get(p);
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ProcessDescriptor p))
				return null;
			switch (col) {
			case 0:
				return p.name;
			case 1:
				if (p.location == null)
					return null;
				EntityCache cache = Cache.getEntityCache();
				if (cache == null)
					return "?";
				LocationDescriptor loc = cache.get(
						LocationDescriptor.class, p.location);
				return loc != null ? loc.name : null;
			case 2:
				return Labels.name(p);
			default:
				return null;
			}
		}
	}
}
