package org.openlca.app.editors.lcia;

import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.ImpactMethod;

public class ImpactMethodEditor extends ModelEditor<ImpactMethod> {

	public static String ID = "editors.impactmethod";

	/**
	 * An event message that indicates a change of an impact category.
	 */
	final String IMPACT_CATEGORY_CHANGE = "IMPACT_CATEGORY_CHANGE";

	public ImpactMethodEditor() {
		super(ImpactMethod.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ImpactMethodInfoPage(this));
			addPage(new ImpactNwPage(this));
			addCommentPage();
		} catch (Exception e) {
			ErrorReporter.on("failed to add editor pages", e);
		}
	}
}
