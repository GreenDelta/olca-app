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
import org.openlca.io.xls.results.IExcelExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExcelExportAction<T extends IResultEditor<?>> extends Action {

	private Logger log = LoggerFactory.getLogger(getClass());

	public ExcelExportAction() {
		setImageDescriptor(Images.descriptor(FileType.EXCEL));
		setText(M.ExportToExcel);
		setToolTipText(M.ExportToExcel);
	}

	@Override
	public void run() {
		T editor = Editors.getActive();
		if (editor == null) {
			log.error(
					"unexpected error: the product system editor is not active");
			return;
		}
		runExport(editor);
	}

	protected abstract IExcelExport createExport(T editor);

	protected abstract String getDefaultFilename();

	private void runExport(T editor) {
		File file = FileChooser.forExport("*.xlsx", getDefaultFilename());
		if (file == null)
			return;
		IExcelExport export = createExport(editor);
		export.setFile(file);
		App.run(M.Export, export, new Runnable() {
			@Override
			public void run() {
				if (export.doneWithSuccess()) {
					InformationPopup.show(M.ExportDone);
				}
			}
		});
	}

}
