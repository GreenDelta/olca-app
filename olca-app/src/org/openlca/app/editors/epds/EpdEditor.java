package org.openlca.app.editors.epds;

import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.Epd;

public class EpdEditor extends ModelEditor<Epd> {

	public EpdEditor() {
		super(Epd.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new InfoPage(this));
			addPage(new DocPage(this));
		} catch (Exception e) {
			ErrorReporter.on("Failed to open EPD", e);
		}
	}

}
