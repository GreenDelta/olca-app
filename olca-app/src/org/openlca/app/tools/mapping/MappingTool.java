package org.openlca.app.tools.mapping;

import java.util.UUID;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.tools.mapping.model.FlowMap;
import org.openlca.app.tools.mapping.model.IMapProvider;
import org.openlca.app.util.Info;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingTool extends SimpleFormEditor {

	private MappingPage page;
	private FlowMap flowMap;
	private IMapProvider provider;

	public static void open() {
		if (Database.get() == null) {
			Info.showBox(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		Editors.open(new SimpleEditorInput(
				"FlowMappings", UUID.randomUUID().toString(), "#Flow mapping"),
				"MappingTool");
	}

	@Override
	protected FormPage getPage() {
		page = new MappingPage();
		return page;
	}

	void setContent(FlowMap flowMap, IMapProvider provider) {
		this.flowMap = flowMap;
		closeProvider();
		this.provider = provider;
		this.flowMap = flowMap;
	}

	@Override
	public void close(boolean save) {
		closeProvider();
		super.close(save);
	}

	private void closeProvider() {
		if (this.provider == null)
			return;
		try {
			this.provider.close();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Failed to close provider", e);
		}
	}

	private class MappingPage extends FormPage {

		public MappingPage() {
			super(MappingTool.this, "MappingPage", "#Flow mapping");
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			ScrolledForm form = UI.formHeader(mform, "#Flow mapping");
			FormToolkit tk = mform.getToolkit();
			Composite body = UI.formBody(form, tk);

			TableViewer table = Tables.createViewer(
					body,
					"Source flow",
					"Source category",
					"Source unit",
					"Target flow",
					"Target category",
					"Target unit",
					"Conversion factor",
					"Default provider");
			table.setLabelProvider(new LabelProvider());

			double w = 1.0 / 8.0;
			Tables.bindColumnWidths(table, w, w, w, w, w, w, w, w);

			form.reflow(true);
		}
	}
}
