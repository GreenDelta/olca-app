package org.openlca.app.db;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.AppContext;
import org.openlca.app.M;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.commons.Strings;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

public class LinkingPropertiesPage extends SimpleFormEditor {

	private LinkingProperties props;

	public static void show() {
		AtomicReference<LinkingProperties> ref = new AtomicReference<>();
		App.runWithProgress(M.CheckDatabaseLinks, () -> {
			LinkingProperties props = LinkingProperties.check(Database.get());
			ref.set(props);
		});
		show(ref.get());
	}

	public static void show(LinkingProperties props) {
		if (props == null)
			return;
		String key = AppContext.put(props);
		var input = new SimpleEditorInput(key, M.LinkingProperties);
		Editors.open(input, "editors.LinkingPropertiesPage");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		setTitleImage(Icon.DATABASE.get());
		var in = (SimpleEditorInput) input;
		props = AppContext.remove(in.id, LinkingProperties.class);
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
					M.LinkingProperties);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			ScrolledForm form = UI.header(mform, M.LinkingProperties);
			tk = mform.getToolkit();
			Composite body = UI.body(form, tk);
			generalPropertiesSection(body);
			processProviderSection(body);
			flowProviderSection(body);
			form.reflow(true);
		}

		private void generalPropertiesSection(Composite body) {
			Composite comp = UI.formSection(body, tk,
					M.RecommendedSettingProductSystems);
			if (props.processesWithoutProviders.isEmpty()) {
				check(comp, Icon.ACCEPT, M.AllProductInputsWasteOutputsAreLinked);
			} else {
				check(comp, Icon.WARNING, M.ThereAreProcessesWithoutDefaultProviders);
			}
			if (props.multiProviderFlows.isEmpty()) {
				check(comp, Icon.ACCEPT, M.AllProductAndWasteHaveSingleProvider);
			} else {
				check(comp, Icon.WARNING, M.ThereAreProductWasteWithMultipleProviders);
			}
			new LinkingMatrix(props).render(body, tk);
		}

		private void check(Composite comp, Icon icon, String message) {
			UI.label(comp, tk, "").setImage(icon.get());
			UI.label(comp, tk, message, SWT.WRAP);  // TODO WRAP currently does
														// not work
		}

		private void processProviderSection(Composite body) {
			if (props.processesWithoutProviders.isEmpty())
				return;
			Section section = UI.section(body, tk,
					M.ProcessesWithoutProviders);
			UI.gridData(section, true, true);
			Composite comp = UI.sectionClient(section, tk);
			UI.gridLayout(comp, 1);
			TableViewer table = Tables.createViewer(comp, M.Process);
			ProcessDao dao = new ProcessDao(Database.get());
			List<ProcessDescriptor> list = dao.getDescriptors(
					props.processesWithoutProviders);
			fillTable(table, list);
		}

		private void flowProviderSection(Composite body) {
			if (props.multiProviderFlows.isEmpty())
				return;
			Section section = UI.section(body, tk,
					M.ProductOrWasteWithMultipleProviders);
			UI.gridData(section, true, true);
			Composite comp = UI.sectionClient(section, tk);
			UI.gridLayout(comp, 1);
			TableViewer table = Tables.createViewer(comp, M.Flow);
			FlowDao dao = new FlowDao(Database.get());
			List<FlowDescriptor> list = dao.getDescriptors(
					props.multiProviderFlows);
			fillTable(table, list);
		}

		private <T extends RootDescriptor> void fillTable(
				TableViewer table, List<T> list) {
			table.setLabelProvider(new TableLabel());
			Tables.bindColumnWidths(table, 1.0);
			list.sort((d1, d2) -> Strings.compareIgnoreCase(
					Labels.name(d1),
					Labels.name(d2)));
			table.setInput(list);
			Tables.onDoubleClick(table, e -> {
				RootDescriptor d = Viewers.getFirstSelected(table);
				App.open(d);
			});
			Action copy = TableClipboard.onCopySelected(table);
			Actions.bind(table, copy);
		}

		private static class TableLabel extends LabelProvider
				implements ITableLabelProvider {

			@Override
			public Image getColumnImage(Object o, int col) {
				if (col != 0)
					return null;
				if (!(o instanceof Descriptor d))
					return null;
				return Images.get(d);
			}

			@Override
			public String getColumnText(Object o, int col) {
				if (col != 0)
					return null;
				if (!(o instanceof Descriptor))
					return null;
				return Labels.name((Descriptor) o);
			}
		}
	}

}
