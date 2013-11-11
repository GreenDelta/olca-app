package org.openlca.app.processes;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.core.model.Process;

class AllocationPage extends FormPage {

	private Process process;
	private ProcessEditor editor;
	private FormToolkit toolkit;

	public AllocationPage(ProcessEditor editor) {
		super(editor, "process.AllocationPage", "Allocation");
		this.process = editor.getModel();
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "Allocation");
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createPhysicalEconomicSection(body);
		createCausalSection(body);
		form.reflow(true);
	}

	private void createPhysicalEconomicSection(Composite body) {
		Section section = UI.section(body, toolkit,
				"Physical & economic allocation");
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		String[] colNames = { Messages.Product, Messages.Physical,
				Messages.Economic };
		TableViewer factorViewer = Tables.createViewer(composite, colNames);
		Tables.bindColumnWidths(factorViewer, 0.3, 0.3, 0.3);
	}

	private void createCausalSection(Composite body) {
		Section section = UI.section(body, toolkit, "Causal allocation");
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 2);
	}

	private static class FactorLabel extends LabelProvider {

	}

}
