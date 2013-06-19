/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.ModelEditorInput;
import org.openlca.core.application.views.navigator.Navigator;
import org.openlca.core.database.DataProviderException;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract form editor form model components
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class ModelEditor extends FormEditor implements
		PropertyChangeListener {

	protected Logger log = LoggerFactory.getLogger(this.getClass());

	private IDatabase database;
	private boolean dirty = false;
	private IModelComponent modelComponent;
	private ModelEditorPage[] pages = new ModelEditorPage[0];

	private List<IEditorComponent> editorComponents = new ArrayList<>();

	@Override
	protected void addPages() {
		pages = initPages();
		try {
			for (final ModelEditorPage page : pages) {
				addPage(page);
			}
			appendExtendedPages();
		} catch (final PartInitException e) {
			log.error("Add pages failed", e);
		}
	}

	private void appendExtendedPages() {
		final IExtensionRegistry extensionRegistry = Platform
				.getExtensionRegistry();
		final IConfigurationElement[] elements = extensionRegistry
				.getConfigurationElementsFor("org.openlca.core.editors.pages");
		log.info("Appending " + elements.length + " additional pages");
		try {
			// for each configuration element found
			for (final IConfigurationElement element : elements) {
				String editorClass = element.getAttribute("editorClass");
				log.info("Additional page for editor " + editorClass + " found");
				// if page is for current editor
				if (getClass().getCanonicalName().equals(editorClass)) {
					IEditorPageFactory pageFactory = (IEditorPageFactory) element
							.createExecutableExtension("pageFactory");
					FormPage page = pageFactory.createEditorPage(this);
					if (page != null) {
						addPage(page);
					}
				}
			}
		} catch (final Exception e) {
			log.error("Initializing extended pages failed", e);
		}
	}

	protected abstract ModelEditorPage[] initPages();

	protected void setDatabase(final IDatabase database) {
		this.database = database;
	}

	@Override
	public void dispose() {
		if (modelComponent != null)
			modelComponent.removePropertyChangeListener(this);
		super.dispose();
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {
		log.trace("Save {} to database.", modelComponent);
		try {
			PlatformUI.getWorkbench().getProgressService()
					.busyCursorWhile(new IRunnableWithProgress() {
						@Override
						public void run(final IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							monitor.beginTask(
									NLS.bind(Messages.Saving,
											modelComponent.getName()),
									IProgressMonitor.UNKNOWN);
							try {
								database.refresh(modelComponent);
							} catch (final DataProviderException e) {
								throw new InvocationTargetException(e);
							}
							monitor.done();
						}
					});
		} catch (final Exception e) {
			log.error("Error while saving model to database.", e);
		}

		setPartName(modelComponent.getName());
		for (final ModelEditorPage page : pages) {
			page.updateFormTitle();
		}
		dirty = false;
		editorDirtyStateChanged();
		fireSaved();
		Navigator.refresh();
	}

	@Override
	public void doSaveAs() {
	}

	public IDatabase getDatabase() {
		return database;
	}

	public IModelComponent getModelComponent() {
		return modelComponent;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		log.trace("open editor {}", input);
		super.init(site, input);
		setPartName(input.getName());
		try {
			ModelEditorInput modelInput = (ModelEditorInput) input;
			database = modelInput.getDatabase();
			BaseDescriptor descriptor = modelInput.getDescriptor();
			String id = descriptor.getId();
			Class<?> clazz = descriptor.getModelType().getModelClass();
			loadModelComponent(clazz, id, input.getName());
			modelComponent.addPropertyChangeListener(this);
		} catch (Exception e) {
			log.error("Failed to open model " + input, e);
			throw new PartInitException("Failed to open model", e);
		}
	}

	private void loadModelComponent(final Class<?> clazz, final String id,
			final String name) {
		log.trace("Load instance (type={}, id={}) from database", clazz, id);
		try {
			PlatformUI.getWorkbench().getProgressService()
					.busyCursorWhile(new IRunnableWithProgress() {
						@Override
						public void run(final IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							monitor.beginTask(NLS.bind(Messages.Loading, name),
									IProgressMonitor.UNKNOWN);
							try {
								modelComponent = (IModelComponent) database
										.createDao(clazz).getForId(id);
							} catch (Exception e) {
								throw new InvocationTargetException(e);
							}
							monitor.done();
						}
					});
		} catch (Exception e) {
			log.error("Error while loading model from database.", e);
		}
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	// TODO: use mark dirty instead
	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		if (!(evt.getSource() instanceof ProductSystem && evt.getPropertyName()
				.equals("lciaResult"))) {
			dirty = true;
			this.editorDirtyStateChanged();
		}
	}

	public void fireChange() {
		dirty = true;
		for (IEditorComponent component : editorComponents)
			component.onChange();
		editorDirtyStateChanged();
	}

	public void registerComponent(IEditorComponent component) {
		editorComponents.add(component);
	}

	void fireSaved() {
		for (IEditorComponent component : editorComponents)
			component.onSaved();
	}

}
