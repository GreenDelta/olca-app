package org.openlca.app.db;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Editors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Strings;

public class LinkingPropertiesPage extends SimpleFormEditor {

	private LinkingProperties props;

	public static void show() {
		AtomicReference<LinkingProperties> ref = new AtomicReference<>();
		App.runWithProgress("#Check database links", () -> {
			LinkingProperties props = LinkingProperties.check(Database.get());
			ref.set(props);
		});
		LinkingProperties props = ref.get();
		if (props == null)
			return;
		String key = Cache.getAppCache().put(props);
		Editors.open(new Input(key), "editors.LinkingPropertiesPage");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		Input in = (Input) input;
		props = Cache.getAppCache().remove(
				in.key, LinkingProperties.class);
	}

	@Override
	protected FormPage getPage() {
		return new Page();
	}

	private class Page extends FormPage {

		private FormToolkit tk;

		public Page() {
			super(LinkingPropertiesPage.this,
					"LinkingPropertiesPage.Page",
					"#Linking properties");
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			ScrolledForm form = UI.formHeader(mform, "#Linking properties");
			tk = mform.getToolkit();
			Composite body = UI.formBody(form, tk);
			generalPropertiesSection(body);
			processProviderSection(body);
			flowProviderSection(body);
			form.reflow(true);
		}

		private void generalPropertiesSection(Composite body) {
			Composite comp = UI.formSection(body, tk,
					"#General database properties");
			if (props.processesWithoutProviders.isEmpty()) {
				check(comp, Icon.ACCEPT, "#All product inputs "
						+ "and waste outputs are linked to a "
						+ "default provider.");
			} else {
				check(comp, Icon.WARNING, "#There are processes "
						+ "in the database without default providers"
						+ " for product inputs and/or waste outputs "
						+ "(see table below).");
			}
			if (props.multiProviderFlows.isEmpty()) {
				check(comp, Icon.ACCEPT, "#All product and waste"
						+ " flows in the database have a single "
						+ "provider.");
			} else {
				check(comp, Icon.WARNING, "#There are product "
						+ "and/or waste flows in the database "
						+ "that have multiple providers "
						+ "(see table below).");
			}
		}

		private void check(Composite comp, Icon icon, String message) {
			tk.createLabel(comp, "").setImage(icon.get());
			tk.createLabel(comp, message, SWT.WRAP); // TODO WRAP currently does
														// not work
		}

		private void processProviderSection(Composite body) {
			if (props.processesWithoutProviders.isEmpty())
				return;
			Composite comp = UI.formSection(body, tk,
					"#Processes without providers (max. 50)");
			UI.gridLayout(comp, 1);
			TableViewer table = Tables.createViewer(comp, "#Process");
			ProcessDao dao = new ProcessDao(Database.get());
			List<ProcessDescriptor> list = dao.getDescriptors(
					props.processesWithoutProviders);
			fillTable(table, list);
		}

		private void flowProviderSection(Composite body) {
			if (props.multiProviderFlows.isEmpty())
				return;
			Composite comp = UI.formSection(body, tk,
					"#Product or waste flows with multiple providers (max. 50)");
			UI.gridLayout(comp, 1);
			TableViewer table = Tables.createViewer(comp, "#Flow");
			FlowDao dao = new FlowDao(Database.get());
			List<FlowDescriptor> list = dao.getDescriptors(
					props.multiProviderFlows);
			fillTable(table, list);
		}

		private <T extends CategorizedDescriptor> void fillTable(
				TableViewer table, List<T> list) {
			table.setLabelProvider(new TableLabel());
			Tables.bindColumnWidths(table, 1.0);
			list.sort((d1, d2) -> Strings.compare(
					Labels.getDisplayName(d1),
					Labels.getDisplayName(d2)));
			table.setInput(list);
			Tables.onDoubleClick(table, e -> {
				CategorizedDescriptor d = Viewers.getFirstSelected(table);
				App.openEditor(d);
			});
		}

		private class TableLabel extends LabelProvider implements ITableLabelProvider {

			@Override
			public Image getColumnImage(Object o, int col) {
				if (col != 0)
					return null;
				if (!(o instanceof BaseDescriptor))
					return null;
				BaseDescriptor d = (BaseDescriptor) o;
				return Images.get(d);
			}

			@Override
			public String getColumnText(Object o, int col) {
				if (col != 0)
					return null;
				if (!(o instanceof BaseDescriptor))
					return null;
				BaseDescriptor d = (BaseDescriptor) o;
				return Labels.getDisplayName(d);
			}
		}
	}

	private static class Input implements IEditorInput {

		private final String key;

		public Input(String key) {
			this.key = key;
		}

		@Override
		@SuppressWarnings("rawtypes")
		// need to support several target platforms
		public Object getAdapter(Class adapter) {
			return null;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public ImageDescriptor getImageDescriptor() {
			return Icon.LINK.descriptor();
		}

		@Override
		public String getName() {
			return "#Linking properties";
		}

		@Override
		public IPersistableElement getPersistable() {
			return null;
		}

		@Override
		public String getToolTipText() {
			return getName();
		}
	}
}
