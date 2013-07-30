package org.openlca.core.application.views;

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
import org.openlca.core.application.App;
import org.openlca.core.application.Messages;
import org.openlca.core.application.plugin.HtmlView;
import org.openlca.core.database.ActorDao;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.FlowPropertyDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.database.SourceDao;
import org.openlca.core.database.UnitGroupDao;
import org.openlca.core.model.Actor;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Process;
import org.openlca.core.model.Source;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.ui.HtmlPage;
import org.openlca.ui.UI;
import org.openlca.ui.html.IHtmlResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * View of usages of a model entity in other entities.
 */
public class UsageView extends FormEditor {

	public static String ID = "application.views.UsageView";

	private Logger log = LoggerFactory.getLogger(getClass());
	private BaseDescriptor model;
	private IDatabase database;

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
			super(UsageView.this, "UsageView.Page", Messages.Common_Usage);
		}

		@Override
		public IHtmlResource getResource() {
			return HtmlView.USAGES_VIEW.getResource();
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
				System.out.println(function);
				browser.evaluate(function);
			} catch (Exception e) {
				log.trace("Failed to load data: where used", e);
			}
		}

		private List<BaseDescriptor> runSearch() {
			if (model == null || model.getModelType() == null)
				return Collections.emptyList();
			switch (model.getModelType()) {
			case ACTOR:
				ActorDao aDao = new ActorDao(database);
				return aDao
						.whereUsed(new Actor(model.getId(), model.getName()));
			case SOURCE:
				SourceDao sDao = new SourceDao(database);
				return sDao
						.whereUsed(new Source(model.getId(), model.getName()));
			case UNIT_GROUP:
				UnitGroupDao uDao = new UnitGroupDao(database);
				return uDao.whereUsed(new UnitGroup(model.getId(), model
						.getName()));
			case FLOW_PROPERTY:
				FlowPropertyDao fpDao = new FlowPropertyDao(database);
				return fpDao.whereUsed(new FlowProperty(model.getId(), model
						.getName()));
			case FLOW:
				FlowDao fDao = new FlowDao(database);
				return fDao.whereUsed(new Flow(model.getId(), model.getName()));
			case PROCESS:
				ProcessDao pDao = new ProcessDao(database);
				return pDao.whereUsed(new Process(model.getId(), model
						.getName()));
			default:
				return Collections.emptyList();
			}
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			if (model == null)
				return;
			ScrolledForm form = UI.formHeader(managedForm,
					Messages.Common_UsageOf + ": " + model.getDisplayName());
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
				Gson gson = new Gson();
				BaseDescriptor descriptor = gson.fromJson(json,
						BaseDescriptor.class);
				App.openEditor(descriptor);
			} catch (Exception e) {
				log.error("Failed to open model from usage page", e);
			}
		}
	}
}
