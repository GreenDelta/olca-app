package org.openlca.app.editors.lcia_methods;

import java.util.List;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.IEditor;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.parameters.Formulas;
import org.openlca.app.editors.parameters.ModelParameterPage;
import org.openlca.app.editors.parameters.ParameterChangeSupport;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.core.model.ImpactMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImpactMethodEditor extends ModelEditor<ImpactMethod> implements
		IEditor {

	public static String ID = "editors.impactmethod";

	/**
	 * An event message that indicates a change of an impact category.
	 */
	final String IMPACT_CATEGORY_CHANGE = "IMPACT_CATEGORY_CHANGE";

	private Logger log = LoggerFactory.getLogger(getClass());
	private ParameterChangeSupport parameterSupport;

	public ImpactMethodEditor() {
		super(ImpactMethod.class);
	}

	public ParameterChangeSupport getParameterSupport() {
		return parameterSupport;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		// evalFormulas() takes quite long; we skip this for LCIA methods
		parameterSupport = new ParameterChangeSupport();
		parameterSupport.doEvaluation(this::evalFormulas);
	}

	private void evalFormulas() {
		ImpactMethod m = getModel();
		List<String> errors = Formulas.eval(Database.get(), m);
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
			addPage(new ImpactMethodInfoPage(this));
			addPage(new ImpactFactorPage(this));
			addPage(new ImpactNwPage(this));
			addPage(new ModelParameterPage(this));
			if (FeatureFlag.LOCALISED_LCIA.isEnabled()) {
				addPage(new ShapeFilePage(this));
			}
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

}
