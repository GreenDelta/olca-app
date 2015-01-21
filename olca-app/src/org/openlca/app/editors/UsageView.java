package org.openlca.app.editors;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.util.Editors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.usage.IUseSearch;
import org.openlca.core.model.descriptors.ActorDescriptor;
import org.openlca.core.model.descriptors.BaseDescriptor;
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

/**
 * View of usages of a model entity in other entities.
 */
public class UsageView extends FormEditor {

	public static String ID = "editors.usage";

	private Logger log = LoggerFactory.getLogger(getClass());
	private BaseDescriptor model;
	private IDatabase database;

	public static void open(BaseDescriptor descriptor) {
		if (descriptor == null)
			return;
		UsageViewInput input = new UsageViewInput(descriptor, Database.get());
		Editors.open(input, UsageView.ID);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new Page());
		} catch (Exception e) {
			log.error("Failed to add form page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
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

	private class Page extends FormPage implements HtmlPage {

		private Browser browser;

		public Page() {
			super(UsageView.this, "UsageView.Page", Messages.Usage);
		}

		@Override
		public String getUrl() {
			return HtmlView.USAGES_VIEW.getUrl();
		}

		@Override
		public void onLoaded() {
			log.trace("page completed, set data");
			registerFunction();
			try {
				List<BaseDescriptor> list = runSearch();
				Gson gson = new Gson();
				String json = gson.toJson(list);
				String function = "setData(" + json + ")";
				browser.evaluate(function);
			} catch (Exception e) {
				log.trace("Failed to load data: where used", e);
			}
		}

		private List<BaseDescriptor> runSearch() {
			if (model == null || model.getModelType() == null)
				return Collections.emptyList();
			return IUseSearch.FACTORY.createFor(model.getModelType(), database)
					.findUses(model);
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			if (model == null)
				return;
			ScrolledForm form = UI.formHeader(managedForm, Messages.UsageOf
					+ ": " + Labels.getDisplayName(model));
			FormToolkit toolkit = managedForm.getToolkit();
			Composite body = UI.formBody(form, toolkit);
			browser = UI.createBrowser(body, this);
			UI.gridData(browser, true, true);
			form.reflow(true);
		}

		private void registerFunction() {
			new BrowserFunction(browser, "openModel") {
				@Override
				public Object function(Object[] arguments) {
					openModel(arguments);
					return null;
				}
			};
		}

		private void openModel(Object[] args) {
			if (args == null || args.length < 1 || args[0] == null) {
				log.error("Could not open model, no Json string in arg[0]");
				return;
			}
			String json = args[0].toString();
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
			// load the descriptor specific attributes for the given model type
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
