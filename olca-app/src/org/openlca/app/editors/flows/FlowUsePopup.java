package org.openlca.app.editors.flows;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Strings;

class FlowUsePopup extends FormDialog {

	private final Flow flow;
	private final List<ProcessDescriptor> processes;
	private final String usageType;

	public static void show(Flow flow, Set<Long> processIds, String usageType) {
		if (flow == null || processIds.isEmpty())
			return;
		var processes = App.exec(
				"Loading process list...", () -> loadDescriptors(processIds));
		var dialog = new FlowUsePopup(flow, processes, usageType);
		dialog.open();
	}

	private static List<ProcessDescriptor> loadDescriptors(Set<Long> processIds) {
		if (processIds.isEmpty())
			return Collections.emptyList();
		var dao = new ProcessDao(Database.get());
		var processes = dao.getDescriptors(processIds);
		processes.sort((p1, p2)
				-> Strings.compare(Labels.name(p1), Labels.name(p2)));
		return processes;
	}

	private FlowUsePopup(
			Flow flow, List<ProcessDescriptor> processes, String usageType
	) {
		super(UI.shell());
		this.flow = flow;
		this.processes = processes;
		this.usageType = usageType;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Labels.name(flow) + " - " + usageType);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(750, 500);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(
				parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.dialogBody(mForm.getForm(), tk);
		var table = Tables.createViewer(body,
				M.Process,
				M.Category);
		table.setLabelProvider(new ProcessLabel());
		table.setInput(processes);
		Tables.bindColumnWidths(table, 0.7, 0.3);

		var onOpen = Actions.onOpen(() -> {
			ProcessDescriptor p = Viewers.getFirstSelected(table);
			if (p != null) {
				App.open(p);
				okPressed();
			}
		});
		Actions.bind(table, onOpen);
		Tables.onDoubleClick(table, e -> onOpen.run());
	}

	private static class ProcessLabel
			extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof ProcessDescriptor p))
				return null;
			return col == 0 ? Images.get(p) : null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ProcessDescriptor p))
				return null;
			return switch (col) {
				case 0 -> Labels.name(p);
				case 1 -> Labels.category(p);
				default -> null;
			};
		}
	}
}
