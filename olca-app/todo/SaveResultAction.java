package org.openlca.core.editors.result;

import org.eclipse.jface.action.Action;
import org.openlca.app.Messages;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Info;
import org.openlca.core.database.DataProviderException;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.results.LCIAResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An action for storing the impact assessment result. */
class SaveResultAction extends Action {

	private Logger log = LoggerFactory.getLogger(getClass());
	private LCIAResult result;
	private IDatabase database;

	public SaveResultAction() {
		setImageDescriptor(ImageType.LCIA_CATEGORY_ICON.getDescriptor());
		setDisabledImageDescriptor(ImageType.LCIA_CATEGORY_ICON_DIS
				.getDescriptor());
		setToolTipText(Messages.SaveImpactResult);
		setEnabled(false);
	}

	public void setData(LCIAResult result, IDatabase database) {
		this.result = result;
		this.database = database;
		boolean canExec = result != null && database != null;
		setEnabled(canExec);
	}

	@Override
	public void run() {
		try {
			if (contains())
				log.trace("result already stored");
			else {
				log.trace("insert result");
				result.setCategoryId(LCIAResult.class.getCanonicalName());
				database.insert(result);
				Info.showBox(Messages.ImpactResultSavedTitle,
						Messages.ImpactResultSavedMessage);
				Navigator.refresh();
			}
		} catch (DataProviderException e) {
			log.error("Save result failed", e);
		}
	}

	private boolean contains() throws DataProviderException {
		return database.select(LCIAResult.class, result.getId()) != null;
	}
}
