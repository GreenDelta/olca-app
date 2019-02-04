package org.openlca.app.navigation.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.db.DerbyConfiguration;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.db.MySQLConfiguration;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.actions.XNexusIndexExportAction.IndexEntry;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.viewers.table.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.KeyGen;
import org.python.jline.internal.Log;

public class XNexusEcoinventIndexExportAction extends Action implements INavigationAction {

	public XNexusEcoinventIndexExportAction() {
		setImageDescriptor(Icon.EXTENSION.descriptor());
		setText("Export Ecoinvent Nexus Index");
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement e = (DatabaseElement) element;
		return Database.isActive(e.getContent());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		DbSelectDialog dialog = new DbSelectDialog();
		if (dialog.open() != IDialogConstants.OK_ID)
			return;
		File file = FileChooser.forExport("*.json", "ecoinvent_nexus_index.json");
		if (file == null)
			return;
		App.runWithProgress("Creating Ecoinvent Nexus Index", () -> {
			try {
				Map<String, IndexEntry> index = new HashMap<>();
				for (Entry e : dialog.entries) {
					IDatabase db = null;
					if (Database.get() != null && Database.get().getName().equals(e.database.getName())) {
						db = Database.get();
					} else {
						db = e.database.createInstance();
					}
					ProcessDao dao = new ProcessDao(db);
					for (ProcessDescriptor descriptor : dao.getDescriptors()) {
						Process process = dao.getForId(descriptor.id);
						String id = getId(process);
						IndexEntry entry = index.get(id);
						if (entry == null) {
							index.put(id, entry = new IndexEntry(process));
						}
						entry.name = getName(process);
						entry.systemModel.add(e.systemModel);
					}
					db.close();
				}
				IndexEntry.writeEntries(index.values(), file);
			} catch (Exception e) {
				Log.error("Error creating ecoinvent nexus index", e);
			}
		});
	}

	private String getId(Process p) {
		String name = getName(p);
		String product = p.quantitativeReference.flow.name;
		String location = p.location != null
				? p.location.code
				: null;
		return KeyGen.get(name, product, location);
	}

	private String getName(Process process) {
		return process.name.substring(0, process.name.lastIndexOf('|')).trim();
	}

	private class DbSelectDialog extends FormDialog {

		private List<Entry> entries = new ArrayList<>();
		private TableViewer viewer;

		public DbSelectDialog() {
			super(UI.shell());
		}

		@Override
		protected Point getInitialSize() {
			return new Point(600, 400);
		}

		@Override
		protected void createFormContent(IManagedForm form) {
			FormToolkit toolkit = form.getToolkit();
			UI.formHeader(form, "Select system model databases");
			Composite body = UI.formBody(form.getForm(), toolkit);
			viewer = Tables.createViewer(body, "System model", "Database");
			viewer.setLabelProvider(new Label());
			entries.add(new Entry("Cut-off"));
			entries.add(new Entry("Consequential long-term"));
			entries.add(new Entry("APOS"));
			setInput();
			ModifySupport<Entry> ms = new ModifySupport<>(viewer);
			ms.bind("System model", new SystemModelCell());
			ms.bind("Database", new DatabaseCell());
			Actions.bind(viewer, new AddAction());
		}

		private void setInput() {
			viewer.setInput(entries.toArray(new Entry[entries.size()]));
		}

		private class Label extends LabelProvider implements ITableLabelProvider {

			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				Entry entry = (Entry) element;
				if (columnIndex == 0)
					return null;
				if (entry.database instanceof DerbyConfiguration)
					return Icon.DATABASE.get();
				if (entry.database instanceof MySQLConfiguration)
					return Icon.SQL.get();
				return null;
			}

			@Override
			public String getColumnText(Object element, int columnIndex) {
				Entry entry = (Entry) element;
				switch (columnIndex) {
				case 0:
					return entry.systemModel;
				case 1:
					if (entry.database == null)
						return "";
					return entry.database.getName();
				default:
					return "";
				}
			}

		}

		private class SystemModelCell extends TextCellModifier<Entry> {

			@Override
			protected String getText(Entry element) {
				return element.systemModel;
			}

			@Override
			protected void setText(Entry element, String text) {
				element.systemModel = text;
			}

		}

		private class DatabaseCell extends ComboBoxCellModifier<Entry, IDatabaseConfiguration> {

			@Override
			protected IDatabaseConfiguration[] getItems(Entry element) {
				List<IDatabaseConfiguration> databases = new ArrayList<>();
				databases.addAll(Database.getConfigurations().getLocalDatabases());
				databases.addAll(Database.getConfigurations().getRemoteDatabases());
				return databases.toArray(new IDatabaseConfiguration[databases.size()]);
			}

			@Override
			protected IDatabaseConfiguration getItem(Entry element) {
				return element.database;
			}

			@Override
			protected String getText(IDatabaseConfiguration value) {
				return value.getName();
			}

			@Override
			protected void setItem(Entry element, IDatabaseConfiguration item) {
				element.database = item;
			}

		}

		private class AddAction extends Action {

			public AddAction() {
				setText("Add system model");
				setImageDescriptor(Icon.ADD.descriptor());
			}

			@Override
			public void run() {
				entries.add(new Entry("model"));
				setInput();
			}

		}

	}

	private class Entry {

		private String systemModel;
		private IDatabaseConfiguration database;

		private Entry(String systemModel) {
			this.systemModel = systemModel;
		}

	}

}
