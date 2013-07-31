package org.openlca.app.search;

import org.openlca.core.database.CategoryPath;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.modelprovider.IModelComponent;

/**
 * The result of a search.
 */
class SearchResult {

	private IDatabase database;
	private IModelComponent modelComponent;
	private String categoryPath;

	public SearchResult(IModelComponent modelComponent, IDatabase database) {
		this.modelComponent = modelComponent;
		this.database = database;
	}

	public IDatabase getDatabase() {
		return database;
	}

	public IModelComponent getModelComponent() {
		return modelComponent;
	}

	public String getCategoryPath() {
		if (categoryPath == null) {
			categoryPath = CategoryPath.getShort(
					modelComponent.getCategoryId(), database);
		}
		return categoryPath;
	}

}
