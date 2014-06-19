package org.openlca.app.editors.locations;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.util.UI;

class LocationsPage extends FormPage {

	LocationsPage(LocationsEditor editor) {
		super(editor, LocationsPage.class.getCanonicalName(), "Locations");
	}

	@Override
	public LocationsEditor getEditor() {
		return (LocationsEditor) super.getEditor();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Locations);
		UI.gridData(form, true, true);
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		UI.gridData(body, true, true);
		Section section = UI.section(body, toolkit, Messages.Locations);
		UI.gridData(section, true, true);
		Composite container = UI.sectionClient(section, toolkit);
		UI.gridData(container, true, true);
		int heightHint = form.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		LocationViewer viewer = new LocationViewer(getEditor(), container,
				heightHint);
		viewer.bindTo(section);
		form.reflow(true);
	}
}
