package org.openlca.app.editors.lcia_methods;

import java.util.List;
import java.util.function.Supplier;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.editors.IEditor;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ParameterPage;
import org.openlca.app.editors.ParameterPageSupport;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImpactMethodEditor extends ModelEditor<ImpactMethod> implements
		IEditor {

	public static String ID = "editors.impactmethod";

	/**
	 * An event message that indicates a change of an impact category.
	 */
	final String IMPACT_CATEGORY_CHANGE = "IMPACT_CATEGORY_CHANGE";

	/**
	 * An event message that indicates a change of an impact factor.
	 */
	final String IMPACT_FACTOR_CHANGE = "IMPACT_FACTOR_CHANGE";

	private Logger log = LoggerFactory.getLogger(getClass());
	private ParameterPageSupport parameterSupport;

	public ImpactMethodEditor() {
		super(ImpactMethod.class);
	}

	public ParameterPageSupport getParameterSupport() {
		return parameterSupport;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		// Formulas.eval(Database.get(), getModel());
		Supplier<List<Parameter>> supplier = () -> getModel().getParameters();
		parameterSupport = new ParameterPageSupport(this, supplier,
				ParameterScope.IMPACT_METHOD);
		// it is important that this listener is added before the listener
		// in the LCIA factor page, otherwise the factor table will be
		// refreshed with old values
		parameterSupport.addListener(() -> {
			log.trace("evaluate LCIA factor formulas");
			for (ImpactCategory category : getModel().getImpactCategories()) {
				for (ImpactFactor factor : category.getImpactFactors()) {
					parameterSupport.eval(factor);
				}
			}
		});
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ImpactMethodInfoPage(this));
			addPage(new ImpactFactorPage(this));
			addPage(new ImpactNwPage(this));
			addPage(new ParameterPage(parameterSupport));
			if (FeatureFlag.LOCALISED_LCIA.isEnabled()) {
				addPage(new ShapeFilePage(this));
				// addPage(new ImpactLocalisationPage(this));
			}
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

}
