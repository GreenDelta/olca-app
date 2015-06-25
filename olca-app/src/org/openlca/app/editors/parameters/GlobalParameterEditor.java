package org.openlca.app.editors.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.IEditor;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalParameterEditor extends FormEditor implements IEditor {

	private Logger log = LoggerFactory.getLogger(getClass());

	private List<Parameter> parameters;
	private ParameterChangeSupport support;
	private boolean dirty;

	public static void open() {
		if (Database.get() == null) {
			org.openlca.app.util.Error.showBox(
					Messages.NoDatabaseOpened, Messages.NeedOpenDatabase);
			return;
		}
		GlobalParameterInput input = new GlobalParameterInput();
		Editors.open(input, "editors.GlobalParameterEditor");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		try {
			setPartName(Messages.GlobalParameters);
			ParameterDao dao = new ParameterDao(Database.get());
			parameters = dao.getGlobalParameters();
			support = new ParameterChangeSupport();
			support.doEvaluation(this::evalFormulas);
		} catch (Exception e) {
			log.error("failed to load global parameters", e);
			parameters = new ArrayList<>();
		}
	}

	private void evalFormulas() {
		List<String> errors = Formulas.eval(parameters);
		if (!errors.isEmpty()) {
			String message = errors.get(0);
			if (errors.size() > 1)
				message += " (" + (errors.size() - 1) + " more)";
			org.openlca.app.util.Error.showBox(
					Messages.FormulaEvaluationFailed, message);
		}
	}

	@Override
	protected void addPages() {
		try {
			addPage(new Page());
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

	@Override
	public void setDirty(boolean b) {
		if (b == dirty)
			return;
		dirty = b;
		editorDirtyStateChanged();
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			ParameterDao dao = new ParameterDao(Database.get());
			for (Parameter p : parameters) {
				if (p.getId() == 0)
					dao.insert(p);
				else
					dao.update(p);
			}
			parameters = dao.getGlobalParameters();
			setDirty(false);
		} catch (Exception e) {
			log.error("failed to save global parameters", e);
		}
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	private class Page extends FormPage {

		public Page() {
			super(GlobalParameterEditor.this, "GlobalParameterEditor.Page",
					Messages.GlobalParameters);
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			ScrolledForm form = UI.formHeader(managedForm,
					Messages.GlobalParameters);
			FormToolkit toolkit = managedForm.getToolkit();
			Composite body = UI.formBody(form, toolkit);
			SashForm sash = new SashForm(body, SWT.VERTICAL);
			UI.gridData(sash, true, true);
			toolkit.adapt(sash);
			Supplier<List<Parameter>> supplier = () -> parameters;
			ParameterSection
					.forInputParameters(GlobalParameterEditor.this,
							support, sash, toolkit)
					.setSupplier(supplier, ParameterScope.GLOBAL);
			ParameterSection
					.forDependentParameters(GlobalParameterEditor.this,
							support, sash, toolkit)
					.setSupplier(supplier, ParameterScope.GLOBAL);
		}
	}
}
