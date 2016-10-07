package org.openlca.app.editors;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.util.Editors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.usage.IUseSearch;
import org.openlca.core.model.descriptors.ActorDescriptor;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;
import org.openlca.core.model.descriptors.ProjectDescriptor;
import org.openlca.core.model.descriptors.SourceDescriptor;
import org.openlca.core.model.descriptors.UnitGroupDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import javafx.scene.web.WebEngine;

/**
 * View of usages of a model entity in other entities.
 */
public class UsageView extends SimpleFormEditor {

	public static String ID = "editors.usage";

	private Logger log = LoggerFactory.getLogger(getClass());
	private CategorizedDescriptor model;
	private IDatabase database;

	public static void open(CategorizedDescriptor descriptor) {
		if (descriptor == null)
			return;
		UsageViewInput input = new UsageViewInput(descriptor, Database.get());
		Editors.open(input, UsageView.ID);
	}

	@Override
	protected FormPage getPage() {
		return new Page();
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		if (input instanceof UsageViewInput) {
			UsageViewInput in = (UsageViewInput) input;
			model = in.getDescriptor();
			database = in.getDatabase();
		}
	}

	private class Page extends FormPage implements WebPage {

		public Page() {
			super(UsageView.this, "UsageView.Page", M.Usage);
		}

		@Override
		public String getUrl() {
			return HtmlView.USAGES_VIEW.getUrl();
		}

		@Override
		public void onLoaded(WebEngine webkit) {
			log.trace("page completed, set data");
			UI.bindVar(webkit, "java", new JsHandler());
			try {
				List<CategorizedDescriptor> list = runSearch();
				Gson gson = new Gson();
				String json = gson.toJson(list);
				String command = "setData(" + json + ")";
				webkit.executeScript(command);
			} catch (Exception e) {
				log.trace("Failed to load data: where used", e);
			}
		}

		private List<CategorizedDescriptor> runSearch() {
			if (model == null || model.getModelType() == null)
				return Collections.emptyList();
			return IUseSearch.FACTORY.createFor(model.getModelType(), database)
					.findUses(model);
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			if (model == null)
				return;
			ScrolledForm form = UI.formHeader(managedForm, M.UsageOf
					+ ": " + Labels.getDisplayName(model));
			FormToolkit toolkit = managedForm.getToolkit();
			Composite body = UI.formBody(form, toolkit);
			body.setLayout(new FillLayout());
			UI.createWebView(body, this);
			form.reflow(true);
		}
	}

	public class JsHandler {

		public void openModel(String json) {
			log.trace("open model: json={}", json);
			try {
				BaseDescriptor descriptor = getDescriptor(json);
				if (descriptor != null)
					App.openEditor(descriptor);
			} catch (Exception e) {
				log.error("Failed to open model from usage page", e);
			}
		}

		private BaseDescriptor getDescriptor(String json) {
			Gson gson = new Gson();
			BaseDescriptor descriptor = gson.fromJson(json,
					BaseDescriptor.class);
			if (descriptor == null || descriptor.getModelType() == null)
				return descriptor;
			// load the descriptor specific attributes for the given model
			// type
			// this also assures object equality when comparing different
			// descriptor objects, e.g. when opening the editor
			switch (descriptor.getModelType()) {
			case ACTOR:
				return gson.fromJson(json, ActorDescriptor.class);
			case FLOW:
				return gson.fromJson(json, FlowDescriptor.class);
			case FLOW_PROPERTY:
				return gson.fromJson(json, FlowPropertyDescriptor.class);
			case IMPACT_METHOD:
				return gson.fromJson(json, ImpactMethodDescriptor.class);
			case PROCESS:
				return gson.fromJson(json, ProcessDescriptor.class);
			case PRODUCT_SYSTEM:
				return gson.fromJson(json, ProductSystemDescriptor.class);
			case PROJECT:
				return gson.fromJson(json, ProjectDescriptor.class);
			case SOURCE:
				return gson.fromJson(json, SourceDescriptor.class);
			case UNIT_GROUP:
				return gson.fromJson(json, UnitGroupDescriptor.class);
			default:
				return descriptor;
			}
		}
	}
}
