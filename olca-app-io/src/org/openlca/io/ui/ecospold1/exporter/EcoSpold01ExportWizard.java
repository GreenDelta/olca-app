/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.io.ui.ecospold1.exporter;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.core.model.LCIAMethod;
import org.openlca.core.model.Process;
import org.openlca.io.ecospold1.exporter.EcoSpold01Outputter;
import org.openlca.io.ui.ObjectWrapper;
import org.openlca.io.ui.SelectObjectsExportPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wizard for exporting processes and LCIA methods to the EcoSpold01 format
 * 
 * @author Sebastian Greve
 * 
 */
public class EcoSpold01ExportWizard extends Wizard implements IExportWizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private SelectObjectsExportPage exportPage;
	private boolean singleExport = false;
	private final int type;

	private List<ObjectWrapper> components;
	
	public EcoSpold01ExportWizard(final int type) {
		super();
		setNeedsProgressMonitor(true);
		this.type = type;
	}

	protected final void setModelComponentsToExport(List<ObjectWrapper> components) {
		this.components = components;
	}

	
	protected final void setSingleExport(final boolean singleExport) {
		this.singleExport = singleExport;
	}

	@Override
	public void addPages() {
		exportPage = new SelectObjectsExportPage(singleExport, type, false,
				"EcoSpold1");
		addPage(exportPage);
	}

	@Override
	public void init(final IWorkbench workbench,
			final IStructuredSelection selection) {
		setWindowTitle(Messages.EcoSpoldExportWizard_WindowTitle);
	}

	@Override
	public boolean performFinish() {
		boolean errorOccured = false;
		if (!singleExport) {
			components = exportPage.getSelectedModelComponents();
		}
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(final IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					// set up
					int objectAmount = components.size();
					monitor.beginTask(Messages.Exporting, objectAmount + 1);
					monitor.subTask(Messages.CreatingFolder);
					final EcoSpold01Outputter outputter = new EcoSpold01Outputter(
							exportPage.getExportDestination());
					monitor.worked(1);

					try {
						switch (type) {
						case SelectObjectsExportPage.PROCESS:
							// for each component to export
							for (final ObjectWrapper wrapper : components) {
								if (!monitor.isCanceled()) {
									// load process
									final Process process = wrapper
											.getDatabase().select(
													Process.class,
													wrapper.getModelComponent()
															.getId());
									monitor.subTask(process.getName());
									// export
									outputter.exportProcess(process,
											wrapper.getDatabase());
									monitor.worked(1);
								}
							}
							break;
						case SelectObjectsExportPage.METHOD:
							// for each component to export
							for (final ObjectWrapper wrapper : components) {
								if (!monitor.isCanceled()) {
									// load method
									final LCIAMethod method = wrapper
											.getDatabase().select(
													LCIAMethod.class,
													wrapper.getModelComponent()
															.getId());
									monitor.subTask(method.getName());
									// export
									outputter.exportLCIAMethod(method,
											wrapper.getDatabase());
									monitor.worked(1);
								}
							}
							break;
						}
						// clear cache
						outputter.clearGlobalCache();
					} catch (final Exception e) {
						// TODO: handle exception
						log.error("Perform finish failed", e);
						throw new InterruptedException(e.getMessage());
					}
					monitor.done();
				}
			});
		} catch (final Exception e) {
			// TODO: handle exception
			log.error("Perform finish failed", e);
			errorOccured = true;
		}
		return !errorOccured;
	}

}
