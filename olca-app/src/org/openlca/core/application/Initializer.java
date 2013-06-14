package org.openlca.core.application;

import java.io.File;
import java.util.Locale;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.openlca.core.application.plugin.Activator;
import org.openlca.core.application.preferencepages.Language;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.core.model.modelprovider.ModelComponentRegistry;
import org.openlca.core.model.referencesearch.IReferenceSearcher;
import org.openlca.core.model.referencesearch.ReferenceSearcherRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes the extension points of the plugins
 * 
 * @author Sebastian Greve
 * 
 */
public class Initializer {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * Initializes the model components
	 */
	private void initializeModelComponents() {
		// get model component extensions
		final IExtensionRegistry extensionRegistry = Platform
				.getExtensionRegistry();
		final IConfigurationElement[] elements = extensionRegistry
				.getConfigurationElementsFor("org.openlca.core.model.components");
		try {
			// for each configuration element found
			for (final IConfigurationElement element : elements) {
				Class<? extends IModelComponent> clazz = element
						.createExecutableExtension("class").getClass()
						.asSubclass(IModelComponent.class);
				if (clazz != null && element.getChildren("category").length > 0) {
					IConfigurationElement catElement = element
							.getChildren("category")[0];
					ModelComponentRegistry.getRegistry()
							.registerModelComponent(
									clazz,
									catElement.getAttribute("name"),
									Integer.parseInt(catElement
											.getAttribute("level")));
				}
				// if reference searcher is defined
				if (clazz != null
						&& element.getChildren("referenceSearcher").length > 0) {
					final Object handlerElement = element
							.createExecutableExtension("referenceSearcher");
					if (handlerElement instanceof IReferenceSearcher<?>) {
						// register reference searcher
						ReferenceSearcherRegistry.getRegistry()
								.registerSearcher(clazz.getCanonicalName(),
										(IReferenceSearcher<?>) handlerElement);
					}
				}
			}
		} catch (final Exception e) {
			log.error("Initializing model components failed", e);
		}
	}

	/**
	 * Initializes the locale
	 */
	private void initializeLocale() {
		Locale locale = Locale.ENGLISH;

		switch (Language
				.getLanguage(ApplicationProperties.PROP_NATIONAL_LANGUAGE
						.getValue())) {
		case ENGLISH:
			locale = Locale.ENGLISH;
			break;
		case GERMAN:
			locale = Locale.GERMAN;
			break;
		}
		Locale.setDefault(locale);
	}

	/**
	 * Initializes the model components and calculators
	 */
	public void initialize() {
		initializeLocale();
		initializeModelComponents();
//		// set default image
//		Window.setDefaultImage(				
//				
//				AbstractUIPlugin
//				.imageDescriptorFromPlugin(
//						Activator.PLUGIN_ID,
//						File.separator + "icons" + File.separator
//								+ "logo_16_32bit.png").createImage());
	}
}
