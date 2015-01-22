package org.openlca.app.preferencepages;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
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
import org.openlca.app.Messages;
import org.openlca.app.components.delete.DeleteWizard;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Question;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.core.database.BaseDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.usage.IUseSearch;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private IDatabase database;
	private final List<Location> locations = new ArrayList<>();
	private TableViewer viewer;
	private boolean dirty;

	private final String CODE = Messages.Code;
	private final String DESCRIPTION = Messages.Description;
	private final String LATITUDE = Messages.Latitude;
	private final String LONGITUDE = Messages.Longitude;
	private final String NAME = Messages.Name;

	private final String[] PROPERTIES = new String[] { NAME, DESCRIPTION, CODE,
			LATITUDE, LONGITUDE };

	@Override
	public void init(IWorkbench workbench) {
		database = Database.get();
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		getApplyButton().setEnabled(false);
		getDefaultsButton().setVisible(false);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite body = new Composite(parent, SWT.NONE);
		body.setLayout(new GridLayout());

		Section section = new Section(body, ExpandableComposite.NO_TITLE);
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite composite = new Composite(section, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		section.setClient(composite);

		createViewer(composite);
		Table table = configureTable(parent);
		createActionBars(section, table);
		createCellEditors(table);
		new Label(body, SWT.NONE).setLayoutData(new GridData(SWT.FILL,
				SWT.FILL, true, true)); // placeholder
		initData();
		return body;
	}

	private void createViewer(Composite composite) {
		viewer = new TableViewer(composite, SWT.BORDER | SWT.MULTI
				| SWT.FULL_SELECTION | SWT.NO_REDRAW_RESIZE | SWT.V_SCROLL);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new LocationLabelProvider());
		viewer.getTable().setEnabled(false);
	}

	private Table configureTable(Composite parent) {
		Table table = viewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
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
		return table;
	}

	private void createCellEditors(Table table) {
		CellEditor[] editors = new CellEditor[PROPERTIES.length];
		for (int i = 0; i < editors.length; i++) {
			editors[i] = new TextCellEditor(table);
		}
		viewer.setColumnProperties(PROPERTIES);
		viewer.setCellModifier(new LocationCellModifier());
		viewer.setCellEditors(editors);
	}

	private void createActionBars(Section section, Table table) {
		if (database == null)
			return;
		Action add = new AddLocationAction();
		Action remove = new RemoveLocationAction();
		remove.setEnabled(false);
		viewer.addSelectionChangedListener((event) ->
				remove.setEnabled(!event.getSelection().isEmpty()));
		ToolBarManager toolBar = new ToolBarManager();
		toolBar.add(add);
		toolBar.add(remove);
		MenuManager menu = new MenuManager();
		section.setTextClient(toolBar.createControl(section));
		menu.add(add);
		menu.add(remove);
		menu.add(TableClipboard.onCopy(table));
		table.setMenu(menu.createContextMenu(table));
	}

	@Override
	public boolean performOk() {
		if (isDirty()) {
			if (Question.ask(Messages.SaveChangesQuestion,
					Messages.SaveChangesQuestion)) {
				save();
			}
		}
		return super.performOk();
	}

	private void initData() {
		if (database == null)
			return;
		List<Location> objs = database.createDao(Location.class).getAll();
		locations.clear();
		locations.addAll(objs);
		locations.sort((loc1, loc2)
				-> Strings.compare(loc1.getName(), loc2.getName()));
		viewer.setInput(locations);
		viewer.getTable().setEnabled(true);
	}

	@Override
	protected void performApply() {
		save();
		super.performApply();
	}

	private void save() {
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

	private void setDirty(boolean value) {
		dirty = value;
	}

	private boolean isDirty() {
		return dirty;
	}

	private class AddLocationAction extends Action {

		public AddLocationAction() {
			setId("LocationPreferencePage.AddParameterAction");
			setText(NLS.bind(Messages.CreateNew, Messages.Location));
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED
					.getDescriptor());

		}

		@Override
		public void run() {
			Location location = new Location();
			location.setName(Messages.Location
					+ (viewer.getTable().getItemCount() + 1));
			locations.add(location);
			viewer.setInput(locations);
			getApplyButton().setEnabled(true);
			setDirty(true);
		}

	}

	private class LocationCellModifier implements ICellModifier {

		@Override
		public boolean canModify(Object element, String property) {
			return true;
		}

		@Override
		public Object getValue(Object element, String property) {
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
		public void modify(Object element, String property,
				Object value) {
			if (element instanceof Item) {
				element = ((Item) element).getData();
			}

			if (element instanceof Location) {
				Location location = (Location) element;
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
					} catch (NumberFormatException e) {
						log.error("Not a numeric value for latitude", e);
					}
				}
			}
			viewer.refresh();
		}
	}

	private class LocationLabelProvider extends LabelProvider implements
			ITableLabelProvider {

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

	}

	private class RemoveLocationAction extends Action {

		public RemoveLocationAction() {
			setId("LocationPreferencePage.RemoveParameterAction");
			setText(NLS.bind(Messages.RemoveSelected, Messages.Location));
			setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public void run() {
			Location location = Viewers.getFirstSelected(viewer);
			if (location == null)
				return;
			DeleteWizard<BaseDescriptor> wizard = new DeleteWizard<>(
					IUseSearch.FACTORY.createFor(ModelType.LOCATION, database),
					Descriptors.toDescriptor(location));
			boolean canDelete = true;
			if (wizard.hasProblems())
				canDelete = new WizardDialog(UI.shell(), wizard).open() == Window.OK;

			if (canDelete) {
				locations.remove(location);
				viewer.setInput(locations);
				getApplyButton().setEnabled(true);
				setDirty(true);
			}
		}
	}

}
