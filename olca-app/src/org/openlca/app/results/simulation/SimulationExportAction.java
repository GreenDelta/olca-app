package org.openlca.app.results.simulation;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.AppContext;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.FileType;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.results.SimulationResult;
import org.openlca.io.xls.results.SimulationResultExport;

class SimulationExportAction extends Action {

	private final SimulationResult result;
	private final CalculationSetup setup;

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
		var file = FileChooser.forSavingFile(M.Export, "simulation_result.xlsx");
		if (file == null)
			return;
		App.exec(M.ExportResultsToExcel, () -> {
			try {
				var export = new SimulationResultExport(
						setup, result, AppContext.getEntityCache());
				export.run(file);
			} catch (Exception e) {
				ErrorReporter.on("Result export failed", e);
			}
		});
	}
}
