package org.openlca.app.wizards.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.csv.output.CSVExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSVExportWizard extends Wizard implements IExportWizard {

	private Logger log = LoggerFactory.getLogger(getClass());
	private SelectObjectsExportPage exportPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("SimaPro CSV Export");
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		File exportFile = exportPage.getExportDestination();
		exportFile = new File(exportFile + File.separator + "SimaPro CSV");
		if (!exportFile.exists())
			exportFile.mkdir();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat timeFormat = new SimpleDateFormat("hh-mm-ss");
		StringBuilder builder = new StringBuilder();
		builder.append(exportFile.getAbsolutePath());
		builder.append(File.separator);
		builder.append("openLCA_export_");
		builder.append(dateFormat.format(new Date()));
		builder.append('_');
		builder.append(timeFormat.format(new Date()));
		builder.append(".csv");
		exportFile = new File(builder.toString());
		if (exportFile.exists()) {
			String newFileName = exportFile.getAbsolutePath().toString();
			exportFile = new File(newFileName.substring(0,
					newFileName.indexOf(".csv"))
					+ "_" + Calendar.getInstance().getTimeInMillis() + ".csv");
		}
		List<BaseDescriptor> selection = exportPage
				.getSelectedModelComponents();
		List<ProcessDescriptor> processes = new ArrayList<>();
		for (BaseDescriptor descriptor : selection) {
			if (descriptor instanceof ProcessDescriptor)
				processes.add((ProcessDescriptor) descriptor);
		}

		try {
			final CSVExporter exporter = new CSVExporter(Database.get(),
					exportFile, ';', processes);
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(final IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Export", IProgressMonitor.UNKNOWN);
					exporter.run();
				}
			});
			Navigator.refresh();
		} catch (Exception e) {
			log.error("SimaPro CSV export failed", e);
		}
		return true;
	}

	@Override
	public void addPages() {
		exportPage = SelectObjectsExportPage.withSelection(ModelType.PROCESS);
		exportPage.setSubDirectory("SimaPro CSV");
		addPage(exportPage);
	}

}
