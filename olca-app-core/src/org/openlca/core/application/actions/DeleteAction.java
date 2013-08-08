/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.ModelEditorInput;
import org.openlca.core.application.wizards.DeleteWizard;
import org.openlca.core.database.DataProviderException;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.AdminInfo;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ModelingAndValidation;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Technology;
import org.openlca.core.model.Time;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.core.model.referencesearch.ReferenceSearcherRegistry;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This action deletes the specified model components
 * 
 * @author Sebastian Greve
 * 
 */
public class DeleteAction extends NavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public static final String ID = "org.openlca.core.application.NavigationView.DeleteAction";
	private final IDatabase[] databases;
	private final IModelComponent[] modelComponents;

	/**
	 * Creates a new instance for deleting a single model component
	 * 
	 * @param database
	 *            The database to access the model component
	 * @param modelComponent
	 *            The model component to be deleted
	 */
	public DeleteAction(final IDatabase database,
			final IModelComponent modelComponent) {
		this.databases = new IDatabase[] { database };
		this.modelComponents = new IModelComponent[] { modelComponent };
		setId(ID);
		setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
		setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
				.getDescriptor());
	}

	/**
	 * Creates a new instance for deleting multiple model components
	 * 
	 * @param database
	 *            The database to access the model components
	 * @param modelComponents
	 *            The model copmonents to be deleted
	 */
	public DeleteAction(final IDatabase database,
			final IModelComponent[] modelComponents) {
		databases = new IDatabase[modelComponents.length];
		for (int i = 0; i < modelComponents.length; i++) {
			databases[i] = database;
		}
		this.modelComponents = modelComponents;
		setId(ID);
		setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
		setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
				.getDescriptor());
	}

	/**
	 * Creates a new instance for deleting multiple model components
	 * 
	 * @param databases
	 *            The databases to access the model components (index must match
	 *            the index in the model component array)
	 * @param modelComponents
	 *            The model components to be deleted
	 */
	public DeleteAction(final IDatabase[] databases,
			final IModelComponent[] modelComponents) {
		this.databases = databases;
		this.modelComponents = modelComponents;
		setId(ID);
		setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
		setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
				.getDescriptor());
	}

	/**
	 * Creates a new message dialog for asking the user if he wants to delete a
	 * given model component
	 * 
	 * @param object
	 *            The model component to be deleted
	 * @return The created message dialog
	 */
	private MessageDialog createMessageDialog(final IModelComponent object) {
		return new MessageDialog(UI.shell(), Messages.Common_Delete, null,
				NLS.bind(Messages.NavigationView_DeleteQuestion,
						object.getName()), MessageDialog.QUESTION,
				new String[] { Messages.NavigationView_YesButton,
						Messages.NavigationView_YesAllButton,
						Messages.NavigationView_NoButton,
						Messages.NavigationView_CancelButton }, 2) {

			@Override
			protected void createButtonsForButtonBar(final Composite parent) {
				int i = 0;
				for (final String s : getButtonLabels()) {
					final Button b = createButton(parent, i, s,
							i == getDefaultButtonIndex());
					if (i == getDefaultButtonIndex()) {
						b.setFocus();
					}
					i++;
				}
			}

		};
	}

	/**
	 * Deletes the given object
	 * 
	 * @param object
	 *            The object to be deleted
	 * @param database
	 *            The database to access the object on the data provider
	 * @throws Exception
	 */
	private void deleteObject(IModelComponent object, final IDatabase database)
			throws Exception {
		if (object instanceof Process) {
			final Process process = database.select(Process.class,
					((Process) object).getId());
			deleteProcessInformation(process, database);
			object = process;
		} else if (object instanceof ProductSystem) {
			deleteProductSystem((ProductSystem) object, database);
		}
		database.delete(object);
	}

	/**
	 * Deletes the additional information of the given process
	 * 
	 * @param process
	 *            The process that will be deleted
	 * @param database
	 *            The database to access the process and its additional
	 *            information
	 * @throws DataProviderException
	 */
	private void deleteProcessInformation(final Process process,
			final IDatabase database) throws DataProviderException {
		// delete admin info
		final AdminInfo adminInfo = database.select(AdminInfo.class,
				process.getId());
		if (adminInfo != null) {
			database.delete(adminInfo);
		}

		// delete modeling and validation
		final ModelingAndValidation modelingAndValidation = database.select(
				ModelingAndValidation.class, process.getId());
		if (modelingAndValidation != null) {
			database.delete(modelingAndValidation);
		}

		// delete time
		final Time time = database.select(Time.class, process.getId());
		if (time != null) {
			database.delete(time);
		}

		// delete technology
		final Technology technology = database.select(Technology.class,
				process.getId());
		if (technology != null) {
			database.delete(technology);
		}

		// delete allocation factors
		for (final Exchange exchange : process.getExchanges()) {
			for (final AllocationFactor factor : exchange
					.getAllocationFactors()) {
				database.delete(factor);
			}
		}
	}

	/**
	 * Deletes the LCI result and it's uncertainty distributions of the given
	 * product system
	 * 
	 * @param productSystem
	 *            The product system that will be deleted
	 * @param database
	 *            The database to access the LCI result
	 * @throws DataProviderException
	 */
	private void deleteProductSystem(final ProductSystem productSystem,
			final IDatabase database) throws DataProviderException {
		// TODO implementation
	}

	@Override
	protected String getTaskName() {
		return null;
	}

	@Override
	public String getText() {
		return Messages.Common_Delete;
	}

	@Override
	public void task() {
		boolean deleteAll = false;
		for (int i = 0; i < modelComponents.length; i++) {
			final IModelComponent modelComponent = modelComponents[i];
			final IDatabase database = databases[i];
			boolean canDelete = deleteAll;
			if (!canDelete) {
				// ask the user if he really wants to delete the component
				final MessageDialog dialog = createMessageDialog(modelComponent);
				final int returnCode = dialog.open();

				// user answered yes or yes, all
				canDelete = returnCode == 0 || returnCode == 1;
				// user answered yes, all
				deleteAll = returnCode == 1;
			}
			if (canDelete) {
				// Check for problems with the deletion
				final DeleteWizard wizard = new DeleteWizard(database,
						ReferenceSearcherRegistry.getRegistry().getSearcher(
								modelComponent.getClass().getCanonicalName()),
						modelComponent);
				if (wizard.hasProblems()) {
					final WizardDialog dialog = new WizardDialog(UI.shell(),
							wizard);
					canDelete = dialog.open() == IDialogConstants.OK_ID;
				}
				if (canDelete) {
					try {
						// close editor if open
						final IEditorPart editor = PlatformUI
								.getWorkbench()
								.getActiveWorkbenchWindow()
								.getActivePage()
								.findEditor(
										new ModelEditorInput(Descriptors
												.toDescriptor(modelComponent),
												database));
						if (editor != null) {
							PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow().getActivePage()
									.closeEditor(editor, false);
						}

						// delete the object
						PlatformUI.getWorkbench().getProgressService()
								.busyCursorWhile(new IRunnableWithProgress() {

									@Override
									public void run(
											final IProgressMonitor monitor)
											throws InvocationTargetException,
											InterruptedException {
										monitor.beginTask(NLS.bind(
												Messages.Delete,
												modelComponent.getName()),
												IProgressMonitor.UNKNOWN);
										try {
											deleteObject(modelComponent,
													database);
										} catch (final Exception e) {
											throw new InterruptedException(e
													.getMessage());
										}
									}
								});
					} catch (final Exception e) {
						log.error("Delete failed", e);
					}
				}
			}
		}
	}
}
