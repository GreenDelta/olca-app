package org.openlca.app.navigation.actions.nexus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.actions.nexus.XNexusIndexExportAction.IndexEntry;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.app.viewers.tables.modify.TextCellModifier;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.model.Process;
import org.openlca.util.KeyGen;
import org.python.jline.internal.Log;

public class XNexusEcoinventIndexExportAction extends Action implements INavigationAction {

	public XNexusEcoinventIndexExportAction() {
		setImageDescriptor(Icon.EXTENSION.descriptor());
		setText("Export Ecoinvent Nexus JSON Index");
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.isEmpty())
			return true;
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement e))
			return false;
		return Database.isActive(e.getContent());
	}

	@Override
	public void run() {
		var dialog = new DbSelectDialog();
		if (dialog.open() != IDialogConstants.OK_ID)
			return;
		var file = FileChooser.forSavingFile(
				M.Export, "ecoinvent_nexus_index.json");
		if (file == null)
			return;
		App.runWithProgress("Creating Ecoinvent Nexus Index", () -> {
			try {
				var index = new HashMap<String, IndexEntry>();
				for (var e : dialog.entries) {
					if (e.database == null)
						continue;
					var db = Database.isActive(e.database)
							? Database.get()
							: e.database.connect(Workspace.dbDir());
					var dao = new ProcessDao(db);
					for (var descriptor : dao.getDescriptors()) {
						var process = dao.getForId(descriptor.id);
						var id = getId(process);
						var entry = index.get(id);
						if (entry == null) {
							index.put(id, entry = new IndexEntry(process));
						}
						entry.name = getName(process);
						entry.systemModel.add(e.systemModel);
					}
					if (!Database.isActive(e.database)) {
						db.close();
					}
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

	private static class DbSelectDialog extends FormDialog {

		private final List<Entry> entries = new ArrayList<>();
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
			var tk = form.getToolkit();
			UI.header(form, "Select system model databases");
			var body = UI.dialogBody(form.getForm(), tk);
			viewer = Tables.createViewer(body, "System model", "Database");
			viewer.setLabelProvider(new Label());
			entries.add(new Entry("Cut-off"));
			entries.add(new Entry("Consequential long-term"));
			entries.add(new Entry("APOS"));
			setInput();
			var ms = new ModifySupport<Entry>(viewer);
			ms.bind("System model", new SystemModelCell());
			ms.bind("Database", new DatabaseCell());

			// actions
			var onAdd = Actions.create(
					"Add system model", Icon.ADD.descriptor(), () -> {
						entries.add(new Entry("model"));
						setInput();
					});
			var onRemove = Actions.create(
					"Remove system model", Icon.DELETE.descriptor(), () -> {
						if (Viewers.getFirstSelected(viewer) instanceof Entry e) {
							entries.remove(e);
							setInput();
						}
					}
			);

			Actions.bind(viewer, onAdd, onRemove);
		}

		private void setInput() {
			viewer.setInput(entries.toArray(new Entry[0]));
		}

		private static class Label extends LabelProvider implements ITableLabelProvider {

			@Override
			public Image getColumnImage(Object obj, int col) {
				if (col == 0)
					return null;
				if (!(obj instanceof Entry entry))
					return null;
				return entry.database != null
						? Icon.DATABASE.get()
						: null;
			}

			@Override
			public String getColumnText(Object obj, int col) {
				if (!(obj instanceof Entry entry))
					return "";
				return switch (col) {
					case 0 -> entry.systemModel;
					case 1 -> entry.database != null
							? entry.database.name()
							: "";
					default -> "";
				};
			}
		}

		private static class SystemModelCell extends TextCellModifier<Entry> {

			@Override
			protected String getText(Entry element) {
				return element.systemModel;
			}

			@Override
			protected void setText(Entry element, String text) {
				element.systemModel = text;
			}
		}

		private static class DatabaseCell extends
				ComboBoxCellModifier<Entry, DatabaseConfig> {

			@Override
			protected DatabaseConfig[] getItems(Entry element) {
				var dbs = new ArrayList<DatabaseConfig>();
				dbs.addAll(Database.getConfigurations().getDerbyConfigs());
				dbs.addAll(Database.getConfigurations().getMySqlConfigs());
				return dbs.toArray(new DatabaseConfig[0]);
			}

			@Override
			protected DatabaseConfig getItem(Entry element) {
				return element.database;
			}

			@Override
			protected String getText(DatabaseConfig value) {
				return value.name();
			}

			@Override
			protected void setItem(Entry element, DatabaseConfig item) {
				element.database = item;
			}
		}
	}

	private static class Entry {

		String systemModel;
		DatabaseConfig database;

		Entry(String systemModel) {
			this.systemModel = systemModel;
		}
	}
}
