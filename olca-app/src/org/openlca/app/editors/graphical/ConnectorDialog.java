package org.openlca.app.editors.graphical;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ConnectorDialog extends Dialog {

	private interface LABELS {
		String NAME = Messages.Name;
		String CREATE = Messages.Systems_SelectPossibleProcessesDialog_Create;
		String CONNECT = Messages.Systems_SelectPossibleProcessesDialog_Connect;
		String EXISTS = Messages.Systems_SelectPossibleProcessesDialog_Exists;
		String CONNECTED = Messages.Systems_SelectPossibleProcessesDialog_IsConnected;
		String[] ALL = new String[] { NAME, CREATE, CONNECT, EXISTS, CONNECTED };
	}

	private Exchange exchange;
	private ProcessDescriptor parentProcess;
	private ProductSystem productSystem;
	private TableViewer viewer;
	private boolean selectProvider;
	private List<ConnectableProcess> connectableProcesses = new ArrayList<>();

	public ConnectorDialog(ExchangeNode exchangeNode) {
		super(UI.shell());
		setShellStyle(SWT.BORDER | SWT.TITLE);
		setBlockOnOpen(true);
		exchange = exchangeNode.getExchange();
		parentProcess = exchangeNode.getParent().getParent().getProcess();
		productSystem = exchangeNode.getParent().getParent().getParent()
				.getProductSystem();
		selectProvider = exchange.isInput();
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control c = super.createButtonBar(parent);
		c.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		parent.setBackground(Display.getCurrent().getSystemColor(
				SWT.COLOR_WHITE));
		return c;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	private List<ProcessDescriptor> loadPossibleConnectors() {
		Set<Long> connectorIds = null;
		if (selectProvider)
			connectorIds = new FlowDao(Database.get()).getProviders(exchange
					.getFlow().getId());
		else
			connectorIds = new FlowDao(Database.get()).getRecipients(exchange
					.getFlow().getId());
		return new ProcessDao(Database.get()).getDescriptors(connectorIds);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		List<ProcessDescriptor> processes = loadPossibleConnectors();

		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new FillLayout());
		FormToolkit toolkit = new FormToolkit(Display.getCurrent());

		Section selectObjectSection = toolkit
				.createSection(container, ExpandableComposite.TITLE_BAR
						| ExpandableComposite.FOCUS_TITLE);
		selectObjectSection
				.setText(selectProvider ? Messages.Systems_SelectPossibleProcessesDialog_SelectProviders
						: Messages.Systems_SelectPossibleProcessesDialog_SelectRecipients);
		Composite composite = toolkit.createComposite(selectObjectSection,
				SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 10;
		gridLayout.horizontalSpacing = 10;
		gridLayout.marginRight = 10;
		gridLayout.marginLeft = 10;
		gridLayout.marginBottom = 10;
		gridLayout.marginTop = 10;
		gridLayout.numColumns = 1;
		composite.setLayout(gridLayout);
		selectObjectSection.setClient(composite);
		toolkit.adapt(composite);

		viewer = new TableViewer(composite, SWT.FULL_SELECTION | SWT.BORDER);
		Table table = viewer.getTable();
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new ProcessLabelProvider());
		table.setLinesVisible(false);
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		toolkit.adapt(table, true, true);

		for (String label : LABELS.ALL)
			new TableColumn(table, SWT.NULL).setText(label);

		createInternalModel(processes);
		if (connectableProcesses.size() > 0)
			viewer.setInput(connectableProcesses
					.toArray(new ConnectableProcess[connectableProcesses.size()]));

		for (TableColumn column : table.getColumns())
			column.pack();

		CellEditor[] editors = new CellEditor[5];
		editors[1] = new CheckboxCellEditor(table);
		editors[2] = new CheckboxCellEditor(table);

		viewer.setColumnProperties(LABELS.ALL);
		viewer.setCellModifier(new ProcessCellModifier());
		viewer.setCellEditors(editors);

		toolkit.paintBordersFor(composite);

		return container;
	}

	private void createInternalModel(List<ProcessDescriptor> processes) {
		connectableProcesses.clear();
		for (ProcessDescriptor process : processes) {
			boolean alreadyExisting = productSystem.getProcesses().contains(
					process.getId());
			boolean alreadyConnected = false;
			if (alreadyExisting) {
				if (selectProvider) {
					for (ProcessLink link : ProcessLinks.getOutgoing(
							productSystem, process.getId())) {
						if (link.getRecipientId() == parentProcess.getId()
								&& link.getFlowId() == exchange.getFlow()
										.getId()) {
							alreadyConnected = true;
							break;
						}
					}
				} else {
					for (ProcessLink link : ProcessLinks.getIncoming(
							productSystem, process.getId())) {
						if (link.getProviderId() == parentProcess.getId()
								&& link.getFlowId() == exchange.getFlow()
										.getId()) {
							alreadyConnected = true;
							break;
						}
					}
				}
			}
			connectableProcesses.add(new ConnectableProcess(process,
					alreadyExisting, alreadyConnected));
		}
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 500);
	}

	private boolean canBeConnected(ConnectableProcess process) {
		if (process.isAlreadyConnectedToExchange())
			return false;

		// check if recipient already has a provider
		if (!selectProvider)
			for (ProcessLink link : ProcessLinks.getIncoming(productSystem,
					process.getProcess().getId()))
				if (link.getFlowId() == exchange.getFlow().getId())
					return false;

		// check if the user already selected a provider
		if (selectProvider)
			for (ConnectableProcess connectable : connectableProcesses)
				if (connectable.isAlreadyConnectedToExchange()
						|| connectable.doConnect())
					return false;
		return true;
	}

	public ProcessDescriptor[] getProcessesToCreate() {
		List<ProcessDescriptor> processes = new ArrayList<>();
		for (ConnectableProcess process : connectableProcesses)
			if (process.doCreate())
				processes.add(process.getProcess());
		return processes.toArray(new ProcessDescriptor[processes.size()]);
	}

	public ProcessDescriptor[] getProcessesToConnect() {
		List<ProcessDescriptor> processes = new ArrayList<>();
		for (ConnectableProcess process : connectableProcesses)
			if (process.doConnect())
				processes.add(process.getProcess());
		return processes.toArray(new ProcessDescriptor[processes.size()]);
	}

	private class ConnectableProcess {

		private boolean connect;
		private boolean create;
		private boolean alreadyExisting;
		private boolean alreadyConnected;
		private ProcessDescriptor process;

		public ConnectableProcess(ProcessDescriptor process,
				boolean alreadyExisting, boolean alreadyConnected) {
			this.process = process;
			this.alreadyExisting = alreadyExisting;
			this.alreadyConnected = alreadyConnected;
		}

		public boolean isAlreadyConnectedToExchange() {
			return alreadyConnected;
		}

		public boolean doConnect() {
			return connect;
		}

		public boolean doCreate() {
			return create;
		}

		@Override
		public boolean equals(final Object arg0) {
			if (!(arg0 instanceof ConnectableProcess))
				return false;
			ConnectableProcess other = (ConnectableProcess) arg0;
			if (other.getProcess() == null)
				return process == null;
			return Objects.equals(other.getProcess(), process);
		}

		public boolean isAlreadyExisting() {
			return alreadyExisting;
		}

		public ProcessDescriptor getProcess() {
			return process;
		}

		public void setCreate(boolean create) {
			this.create = create;
		}

		public void setConnect(boolean connect) {
			this.connect = connect;
		}

	}

	private class ProcessCellModifier implements ICellModifier {

		@Override
		public boolean canModify(Object element, String property) {
			if (!(element instanceof ConnectableProcess))
				return false;
			ConnectableProcess process = (ConnectableProcess) element;

			if (property.equals(LABELS.CREATE))
				return canModifyCreateField(process);
			else if (property.equals(LABELS.CONNECT))
				return process.doConnect() || canBeConnected(process);
			return false;
		}

		private boolean canModifyCreateField(ConnectableProcess process) {
			if (process.isAlreadyExisting())
				return false;
			if (process.doConnect())
				return false;
			return true;
		}

		@Override
		public Object getValue(Object element, String property) {
			if (!(element instanceof ConnectableProcess))
				return null;
			ConnectableProcess process = (ConnectableProcess) element;

			if (property.equals(LABELS.CREATE))
				return process.doCreate();
			else if (property.equals(LABELS.CONNECT))
				return process.doConnect();

			return null;
		}

		@Override
		public void modify(Object element, String property, Object value) {
			if (!(element instanceof TableItem))
				return;
			TableItem item = (TableItem) element;
			if (!(item.getData() instanceof ConnectableProcess))
				return;

			ConnectableProcess process = (ConnectableProcess) item.getData();
			if (property.equals(LABELS.CREATE))
				process.setCreate(Boolean.parseBoolean(value.toString()));
			else if (property.equals(LABELS.CONNECT)) {
				process.setConnect(Boolean.parseBoolean(value.toString()));
				if (process.doConnect() && !process.isAlreadyExisting())
					process.setCreate(true);
			}
			viewer.refresh();
		}

	}

	private class ProcessLabelProvider implements ITableLabelProvider {

		@Override
		public void addListener(ILabelProviderListener listener) {

		}

		@Override
		public void dispose() {

		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof ConnectableProcess))
				return null;

			ConnectableProcess process = (ConnectableProcess) element;
			switch (columnIndex) {
			case 1:
				if (process.doCreate())
					return ImageType.CHECK_TRUE.get();
				if (!process.isAlreadyExisting())
					return ImageType.CHECK_FALSE.get();
				return null;
			case 2:
				if (process.doConnect())
					return ImageType.CHECK_TRUE.get();
				if (canBeConnected(process))
					return ImageType.CHECK_FALSE.get();
				return null;
			case 3:
				if (process.isAlreadyExisting())
					return ImageType.ACCEPT_ICON.get();
				return ImageType.DENY_ICON.get();
			case 4:
				if (process.isAlreadyConnectedToExchange())
					return ImageType.ACCEPT_ICON.get();
				return ImageType.DENY_ICON.get();
			default:
				return null;
			}
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ConnectableProcess))
				return null;
			if (columnIndex != 0)
				return null;

			ConnectableProcess process = (ConnectableProcess) element;
			return Labels.getDisplayName(process.getProcess());
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {

		}

	}

}
