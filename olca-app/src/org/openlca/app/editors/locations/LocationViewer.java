package org.openlca.app.editors.locations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.db.Database;
import org.openlca.app.editors.processes.kml.EditorHandler;
import org.openlca.app.editors.processes.kml.KmlUtil;
import org.openlca.app.editors.processes.kml.MapEditor;
import org.openlca.app.util.Error;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Tables;
import org.openlca.app.viewers.BaseLabelProvider;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.usage.IUseSearch;
import org.openlca.core.model.Location;
import org.openlca.core.model.LocationType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.Descriptors;

class LocationViewer extends AbstractTableViewer<Location> {

	private static final String COLUMN_NAME = "Name";
	private static final String COLUMN_CODE = "Code";
	private static final String COLUMN_LATITUDE = "Latitude";
	private static final String COLUMN_LONGITUDE = "Longitude";
	private static final String COLUMN_REF_ID = "Reference Id";
	private static final String COLUMN_KML = "KML";
	private static final String COLUMN_TYPE = "Type";
	private static final String[] COLUMNS = { COLUMN_NAME, COLUMN_CODE,
			COLUMN_LATITUDE, COLUMN_LONGITUDE, COLUMN_REF_ID, COLUMN_KML,
			COLUMN_TYPE };
	private static final int KML_COLUMN = 5;
	private LocationsEditor editor;

	protected LocationViewer(LocationsEditor editor, Composite parent,
			int heightHint) {
		super(parent);
		GridData layoutData = (GridData) getViewer().getTable().getLayoutData();
		layoutData.heightHint = heightHint;
		this.editor = editor;
		getModifySupport().bind(COLUMN_NAME, new NameModifier());
		getModifySupport().bind(COLUMN_CODE, new CodeModifier());
		getModifySupport().bind(COLUMN_LATITUDE, new LatitudeModifier());
		getModifySupport().bind(COLUMN_LONGITUDE, new LongitudeModifier());
		getViewer().getTable().addListener(SWT.MouseDown, new KmlListener());
		setInput(editor.getLocations());
		Tables.bindColumnWidths(getViewer(), 0.3, 0.1, 0.1, 0.1, 0.2, 0.1, 0.1);
		editor.onSaved(() -> setInput(editor.getLocations()));
	}

	@Override
	protected String[] getColumnHeaders() {
		return COLUMNS;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new LabelProvider();
	}

	@OnAdd
	void onAdd() {
		Location location = new Location();
		location.setName("New location");
		location.setRefId(UUID.randomUUID().toString());
		location.setType(LocationType.GLOBAL);
		editor.locationAdded(location);
		setInput(editor.getLocations());
		editor.setDirty(true);
	}

	@OnRemove
	void onRemove() {
		IUseSearch<BaseDescriptor> useSearch = IUseSearch.FACTORY.createFor(
				ModelType.LOCATION, Database.get());
		Set<Location> used = new HashSet<>();
		boolean removedOne = false;
		for (Location location : getAllSelected()) {
			List<BaseDescriptor> usages = useSearch.findUses(Descriptors
					.toDescriptor(location));
			if (!usages.isEmpty()) {
				used.add(location);
				continue;
			}
			editor.locationRemoved(location);
			removedOne = true;
		}
		if (!used.isEmpty())
			showErrorMessage(used);
		if (!removedOne)
			return;
		setInput(editor.getLocations());
		editor.setDirty(true);
	}

	private void showErrorMessage(Set<Location> used) {
		String message = "The following locations could not be removed, because they are used: \n";
		for (Location inUse : used)
			message += inUse.getName() + "\n";
		Error.showBox(message);
	}

	private class LabelProvider extends BaseLabelProvider implements
			ITableLabelProvider, ITableColorProvider {

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof Location))
				return null;
			Location location = (Location) element;
			String column = COLUMNS[columnIndex];
			switch (column) {
			case COLUMN_NAME:
				return location.getName();
			case COLUMN_CODE:
				return location.getCode();
			case COLUMN_LATITUDE:
				return Double.toString(location.getLatitude());
			case COLUMN_LONGITUDE:
				return Double.toString(location.getLongitude());
			case COLUMN_REF_ID:
				return location.getRefId();
			case COLUMN_KML:
				if (location.getKmz() != null)
					return KmlUtil.getDisplayText(location.getKmz())
							+ " (edit)";
				return "(edit)";
			case COLUMN_TYPE:
				return Labels.locationType(location);
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			String column = COLUMNS[columnIndex];
			if (COLUMN_KML.equals(column))
				return new Color(Display.getCurrent(), 0, 0, 128);
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}
	}

	private class NameModifier extends TextCellModifier<Location> {

		@Override
		protected String getText(Location element) {
			return element.getName();
		}

		@Override
		protected void setText(Location element, String text) {
			if (text == null || text.isEmpty())
				return;
			element.setName(text);
			editor.locationChanged(element);
		}

	}

	private class CodeModifier extends TextCellModifier<Location> {

		@Override
		protected String getText(Location element) {
			return element.getCode();
		}

		@Override
		protected void setText(Location element, String text) {
			if (text == null || text.isEmpty())
				return;
			element.setCode(text);
			editor.locationChanged(element);
		}

	}

	private class LatitudeModifier extends TextCellModifier<Location> {

		@Override
		protected String getText(Location element) {
			return Double.toString(element.getLatitude());
		}

		@Override
		protected void setText(Location element, String text) {
			if (text == null || text.isEmpty())
				return;
			try {
				element.setLatitude(Double.parseDouble(text));
				editor.locationChanged(element);
			} catch (NumberFormatException e) {
				// ignore
			}
		}
	}

	private class LongitudeModifier extends TextCellModifier<Location> {

		@Override
		protected String getText(Location element) {
			return Double.toString(element.getLongitude());
		}

		@Override
		protected void setText(Location element, String text) {
			if (text == null || text.isEmpty())
				return;
			try {
				element.setLongitude(Double.parseDouble(text));
				editor.locationChanged(element);
			} catch (NumberFormatException e) {
				// ignore
			}
		}
	}

	private class KmlListener implements Listener {
		public void handleEvent(Event event) {
			Table table = getViewer().getTable();
			Point point = new Point(event.x, event.y);
			int index = table.getTopIndex();
			while (index < table.getItemCount()) {
				TableItem item = table.getItem(index);
				Rectangle rect = item.getBounds(KML_COLUMN);
				if (rect.contains(point)) {
					editKml((Location) item.getData());
					return;
				}
				if (point.x < rect.x)
					break; // not in the right column
				index++;
			}
		}

		private void editKml(Location location) {
			String kml = getKml(location);
			if (kml != null)
				// prepare the KML for the map so that no syntax errors are
				// thrown
				kml = kml.trim().replace("\n", "").replace("\r", "");
			MapEditor.openForEditingOnly(kml, new KmlHandler(location));
		}

		private String getKml(Location location) {
			if (location == null)
				return null;
			if (location.getKmz() == null)
				return null;
			return KmlUtil.toKml(location.getKmz());
		}
	}

	private class KmlHandler implements EditorHandler {

		private Location location;

		private KmlHandler(Location location) {
			this.location = location;
		}

		@Override
		public void contentSaved(MapEditor mapEditor, String kml, boolean overwrite) {
			// ignore overwrite, always true in this case
			location.setKmz(KmlUtil.toKmz(kml));
			editor.locationChanged(location);
			editor.setDirty(true);
			mapEditor.close();
		}

	}

}
