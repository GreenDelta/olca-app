package org.openlca.app.tools.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.app.util.UI;
import org.openlca.io.maps.FlowRef;

public class FlowRefDialog extends FormDialog {

	public static void open(IProvider provider) {
		if (provider == null)
			return;
		AtomicReference<List<FlowRef>> ar = new AtomicReference<>();
		App.runWithProgress("Collect flows ...", () -> {
			ar.set(provider.getFlowRefs());
		}, () -> {
			List<FlowRef> refs = ar.get();
			if (refs != null) {
				FlowRefDialog dialog = new FlowRefDialog(refs);
				dialog.open();
			}
		});
	}

	private final List<FlowRef> flowRefs;
	private Text filterText;

	private FlowRefDialog(List<FlowRef> flowRefs) {
		super(UI.shell());
		this.flowRefs = flowRefs;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(mform.getForm(), tk);
		UI.gridLayout(body, 1);

		Composite filterComp = tk.createComposite(body);
		UI.gridLayout(filterComp, 2);
		UI.gridData(filterComp, true, true);
		Label filterLabel = UI.formLabel(filterComp, tk, M.Filter);
		filterLabel.setFont(UI.boldFont());
		filterText = UI.formText(filterComp, SWT.SEARCH);
		UI.gridData(filterText, true, false);

	}

	private class Tree {

		private final Node root = new Node();

	}

	private class Node {
		String category;
		final List<Node> childs = new ArrayList<>();
		final List<FlowRef> refs = new ArrayList<>();
	}
}
