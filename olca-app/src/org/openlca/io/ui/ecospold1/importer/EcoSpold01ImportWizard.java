/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.io.ui.ecospold1.importer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.openlca.core.application.navigation.NavigationRoot;
import org.openlca.core.application.views.navigator.Navigator;
import org.openlca.core.database.DataProviderException;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.model.Process;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.resources.ImageType;
import org.openlca.io.EcoSpoldUnitFetch;
import org.openlca.io.ParserException;
import org.openlca.io.UnitMapping;
import org.openlca.io.UnitMappingEntry;
import org.openlca.io.ecospold1.importer.EcoSpold01Parser;
import org.openlca.io.ui.FileImportPage;
import org.openlca.io.ui.SelectDatabasePage;
import org.openlca.io.ui.UnitMappingPage;
import org.openlca.ui.SelectCategoryDialog;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import wizard for importing a set of EcoSpold01 formatted files
 * 
 * @author Sebastian Greve
 * 
 */
public class EcoSpold01ImportWizard extends Wizard implements IImportWizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category category;
	private SelectDatabasePage databasePage;
	private FileImportPage importPage;
	private UnitMappingPage mappingPage;
	private Namespace methodNameSpace = Namespace
			.getNamespace("http://www.EcoInvent.org/EcoSpold01Impact");
	private final Namespace processNameSpace = Namespace
			.getNamespace("http://www.EcoInvent.org/EcoSpold01");
	private IDatabase database;

	public EcoSpold01ImportWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	public EcoSpold01ImportWizard(IDatabase database) {
		super();
		setNeedsProgressMonitor(true);
		this.database = database;
	}

	/**
	 * Changes the name space of a document into the process name space
	 * 
	 * @param doc
	 *            The document to change the name space in
	 * @return true if the name space was changed, false if it already was the
	 *         process name space
	 */
	private boolean changeNameSpace(final Document doc) {
		final boolean process = doc.getRootElement().getNamespace()
				.equals(processNameSpace);

		// if LCIA method data set
		if (!process
				&& doc.getRootElement().getNamespace().equals(methodNameSpace)) {
			final Queue<Element> queue = new LinkedList<>();
			queue.add(doc.getRootElement());
			// while more elements
			while (!queue.isEmpty()) {
				// set process name space
				final Element e = queue.poll();
				e.setNamespace(processNameSpace);
				// add each child element to queue
				for (final Object o : e.getChildren()) {
					if (o instanceof Element) {
						queue.add((Element) o);
					}
				}
			}
		}

		return process;
	}

	/**
	 * Reads out the xml files length
	 * 
	 * @param files
	 *            The files to import
	 * @return The accumulated length of all files
	 */
	private long getAmountOfJobs(final File[] files) {
		long sizeOfXmlFiles = 0;
		// for each file
		for (final File file : files) {
			// if xml file
			if (file.getName().toLowerCase().endsWith(".xml")) { //$NON-NLS-1$
				// add length of file
				sizeOfXmlFiles += file.length();
			} else {
				// else it is a zip file
				try (final ZipFile zipFile = new ZipFile(file)) {
					final Enumeration<? extends ZipEntry> entries = zipFile
							.entries();
					// while more entries
					while (entries.hasMoreElements()) {
						final ZipEntry entry = entries.nextElement();
						// if xml file
						if (!entry.isDirectory()
								&& entry.getName().toLowerCase()
										.endsWith(".xml")) { //$NON-NLS-1$
							// add length of file
							sizeOfXmlFiles += entry.getSize();
						}
					}
				} catch (final Exception e) {
					log.error("Reading XML files failed", e);
				}
			}
		}
		return sizeOfXmlFiles;
	}

	/**
	 * Looks up the files if they contain any process
	 * 
	 * @param files
	 *            The files to look up
	 * @return true if at least one process is within the files
	 */
	private boolean hasProcesses(final File[] files) {
		final SAXBuilder builder = new SAXBuilder(false);
		boolean hasProcess = false;
		try {
			// for each file
			for (final File file : files) {
				// if xml file
				if (file.getAbsolutePath().toLowerCase().endsWith(".xml")) { //$NON-NLS-1$
					// build document
					final Document doc = builder.build(file);
					hasProcess = doc.getRootElement().getNamespace()
							.equals(processNameSpace);
				} else if (file.getAbsolutePath().toLowerCase()
						.endsWith(".zip")) { //$NON-NLS-1$
					// else if zip file
					try (ZipFile zipFile = new ZipFile(file)) {
						final Enumeration<? extends ZipEntry> entries = zipFile
								.entries();
						// while more elements
						while (entries.hasMoreElements()) {
							final ZipEntry entry = entries.nextElement();
							// if xml file
							if (!entry.isDirectory()
									&& entry.getName().toLowerCase()
											.endsWith(".xml")) { //$NON-NLS-1$
								// build document
								final Document doc = builder.build(zipFile
										.getInputStream(entry));
								hasProcess = doc.getRootElement()
										.getNamespace()
										.equals(processNameSpace);
								if (hasProcess) {
									break;
								}
							}
						}
					}
				}
				if (hasProcess) {
					break;
				}
			}
		} catch (final Exception e) {
			log.error("Look up for the files failed", e);
		}
		return hasProcess;
	}

	/**
	 * Parses the given files
	 * 
	 * @param monitor
	 *            The progress monitor
	 * @param files
	 *            The files to parse
	 * @param unitMapping
	 *            The unit mapping
	 * @throws ParserException
	 *             If any error occurs while parsing
	 */
	private void parse(final IProgressMonitor monitor, final File[] files,
			final UnitMapping unitMapping) throws ParserException {
		// set up
		EcoSpold01Parser processParser = new EcoSpold01Parser(category,
				database, unitMapping);
		final long sizeOfXmlFiles = getAmountOfJobs(files);
		monitor.beginTask(Messages.EcoSpoldImportWizard_Importing,
				(int) sizeOfXmlFiles);
		final SAXBuilder builder = new SAXBuilder(false);
		int i = 0;

		// while more files and not canceled
		while (!monitor.isCanceled() && i < files.length) {
			final File file = files[i];
			try {
				// if zip file
				if (file.getName().endsWith(".zip")) {
					try (ZipFile zipFile = new ZipFile(file)) {
						final Enumeration<? extends ZipEntry> entries = zipFile
								.entries();
						// while more elements and not canceled
						while (!monitor.isCanceled()
								&& entries.hasMoreElements()) {
							final ZipEntry entry = entries.nextElement();
							// if xml file
							if (!entry.isDirectory()
									&& entry.getName().toLowerCase()
											.endsWith(".xml")) {
								monitor.subTask(entry.getName());
								// build document
								final Document doc = builder.build(zipFile
										.getInputStream(entry));
								// if process or LCIA method
								if (doc.getRootElement().getNamespace()
										.equals(processNameSpace)
										|| doc.getRootElement().getNamespace()
												.equals(methodNameSpace)) {
									// parse
									processParser.parse(zipFile, entry,
											changeNameSpace(doc));
								}
							}
							monitor.worked((int) entry.getSize());
						}
					}
				} else {
					monitor.subTask(file.getName());
					final Document doc = builder.build(file);
					// if process or LCIA method
					if (doc.getRootElement().getNamespace()
							.equals(processNameSpace)
							|| doc.getRootElement().getNamespace()
									.equals(methodNameSpace)) {
						// parse
						processParser.parse(file, changeNameSpace(doc));
					}
					monitor.worked((int) file.length());
				}
			} catch (final Exception e) {
				log.error("Parsing files failed", e);
			}
			i++;
		}
		// clear cache
		monitor.done();
	}

	@Override
	public void addPages() {
		if (database == null) {
			// database page
			// import from wizard (no database selected)
			databasePage = new SelectDatabasePage();
			addPage(databasePage);
		}

		// file import page
		importPage = new FileImportPage(new String[] { "zip", "xml" }, true);
		addPage(importPage);

		// unit mapping page
		mappingPage = new UnitMappingPage() {

			@Override
			protected String[] checkFiles(final File[] files) {
				String[] unitNames;
				final EcoSpoldUnitFetch unitChecker = new EcoSpoldUnitFetch();
				try {
					unitNames = unitChecker.getUnits(files);
				} catch (Exception e) {
					log.error("Failed to get the units from files.");
					unitNames = new String[0];
				}
				return unitNames;
			}

			@Override
			protected File[] getFiles() {
				return EcoSpold01ImportWizard.this.getFiles();
			}

		};
		addPage(mappingPage);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (importPage != null) {
			importPage.dispose();
		}
		if (mappingPage != null) {
			mappingPage.dispose();
		}
	}

	/**
	 * Getter of the files
	 * 
	 * @return The files selected on the file import page
	 */
	public File[] getFiles() {
		return importPage.getFiles();
	}

	/**
	 * Getter of the database
	 * 
	 * @return The database
	 */
	public IDatabase getDatabase() {
		IDatabase database = null;
		if (databasePage != null) {
			// import from workbench import action
			database = databasePage.getDatabase();
		} else {
			// import from navigation
			database = this.database;
		}
		return database;
	}

	@Override
	public void init(final IWorkbench workbench,
			final IStructuredSelection selection) {
		setWindowTitle(Messages.EcoSpoldImportWizard_WindowTitle);
		setDefaultPageImageDescriptor(ImageType.IMPORT_ZIP_WIZARD
				.getDescriptor());
	}

	@Override
	public boolean performFinish() {
		boolean error = false;
		if (database == null) {
			// import from workbench import action
			database = databasePage.getDatabase();
		}
		final File[] files = importPage.getFiles();
		final UnitMapping unitMapping = mappingPage.getUnitMapping();
		saveUnits(unitMapping);
		boolean abort = true;

		// if files contain process data set
		if (hasProcesses(files)) {
			// get navigation root
			NavigationRoot root = null;
			final Navigator navigator = (Navigator) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.findView(Navigator.ID);
			if (navigator != null) {
				root = navigator.getRoot();
			}

			// open category dialog
			final SelectCategoryDialog dialog = new SelectCategoryDialog(
					UI.shell(), Messages.EcoSpold01ImportWizard_SelectCategory,
					Process.class, database, root);
			if (dialog.open() == Window.OK) {
				// get selected category
				category = dialog.getSelectedCategory();
				abort = false;
			}
		} else {
			abort = false;
		}
		if (!abort) {
			// parse with progress monitor
			try {
				getContainer().run(true, true, new IRunnableWithProgress() {

					@Override
					public void run(final IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						try {
							// parse
							parse(monitor, files, unitMapping);
						} catch (final ParserException e) {
							throw new InterruptedException(e.getMessage());
						}
					}

				});
			} catch (final Exception e) {
				error = true;
			}

		}

		Navigator.refresh();

		return !error && !abort;
	}

	private void saveUnits(UnitMapping unitMapping) {
		for (String unitName : unitMapping.getUnits()) {
			UnitMappingEntry entry = unitMapping.getEntry(unitName);
			UnitGroup unitGroup = entry.getUnitGroup();
			if (unitGroup.getUnit(unitName) == null) {
				Unit unit = new Unit(UUID.randomUUID().toString(), unitName);
				unit.setConversionFactor(unitMapping
						.getConversionFactor(unitName));
				unitGroup.add(unit);
				try {
					database.refresh(unitGroup);
				} catch (final DataProviderException e) {
					log.error("Update unit group failed", e);
				}
			}
		}
	}
}
