package org.openlca.app.editors.lcia;

import org.openlca.app.db.Cache;
import org.openlca.app.editors.ModelEditor;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.descriptors.Descriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImpactMethodEditor extends ModelEditor<ImpactMethod> {

	public static String ID = "editors.impactmethod";

	/**
	 * An event message that indicates a change of an impact category.
	 */
	final String IMPACT_CATEGORY_CHANGE = "IMPACT_CATEGORY_CHANGE";

	private Logger log = LoggerFactory.getLogger(getClass());

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
			log.error("failed to add page", e);
		}
	}

	@Override
	protected void doAfterUpdate() {
		super.doAfterUpdate();
		// TODO: this is not necessary anymore?
		EntityCache cache = Cache.getEntityCache();
		for (ImpactCategory category : getModel().impactCategories) {
			cache.refresh(ImpactCategory.class, category.id);
			cache.invalidate(ImpactCategory.class, category.id);
			Cache.evict(Descriptor.of(category));
		}
	}

}
