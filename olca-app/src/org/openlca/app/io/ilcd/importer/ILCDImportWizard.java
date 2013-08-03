/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.io.ilcd.importer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.Messages;
import org.openlca.app.components.ProgressAdapter;
import org.openlca.app.io.FileImportPage;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.resources.ImageType;
import org.openlca.core.database.IDatabase;
import org.openlca.io.ilcd.ILCDImport;

/**
 * Import wizard for importing a set of ILCD formatted files
 */
public class ILCDImportWizard extends Wizard implements IImportWizard {

	private FileImportPage importPage;
	private IDatabase database;

	public ILCDImportWizard() {
		super();
		setNeedsProgressMonitor(true);

	}

	public ILCDImportWizard(IDatabase database) {
		super();
		setNeedsProgressMonitor(true);
		this.database = database;
	}

	@Override
	public void addPages() {
		importPage = new FileImportPage(new String[] { "zip" }, false);
		addPage(importPage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(Messages.ILCDImportWizard_WindowTitle);
		setDefaultPageImageDescriptor(ImageType.IMPORT_ZIP_WIZARD
				.getDescriptor());
	}

	@Override
	public boolean performFinish() {

		// TODO: check arguments (category etc.)
		// TODO: show error message if error occurs

		final File zip = getZip();
		if (zip == null)
			return false;

		boolean error = false;

		try {
			getContainer().run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(final IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					ProgressAdapter adapter = new ProgressAdapter(monitor);
					ILCDImport iImport = new ILCDImport(zip, adapter, database);
					iImport.run();
				}

			});
		} catch (final Exception e) {
			error = true;
		}

		Navigator.refresh();

		return !error;
	}

	private File getZip() {
		File[] files = importPage.getFiles();
		if (files.length > 0)
			return files[0];
		return null;
	}
}
