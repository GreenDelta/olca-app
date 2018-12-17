package org.openlca.app.results.simulation;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.FileType;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.results.SimulationResult;
import org.openlca.io.xls.results.SimulationResultExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SimulationExportAction extends Action {

	private Logger log = LoggerFactory.getLogger(getClass());
	private SimulationResult result;
	private CalculationSetup setup;

	public SimulationExportAction(SimulationResult result,
			CalculationSetup setup) {
		setId("SimulationResultExport");
		setToolTipText(M.ExportResultsToExcel);
		setImageDescriptor(Images.descriptor(FileType.EXCEL));
		this.result = result;
		this.setup = setup;
	}

	@Override
	public void run() {
		File file = FileChooser.forExport("*.xlsx", "simulation_result.xlsx");
		if (file == null)
			return;
		App.run(M.ExportResultsToExcel, () -> {
			try {
				SimulationResultExport export = new SimulationResultExport(
						setup, result, Cache.getEntityCache());
				export.run(file);
			} catch (Exception e) {
				log.error("Result export failed", e);
			}
		});
	}
}
