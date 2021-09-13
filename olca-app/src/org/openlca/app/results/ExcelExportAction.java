package org.openlca.app.results;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.Editors;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Popup;
import org.openlca.io.xls.results.system.ResultExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelExportAction extends Action {

	private Logger log = LoggerFactory.getLogger(getClass());

	public ExcelExportAction() {
		setImageDescriptor(Images.descriptor(FileType.EXCEL));
		setText(M.ExportToExcel);
		setToolTipText(M.ExportToExcel);
	}

	@Override
	public void run() {
		ResultEditor<?> editor = Editors.getActive();
		if (editor == null) {
			log.error("unexpected error: the product system editor is not active");
			return;
		}
		runExport(editor);
	}

	private void runExport(ResultEditor<?> editor) {
		String fileName = Labels.name(editor.setup.target())
				.replaceAll("[^A-Za-z0-9]", "_") + ".xlsx";
		var file = FileChooser.forSavingFile(M.Export, fileName);
		if (file == null)
			return;
		ResultExport export = new ResultExport(editor.setup,
				editor.result, file, Cache.getEntityCache());
		export.setDQResult(editor.dqResult);
		App.run(M.Export, export, () -> {
			if (export.doneWithSuccess()) {
				Popup.info(M.ExportDone);
			}
		});
	}
}
