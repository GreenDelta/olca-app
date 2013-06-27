package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IAction;
import org.openlca.core.application.actions.IExportAction;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.RootEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A class for loading available export actions from the extension registry. */
class ExportActionProvider {

	private Logger log = LoggerFactory.getLogger(getClass());
	private final String EXTENSION_POINT = "org.openlca.core.application.exportAction";

	List<IAction> getFor(RootEntity model, IDatabase database) {
		if (model == null || database == null)
			return Collections.emptyList();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry
				.getConfigurationElementsFor(EXTENSION_POINT);
		List<IAction> actions = new ArrayList<>();
		String modelClass = model.getClass().getCanonicalName();
		for (IConfigurationElement element : elements) {
			if (!isForClass(element, modelClass))
				continue;
			IExportAction action = createAction(element, model, database);
			if (action != null)
				actions.add(action);
		}
		log.trace("{} export actions found", actions.size());
		return actions;
	}

	private IExportAction createAction(IConfigurationElement element,
			RootEntity model, IDatabase database) {
		try {
			IExportAction action = (IExportAction) element
					.createExecutableExtension("exportAction");
			action.setDatabase(database);
			action.setComponent(model);
			log.trace("Export action {} created for {}", action, model);
			return action;
		} catch (Exception e) {
			log.error("Could not create export action for " + model, e);
			return null;
		}
	}

	private boolean isForClass(IConfigurationElement element, String modelClass) {
		IConfigurationElement[] children = element.getChildren("allowedClass");
		for (IConfigurationElement child : children) {
			String className = child.getAttribute("className");
			if (className.equals(modelClass))
				return true;
		}
		return false;
	}

}
