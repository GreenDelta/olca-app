package org.openlca.app.results;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.editors.Editors;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Info;
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
		IResultEditor<?> editor = Editors.getActive();
		if (editor == null) {
			log.error("unexpected error: the product system editor is not active");
			return;
		}
		runExport(editor);
	}

	private void runExport(IResultEditor<?> editor) {
		String fileName = editor.getSetup().productSystem.getName();
		fileName = fileName.replaceAll("[^A-Za-z0-9]", "_") + ".xlsx";
		File file = FileChooser.forExport("*.xlsx", fileName);
		if (file == null)
			return;
		ResultExport export = new ResultExport(editor.getSetup(),
				editor.getResult(), file);
		export.setDQResult(editor.getDqResult());
		App.run(M.Export, export, () -> {
			if (export.doneWithSuccess()) {
				Info.popup(M.ExportDone);
			}
		});
	}

}
