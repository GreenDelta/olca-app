package org.openlca.core.application.preferencepages;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.Messages;
import org.openlca.core.application.wizards.DeleteWizard;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Location;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.BaseNameSorter;
import org.openlca.ui.Question;
import org.openlca.ui.UI;
import org.openlca.ui.Viewers;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private AddLocationAction addLocationAction;
	private IDatabase database;
	private final List<Location> locations = new ArrayList<>();
	private TableViewer locationViewer;

	private final String CODE = Messages.Code;
	private final String DESCRIPTION = Messages.Description;
	private final String LATITUDE = Messages.Latitude;
	private final String LONGITUDE = Messages.Longitude;
	private final String NAME = Messages.Name;

	private final String[] PROPERTIES = new String[] { NAME, DESCRIPTION, CODE,
			LATITUDE, LONGITUDE };

	private RemoveLocationAction removeLocationAction;

	private void initListeners() {
		locationViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {

					@Override
					public void selectionChanged(
							final SelectionChangedEvent event) {
						if (event.getSelection().isEmpty()) {
							removeLocationAction.setEnabled(false);
						} else {
							removeLocationAction.setEnabled(true);
						}
					}
				});
	}

	@Override
	protected Control createContents(final Composite parent) {
		final Composite body = new Composite(parent, SWT.NONE);
		body.setLayout(new GridLayout());

		// create section for the location viewer
		final Section section = new Section(body, ExpandableComposite.NO_TITLE);
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		final Composite composite = new Composite(section, SWT.NONE);

		final GridLayout layout = new GridLayout();
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		section.setClient(composite);

		// create table viewer for displaying locations
		locationViewer = new TableViewer(composite, SWT.BORDER
				| SWT.FULL_SELECTION | SWT.NO_REDRAW_RESIZE | SWT.V_SCROLL);
		locationViewer.setContentProvider(new ArrayContentProvider());
		locationViewer.setSorter(new BaseNameSorter());
		locationViewer.setLabelProvider(new LocationLabelProvider());
		locationViewer.getTable().setEnabled(false);

		// configure table
		final Table table = locationViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = parent.getParent()
				.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		table.setLayoutData(gd);

		for (String p : PROPERTIES) {
			TableColumn c = new TableColumn(table, SWT.NULL);
			c.setText(p);
		}
		for (TableColumn c : table.getColumns()) {
			if (c.getText().equals(NAME))
				c.setWidth(150);
			else
				c.pack();
		}

		// create table's tool bar
		final ToolBarManager locationBar = new ToolBarManager();
		addLocationAction = new AddLocationAction();
		removeLocationAction = new RemoveLocationAction();
		locationBar.add(addLocationAction);
		locationBar.add(removeLocationAction);

		// create table's popup menu
		final MenuManager locationMenu = new MenuManager();
		section.setTextClient(locationBar.createControl(section));
		locationMenu.add(addLocationAction);
		locationMenu.add(removeLocationAction);
		table.setMenu(locationMenu.createContextMenu(table));

		addLocationAction.setEnabled(false);
		removeLocationAction.setEnabled(false);

		// create cell editors
		final CellEditor[] editors = new CellEditor[PROPERTIES.length];
		for (int i = 0; i < editors.length; i++) {
			editors[i] = new TextCellEditor(table);
		}

		locationViewer.setColumnProperties(PROPERTIES);
		locationViewer.setCellModifier(new LocationCellModifier());
		locationViewer.setCellEditors(editors);
		locationViewer.setSorter(new LocationSorter());
		// placeholder
		new Label(body, SWT.NONE).setLayoutData(new GridData(SWT.FILL,
				SWT.FILL, true, true));

		initListeners();
		return body;
	}

	@Override
	protected void onDatabaseSelection(final IDatabase selectedDatabase) {
		database = selectedDatabase;
		try {
			List<Location> objs = database.createDao(Location.class).getAll();
			locations.clear();
			for (Location l : objs) {
				locations.add(l);
			}
			locationViewer.setInput(locations.toArray(new Location[locations
					.size()]));
		} catch (Exception e) {
			log.error("Load locations failed", e);
		}
		addLocationAction.setEnabled(true);
		locationViewer.getTable().setEnabled(true);
	}

	@Override
	protected void performApply() {
		save();
		super.performApply();
	}

	@Override
	protected void save() {
		try {
			BaseDao<Location> dao = database.createDao(Location.class);
			List<Location> dataProviderLocations = dao.getAll();
			List<Location> temp = new ArrayList<>();
			for (Location location : locations) {
				temp.add(location);
			}
			for (Location l : dataProviderLocations) {
				if (temp.contains(l)) {
					dao.update(temp.get(temp.indexOf(l)));
					temp.remove(l);
				} else {
					dao.delete(l);
				}
			}
			for (Location location : temp) {
				dao.insert(location);
			}
		} catch (Exception e) {
			log.error("Save failed", e);
		}
		getApplyButton().setEnabled(false);
		setDirty(false);
	}

	@Override
	public void createControl(final Composite parent) {
		super.createControl(parent);
		getApplyButton().setEnabled(false);
		getDefaultsButton().setVisible(false);
	}

	@Override
	public void init(final IWorkbench workbench) {
	}

	@Override
	public boolean performOk() {
		if (isDirty()) {
			if (Question.ask(Messages.Common_SaveChangesQuestion,
					Messages.Common_SaveChangesQuestion)) {
				save();
			}
		}
		return super.performOk();
	}

	private class AddLocationAction extends Action {

		public AddLocationAction() {
			setId("LocationPreferencePage.AddParameterAction");
			setText(NLS.bind(Messages.AddAction_Text, Messages.Common_Location));
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED
					.getDescriptor());

		}

		@Override
		public void run() {
			Location location = new Location();
			location.setId(UUID.randomUUID().toString());
			location.setName(Messages.Common_Location
					+ (locationViewer.getTable().getItemCount() + 1));
			location.addPropertyChangeListener(LocationPreferencePage.this);
			locations.add(location);
			locationViewer.setInput(locations);
			getApplyButton().setEnabled(true);
			setDirty(true);
		}

	}

	private class LocationCellModifier implements ICellModifier {

		@Override
		public boolean canModify(final Object element, final String property) {
			return true;
		}

		@Override
		public Object getValue(Object element, final String property) {
			Object v = null;
			if (element instanceof Location) {
				Location location = (Location) element;
				if (property.equals(NAME)) {
					v = location.getName();
				} else if (property.equals(DESCRIPTION)) {
					v = location.getDescription();
				} else if (property.equals(CODE)) {
					v = location.getCode();
				} else if (property.equals(LATITUDE)) {
					v = Double.toString(location.getLatitude());
				} else if (property.equals(LONGITUDE)) {
					v = Double.toString(location.getLongitude());
				}
			}
			return v;
		}

		@Override
		public void modify(Object element, final String property,
				final Object value) {
			if (element instanceof Item) {
				element = ((Item) element).getData();
			}

			if (element instanceof Location) {
				final Location location = (Location) element;
				if (property.equals(NAME)) {
					location.setName(value.toString());
				}
				if (property.equals(DESCRIPTION)) {
					location.setDescription(value.toString());
				}
				if (property.equals(CODE)) {
					location.setCode(value.toString());
				} else if (property.equals(LATITUDE)) {
					try {
						double latitude = Double.parseDouble(value.toString());
						location.setLatitude(latitude);
					} catch (NumberFormatException e) {
						log.error("Not a numeric value for latitude", e);
					}
				} else if (property.equals(LONGITUDE)) {
					try {
						double longitude = Double.parseDouble(value.toString());
						location.setLongitude(longitude);
					} catch (final NumberFormatException e) {
						log.error("Not a numeric value for latitude", e);
					}
				}
			}
			locationViewer.refresh();
		}
	}

	private class LocationLabelProvider implements ITableLabelProvider {

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof Location))
				return null;
			Location location = (Location) element;
			switch (columnIndex) {
			case 0:
				return location.getName();
			case 1:
				return location.getDescription();
			case 2:
				return location.getCode();
			case 3:
				return Double.toString(location.getLatitude());
			case 4:
				return Double.toString(location.getLongitude());
			default:
				return null;
			}
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(final ILabelProviderListener listener) {
		}

	}

	private class RemoveLocationAction extends Action {

		public RemoveLocationAction() {
			setId("LocationPreferencePage.RemoveParameterAction");
			setText(NLS.bind(Messages.RemoveAction_Text,
					Messages.Common_Location));
			setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public void run() {
			Location location = Viewers.getFirstSelected(locationViewer);
			if (location == null)
				return;
			DeleteWizard wizard = new DeleteWizard(database, location);
			boolean canDelete = true;
			if (wizard.hasProblems())
				canDelete = new WizardDialog(UI.shell(), wizard).open() == Window.OK;

			if (canDelete) {
				locations.remove(location);
				locationViewer.setInput(locations
						.toArray(new Location[locations.size()]));
				getApplyButton().setEnabled(true);
				setDirty(true);
			}
		}
	}

	private class LocationSorter extends ViewerSorter {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (!(e1 instanceof Location) || !(e2 instanceof Location))
				return 0;
			Location loc1 = (Location) e1;
			Location loc2 = (Location) e2;
			return Strings.compare(loc1.getName(), loc2.getName());
		}
	}

}
