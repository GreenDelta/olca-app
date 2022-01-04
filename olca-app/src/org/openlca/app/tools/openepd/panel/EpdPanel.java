package org.openlca.app.tools.openepd.panel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.UI;

public class EpdPanel extends SimpleFormEditor {

	public static void open() {
		Editors.open(new SimpleEditorInput("EpdPanel", "openEPD"), "EpdPanel");
	}

	@Override
	protected FormPage getPage() {
		return new Page(this);
	}

	private static class Page extends FormPage {

		public Page(EpdPanel panel) {
			super(panel, "EpdPanel.Page", "openEPD");
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(mform, "Building Transparency - openEPD",
					Icon.EC3_WIZARD.get());
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);
			var loginPanel = LoginPanel.create(body, tk);

			var section = UI.section(body, tk, "Find EPDs");
			UI.gridData(section, true, true);
			var comp = UI.sectionClient(section, tk, 1);
			var searchComp = tk.createComposite(comp);
			UI.fillHorizontal(searchComp);
			UI.gridLayout(searchComp, 4);

			var searchText = tk.createText(searchComp, "");
			UI.fillHorizontal(searchText);
			var searchButton = tk.createButton(searchComp, "Search", SWT.NONE );
			searchButton.setImage(Icon.SEARCH.get());

			tk.createLabel(searchComp, "Max. count:");
			var spinner = new Spinner(searchComp, SWT.BORDER);
			spinner.setValues(100, 10, 1000, 0, 50, 100);
			tk.adapt(spinner);


			form.reflow(true);
		}
	}
}
