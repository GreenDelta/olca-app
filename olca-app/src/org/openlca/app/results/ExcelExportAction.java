package org.openlca.app.results;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Editors;
import org.openlca.app.util.FileType;
import org.openlca.app.util.InformationPopup;
import org.openlca.io.xls.results.system.ResultExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelExportAction extends Action {

	private Logger log = LoggerFactory.getLogger(getClass());
	private final String type;

	public ExcelExportAction(String type) {
		setImageDescriptor(Images.descriptor(FileType.EXCEL));
		setText(M.ExportToExcel);
		setToolTipText(M.ExportToExcel);
		this.type = type;
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
		File file = FileChooser.forExport("*.xlsx", toFileName(type));
		if (file == null)
			return;
		ResultExport export = new ResultExport(editor.getSetup(), editor.getResult(), editor.getDqResult(), type, file);
		App.run(M.Export, export, new Runnable() {
			@Override
			public void run() {
				if (export.doneWithSuccess()) {
					InformationPopup.show(M.ExportDone);
				}
			}
		});
	}

	private String toFileName(String type) {
		return type.toLowerCase().replace(' ', '_') + ".xlsx";
	}

}
