package org.openlca.app.inventory;

import java.util.Collection;

import org.openlca.core.database.EntityCache;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;

/**
 * Provider of the total impact category results of a product system. We put
 * inventory results and analysis results behind this interface so that they can
 * use the same editor pages for the overall impact result.
 */
public interface ImpactResultProvider {

	/** Get all impact categories of the result. */
	public Collection<ImpactCategoryDescriptor> getImpactCategories(
			EntityCache cache);

	/** Get the result for the given impact category. */
	public double getAmount(ImpactCategoryDescriptor impact);

}
