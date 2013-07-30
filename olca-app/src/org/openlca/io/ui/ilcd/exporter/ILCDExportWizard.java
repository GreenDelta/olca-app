/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.io.ui.ilcd.exporter;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.core.application.db.Database;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.io.ilcd.ILCDExport;
import org.openlca.io.ui.SelectObjectsExportPage;

/**
 * Wizard for exporting processes, flows, flow properties and unit group to the
 * ILCD format
 * 
 * @author Sebastian Greve
 * @author Michael Srocka
 * 
 */
public class ILCDExportWizard extends Wizard implements IExportWizard {

	private SelectObjectsExportPage exportPage;
	private List<BaseDescriptor> components;
	private final ModelType type;

	public ILCDExportWizard(ModelType type) {
		setNeedsProgressMonitor(true);
		this.type = type;
	}

	protected void setComponents(List<BaseDescriptor> components) {
		this.components = components;
	}

	@Override
	public void addPages() {
		if (components != null)
			exportPage = SelectObjectsExportPage.withoutSelection(type);
		else
			exportPage = SelectObjectsExportPage.withSelection(type);
		exportPage.setSubDirectory("ILCD");
		addPage(exportPage);
	}

	@Override
	public void init(final IWorkbench workbench,
			final IStructuredSelection selection) {
		setWindowTitle(Messages.ILCDExportWizard_WindowTitle);
	}

	@Override
	public boolean performFinish() {
		final IDatabase database = Database.get();
		if (database == null)
			// TODO: show error message
			return false;
		// test the export parameters
		// the target directory
		final File targetDir = exportPage.getExportDestination();
		if (targetDir == null || !targetDir.isDirectory()) {
			// TODO: show error message
			return false;
		}

		// the components to be exported
		final List<BaseDescriptor> components = this.components != null ? this.components
				: exportPage.getSelectedModelComponents();
		if (components == null || components.size() == 0) {
			// TODO: show error message
			return false;
		}

		// run the export
		boolean errorOccured = false;
		try {

			// create the runnable
			IRunnableWithProgress runnable = new IRunnableWithProgress() {

				@Override
				public void run(final IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.beginTask("ILCD Export", components.size());
					int worked = 0;
					ILCDExport export = new ILCDExport(targetDir);
					for (BaseDescriptor descriptor : components) {
						if (monitor.isCanceled())
							break;
						monitor.setTaskName(descriptor.getName());
						try {
							Object component = database.createDao(
									descriptor.getModelType().getModelClass())
									.getForId(descriptor.getId());
							if (component instanceof CategorizedEntity)
								export.export((CategorizedEntity) component,
										database);
						} catch (Exception e) {
							throw new InvocationTargetException(e);
						} finally {
							monitor.worked(++worked);
						}
					}
					export.close();
				}
			};

			// run it in the wizard container
			getContainer().run(true, true, runnable);

		} catch (final Exception e) {
			errorOccured = true;
		}

		return !errorOccured;
	}
}
