package org.openlca.app.viewers.combo;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ProcessViewer extends AbstractComboViewer<ProcessDescriptor> {

	private static final String[] COLUMN_HEADERS = new String[] { M.Name, M.Location, " " };
	private static final int[] COLUMN_BOUNDS_PERCENTAGES = new int[] { 90, 10, 0 };

	private EntityCache cache;

	public ProcessViewer(Composite parent, EntityCache cache) {
		super(parent);
		setInput(new ProcessDescriptor[0]);
	}

	@Override
	protected int getDisplayColumn() {
		return 2;
	}

	@Override
	protected String[] getColumnHeaders() {
		return COLUMN_HEADERS;
	}

	@Override
	protected int[] getColumnBoundsPercentages() {
		return COLUMN_BOUNDS_PERCENTAGES;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new ProcessLabelProvider();
	}

	@Override
	public Class<ProcessDescriptor> getType() {
		return ProcessDescriptor.class;
	}

	private Location getLocation(ProcessDescriptor process) {
		if (process == null || process.location == null)
			return null;
		return cache.get(Location.class, process.location);
	}

	private class ProcessLabelProvider extends BaseLabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			if (col == 1)
				return Images.get(ModelType.LOCATION);
			ProcessDescriptor process = (ProcessDescriptor) element;
			return Images.get(process);
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			ProcessDescriptor process = (ProcessDescriptor) element;
			switch (columnIndex) {
			case 0:
				return process.name;
			case 1:
				Location location = getLocation(process);
				return location != null ? location.name : null;
			case 2:
				return Labels.getDisplayName(process);
			default:
				return null;
			}
		}

	}
}
