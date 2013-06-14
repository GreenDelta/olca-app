package org.openlca.ilcd.network.rcp.ui;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.modelprovider.IModelComponent;

public class ExportTupel {

	private IModelComponent model;
	private IDatabase database;

	public ExportTupel() {
	}

	public ExportTupel(IModelComponent model, IDatabase database) {
		this.model = model;
		this.database = database;
	}

	public IModelComponent getModel() {
		return model;
	}

	public void setModel(IModelComponent model) {
		this.model = model;
	}

	public IDatabase getDatabase() {
		return database;
	}

	public void setDatabase(IDatabase database) {
		this.database = database;
	}

}
