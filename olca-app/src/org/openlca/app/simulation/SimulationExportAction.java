package org.openlca.app.simulation;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.resources.ImageType;
import org.openlca.core.editors.io.ui.FileChooser;
import org.openlca.core.results.SimulationResult;
import org.openlca.io.xls.results.SimulationResultExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SimulationExportAction extends Action {

	private Logger log = LoggerFactory.getLogger(getClass());
	private SimulationResult result;

	public SimulationExportAction() {
		setId("SimulationResultExport");
		setToolTipText("Export results to Excel");
		setImageDescriptor(ImageType.EXCEL_ICON.getDescriptor());
	}

	public void configure(SimulationResult result) {
		this.result = result;
	}

	@Override
	public void run() {
		final File file = FileChooser.forExport("xls", "simulation_result.xls");
		if (file == null)
			return;
		App.run("Export simulation result", new Runnable() {
			public void run() {
				tryRun(file);
			}
		});
	}

	private void tryRun(File file) {
		try {
			SimulationResultExport export = new SimulationResultExport(result,
					Database.getCache());
			export.run(file);
		} catch (Exception e) {
			log.error("Result export failed", e);
		}
	}
}
