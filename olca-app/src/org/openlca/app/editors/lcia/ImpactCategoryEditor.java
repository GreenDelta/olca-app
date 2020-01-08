package org.openlca.app.editors.lcia;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.lcia.shapefiles.ShapeFilePage;
import org.openlca.app.editors.parameters.Formulas;
import org.openlca.app.editors.parameters.ParameterChangeSupport;
import org.openlca.app.editors.parameters.ParameterPage;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.model.ImpactCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImpactCategoryEditor extends ModelEditor<ImpactCategory> {

	private ParameterChangeSupport parameterSupport;

	public ImpactCategoryEditor() {
		super(ImpactCategory.class);
	}

	public ParameterChangeSupport getParameterSupport() {
		return parameterSupport;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		// TODO Auto-generated method stub
		super.init(site, input);
		// evalFormulas() takes quite long; we skip this for LCIA methods
		parameterSupport = new ParameterChangeSupport();
		parameterSupport.onEvaluation(this::evalFormulas);
	}

	private void evalFormulas() {
		ImpactCategory impact = getModel();
		List<String> errors = Formulas.eval(Database.get(), impact);
		if (!errors.isEmpty()) {
			String message = errors.get(0);
			if (errors.size() > 1)
				message += " (" + (errors.size() - 1) + " more)";
			MsgBox.error(M.FormulaEvaluationFailed, message);
		}
	}

	@Override
	protected void addPages() {
		// TODO LCIA factors, parameters, regionalization
		try {
			addPage(new InfoPage(this));
			addPage(new ImpactFactorPage(this));
			addPage(ParameterPage.create(this));
			addPage(new ShapeFilePage(this));
			addCommentPage();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to init pages", e);
		}
	}

	static class InfoPage extends ModelPage<ImpactCategory> {

		InfoPage(ImpactCategoryEditor editor) {
			super(editor, "ImpactCategoryInfo", M.GeneralInformation);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			ScrolledForm form = UI.formHeader(this);
			FormToolkit toolkit = mform.getToolkit();
			Composite body = UI.formBody(form, toolkit);
			InfoSection info = new InfoSection(getEditor());
			info.render(body, toolkit);
			Composite comp = info.getContainer();
			text(comp, M.ReferenceUnit, "referenceUnit");
			body.setFocus();
			form.reflow(true);
		}
	}

}
