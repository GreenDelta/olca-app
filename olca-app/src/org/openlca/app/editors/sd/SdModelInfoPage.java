package org.openlca.app.editors.sd;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.util.UI;

public class SdModelInfoPage extends FormPage {

	private final File modelDir;
	private FormToolkit toolkit;
	private Text nameText;
	private Text startTimeText;
	private Text endTimeText;
	private Text timeStepsText;
	private boolean dirty = false;

	public SdModelInfoPage(FormEditor editor, File modelDir) {
		super(editor, "SdModelInfoPage", "General information");
		this.modelDir = modelDir;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = mform.getForm();
		toolkit = mform.getToolkit();
		form.setText("System Dynamics Model");

		Composite body = form.getBody();
		toolkit.decorateFormHeading(form.getForm());
		toolkit.paintBordersFor(body);
		UI.gridLayout(body, 1);

		createGeneralSection(body);
		createSimulationSection(body);
		createImageSection(body);

		loadData();
	}

	private void createGeneralSection(Composite parent) {
		Section section = toolkit.createSection(parent,
			Section.TITLE_BAR | Section.EXPANDED);
		section.setText("General information");
		UI.gridData(section, true, false);

		Composite comp = toolkit.createComposite(section);
		section.setClient(comp);
		UI.gridLayout(comp, 2);

		nameText = UI.labeledText(comp, toolkit, M.Name);
		nameText.addModifyListener(e -> {
			dirty = true;
			getEditor().editorDirtyStateChanged();
		});
	}

	private void createSimulationSection(Composite parent) {
		Section section = toolkit.createSection(parent,
			Section.TITLE_BAR | Section.EXPANDED);
		section.setText("Simulation settings");
		UI.gridData(section, true, false);

		Composite comp = toolkit.createComposite(section);
		section.setClient(comp);
		UI.gridLayout(comp, 2);

		startTimeText = UI.labeledText(comp, toolkit, "Start time");
		startTimeText.addModifyListener(e -> {
			dirty = true;
			getEditor().editorDirtyStateChanged();
		});

		endTimeText = UI.labeledText(comp, toolkit, "End time");
		endTimeText.addModifyListener(e -> {
			dirty = true;
			getEditor().editorDirtyStateChanged();
		});

		timeStepsText = UI.labeledText(comp, toolkit, "Time steps");
		timeStepsText.addModifyListener(e -> {
			dirty = true;
			getEditor().editorDirtyStateChanged();
		});
	}

	private void createImageSection(Composite parent) {
		Section section = toolkit.createSection(parent,
			Section.TITLE_BAR | Section.EXPANDED);
		section.setText("Model image");
		UI.gridData(section, true, false);

		Composite comp = toolkit.createComposite(section);
		section.setClient(comp);
		UI.gridLayout(comp, 1);

		// TODO: Add model image display here later
		toolkit.createLabel(comp, "Model image will be displayed here");
	}

	private void loadData() {
		if (modelDir == null)
			return;

		// Load model name from directory name
		nameText.setText(modelDir.getName());

		// TODO: Load simulation settings from model file
		startTimeText.setText("0");
		endTimeText.setText("100");
		timeStepsText.setText("1");

		dirty = false;
	}

	public void doSave(IProgressMonitor monitor) {
		if (!dirty)
			return;

		// TODO: Save data to model file
		// For now, just mark as clean
		dirty = false;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}
}
