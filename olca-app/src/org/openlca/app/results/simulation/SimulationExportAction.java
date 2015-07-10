package org.openlca.app.results.simulation;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.ImageType;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.results.SimulationResultProvider;
import org.openlca.io.xls.results.SimulationResultExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SimulationExportAction extends Action {

	private Logger log = LoggerFactory.getLogger(getClass());
	private SimulationResultProvider<?> result;
	private CalculationSetup setup;

	public SimulationExportAction(SimulationResultProvider<?> result,
			CalculationSetup setup) {
		setId("SimulationResultExport");
		setToolTipText(Messages.ExportResultsToExcel);
		setImageDescriptor(ImageType.FILE_EXCEL_SMALL.getDescriptor());
		this.result = result;
		this.setup = setup;
	}

	@Override
	public void run() {
		File file = FileChooser.forExport("*.xlsx", "simulation_result.xlsx");
		if (file == null)
			return;
		App.run(Messages.ExportResultsToExcel, () -> {
			try {
				SimulationResultExport export = new SimulationResultExport(
						setup, result);
				export.run(file);
			} catch (Exception e) {
				log.error("Result export failed", e);
			}
		});
	}
}
