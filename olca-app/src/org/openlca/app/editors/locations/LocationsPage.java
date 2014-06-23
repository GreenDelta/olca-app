package org.openlca.app.editors.locations;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.wizards.IWizardDescriptor;
import org.openlca.app.Messages;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;

class LocationsPage extends FormPage {

	private LocationViewer viewer;

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
		createImportButton(body);
		Section section = UI.section(body, toolkit, Messages.Locations);
		UI.gridData(section, true, true);
		Composite container = UI.sectionClient(section, toolkit);
		UI.gridData(container, true, true);
		int heightHint = form.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		viewer = new LocationViewer(getEditor(), container, heightHint);
		viewer.bindTo(section);
		form.reflow(true);
	}

	private void createImportButton(Composite parent) {
		Button importButton = new Button(parent, SWT.NONE);
		importButton.setText("Import data from KML file...");
		importButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean doContinue = true;
				if (getEditor().isDirty()) {
					doContinue = Question
							.ask("Import KML data",
									"Previous changes must be saved before importing kml data. Do you want to continue?");
					if (doContinue)
						getEditor().doSave();
				}
				if (!doContinue)
					return;
				IWizardDescriptor descriptor = PlatformUI.getWorkbench()
						.getImportWizardRegistry()
						.findWizard(KmzImportWizard.ID);
				if (descriptor == null)
					return;
				try {
					IWizard wizard = descriptor.createWizard();
					WizardDialog wd = new WizardDialog(UI.shell(), wizard);
					wd.setTitle(wizard.getWindowTitle());
					wd.open();
					getEditor().updateModel();
					viewer.reload();
				} catch (CoreException e1) {
					e1.printStackTrace();
				}
			}

		});
	}
}
