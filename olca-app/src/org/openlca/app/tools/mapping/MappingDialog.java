package org.openlca.app.tools.mapping;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Fn;
import org.openlca.app.util.UI;
import org.openlca.io.maps.FlowMapEntry;

class MappingDialog extends FormDialog {

	/**
	 * Opens a dialog for editing the given entry. When the returned status code
	 * is `OK`, the entry was updated (maybe modified). Otherwise, it is
	 * unchanged.
	 */
	static int open(MappingTool tool, FlowMapEntry entry) {
		if (tool == null || entry == null)
			return CANCEL;
		MappingDialog d = new MappingDialog(tool, entry.clone());
		int state = d.open();
		if (state != OK)
			return state;
		entry.factor = d.entry.factor;
		entry.sourceFlow = d.entry.sourceFlow;
		entry.targetFlow = d.entry.targetFlow;
		entry.status = d.entry.status;
		return state;
	}

	private final MappingTool tool;
	private final FlowMapEntry entry;

	MappingDialog(MappingTool tool, FlowMapEntry entry) {
		super(UI.shell());
		this.tool = tool;
		this.entry = entry;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		FormToolkit tk = mform.getToolkit();
		Composite body = mform.getForm().getBody();
		body.setLayout(new FillLayout());
		Composite comp = tk.createComposite(body);
		UI.gridLayout(comp, 2).makeColumnsEqualWidth = true;

		Fn.with(UI.formLabel(comp, tk, "Source flow"), label -> {
			label.setFont(UI.boldFont());
			UI.gridData(label, true, false);
		});
		Fn.with(UI.formLabel(comp, tk, "Target flow"), label -> {
			label.setFont(UI.boldFont());
			UI.gridData(label, true, false);
		});

	}

}
