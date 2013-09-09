package org.openlca.app.lcia_methods;

import org.openlca.app.Event;
import org.openlca.app.editors.ModelEditor;
import org.openlca.core.editors.IEditor;
import org.openlca.core.model.ImpactMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

public class ImpactMethodEditor extends ModelEditor<ImpactMethod> implements
		IEditor {

	public static String ID = "editors.impactmethod";

	/** An event message that indicates a change of an impact category. */
	final String IMPACT_CATEGORY_CHANGE = "IMPACT_CATEGORY_CHANGE";

	/** An event message that indicates a change of an impact factor. */
	final String IMPACT_FACTOR_CHANGE = "IMPACT_FACTOR_CHANGE";

	private Logger log = LoggerFactory.getLogger(getClass());
	private EventBus eventBus = new EventBus();

	public ImpactMethodEditor() {
		super(ImpactMethod.class);
	}

	public EventBus getEventBus() {
		return eventBus;
	}

	public void postEvent(String message, Object source) {
		eventBus.post(new Event(message, source));
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ImpactMethodInfoPage(this));
			addPage(new ImpactFactorsPage(this));
			addPage(new ImpactNormalizationWeightingPage(this));
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

}
