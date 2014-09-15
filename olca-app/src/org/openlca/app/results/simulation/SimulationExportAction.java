package org.openlca.app.results.simulation;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.ImageType;
import org.openlca.core.results.SimulationResult;
import org.openlca.core.results.SimulationResultProvider;
import org.openlca.io.xls.results.SimulationResultExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SimulationExportAction extends Action {

	private Logger log = LoggerFactory.getLogger(getClass());
	private SimulationResult result;

	public SimulationExportAction() {
		setId("SimulationResultExport");
		setToolTipText(Messages.ExportResultsToExcel);
		setImageDescriptor(ImageType.FILE_EXCEL_SMALL.getDescriptor());
	}

	public void configure(SimulationResult result) {
		this.result = result;
	}

	@Override
	public void run() {
		final File file = FileChooser.forExport("*.xlsx",
				"simulation_result.xlsx");
		if (file == null)
			return;
		App.run(Messages.ExportResultsToExcel, new Runnable() {
			public void run() {
				tryRun(file);
			}
		});
	}

	private void tryRun(File file) {
		try {
			SimulationResultProvider<?> provider = new SimulationResultProvider<>(
					result, Cache.getEntityCache());
			SimulationResultExport export = new SimulationResultExport(provider);
			export.run(file);
		} catch (Exception e) {
			log.error("Result export failed", e);
		}
	}
}
