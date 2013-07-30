/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.io.ecospold1;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.db.Database;
import org.openlca.app.io.SelectObjectsExportPage;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.io.ecospold1.exporter.EcoSpold01Outputter;
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
	private final ModelType type;
	private List<BaseDescriptor> components;

	public EcoSpold01ExportWizard(ModelType type) {
		super();
		setNeedsProgressMonitor(true);
		this.type = type;
	}

	public final void setComponents(List<BaseDescriptor> components) {
		this.components = components;
	}

	@Override
	public void addPages() {
		if (components != null)
			exportPage = SelectObjectsExportPage.withoutSelection(type);
		else
			exportPage = SelectObjectsExportPage.withSelection(type);
		exportPage.setSubDirectory("EcoSpold1");
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
		if (components == null) {
			components = exportPage.getSelectedModelComponents();
		}
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					// set up
					int objectAmount = components.size();
					monitor.beginTask(Messages.Exporting, objectAmount + 1);
					monitor.subTask(Messages.CreatingFolder);
					EcoSpold01Outputter outputter = new EcoSpold01Outputter(
							exportPage.getExportDestination());
					monitor.worked(1);

					try {
						for (BaseDescriptor descriptor : components) {
							if (!monitor.isCanceled()) {
								monitor.subTask(descriptor.getName());
								if (type == ModelType.PROCESS) {
									Process process = new ProcessDao(Database
											.get()).getForId(descriptor.getId());
									outputter.exportProcess(process);
								} else if (type == ModelType.IMPACT_METHOD) {
									ImpactMethod method = new ImpactMethodDao(
											Database.get()).getForId(descriptor
											.getId());
									outputter.exportLCIAMethod(method);
								}
								monitor.worked(1);
							}
						}
					} catch (final Exception e) {
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
