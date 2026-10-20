package org.openlca.app.editors.epds;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Popup;
import org.openlca.app.util.UI;
import org.openlca.io.xls.epds.EpdExcelWriter;

record ExportButton(EpdEditor editor) {

	void render(Composite parent, FormToolkit tk) {
		var btn = UI.button(parent, tk, M.ExportToExcel);
		btn.setImage(Images.get(FileType.EXCEL));
		Controls.onSelect(btn, _ -> export());
	}

	private void export() {
		var epd = editor.getModel();
		if (epd == null)
			return;
		var name = Labels.name(epd);
		name = name == null ? "epd" : name;
		name = name.replaceAll("\\W+", "_") + ".xlsx";
		var file = FileChooser.forSavingFile(M.Export, name);
		if (file == null)
			return;

		var res = App.exec(M.ExcelExport,
			() -> new EpdExcelWriter(epd, file).write());
		if (res == null)
			return;
		if (res.isError()) {
			MsgBox.error(M.ExcelExport, res.error());
			return;
		}
		Popup.info(M.ExportDone);
	}
}
