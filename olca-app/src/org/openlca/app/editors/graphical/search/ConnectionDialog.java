package org.openlca.app.editors.graphical.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionDialog extends Dialog {

	interface LABELS {
		String NAME = M.Name;
		String CREATE = M.Add;
		String CONNECT = M.Connect;
		String EXISTS = M.AlreadyPresent;
		String CONNECTED = M.AlreadyConnected;
		String DEFAULT_PROVIDER = M.IsDefaultProvider;
		String[] ALL = new String[] { NAME, CREATE, CONNECT, EXISTS, CONNECTED, DEFAULT_PROVIDER };
	}

	private static final Logger log = LoggerFactory.getLogger(ConnectionDialog.class);
	private final Exchange exchange;
	private final long processId;
	private final Set<Long> existingProcesses;
	private final MutableProcessLinkSearchMap linkSearch;
	private final boolean selectProvider;
	private final List<AvailableConnection> availableConnections = new ArrayList<>();
	private final boolean isConnected;
	TableViewer viewer;

	public ConnectionDialog(ExchangeNode exchangeNode) {
		super(UI.shell());
		setShellStyle(SWT.BORDER | SWT.TITLE);
		setBlockOnOpen(true);
		exchange = exchangeNode.exchange;
		processId = exchangeNode.parent().process.id;
		ProductSystemNode systemNode = exchangeNode.parent().parent();
		existingProcesses = systemNode.getProductSystem().processes;
		linkSearch = systemNode.linkSearch;
		selectProvider = exchangeNode.exchange.isInput;
		boolean foundLink = false;
		for (ProcessLink link : linkSearch.getConnectionLinks(processId))
			if (link.flowId == exchange.flow.id)
				foundLink = true;
		isConnected = foundLink;
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control c = super.createButtonBar(parent);
		c.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		return c;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new FillLayout());
		FormToolkit toolkit = new FormToolkit(Display.getCurrent());
		Section selectObjectSection = toolkit.createSection(container, ExpandableComposite.TITLE_BAR
				| ExpandableComposite.FOCUS_TITLE);
		selectObjectSection.setText(selectProvider ? M.SelectProviders : M.SelectRecipients);
		Composite composite = toolkit.createComposite(selectObjectSection, SWT.NONE);
		UI.gridLayout(composite, 1);
		selectObjectSection.setClient(composite);
		toolkit.adapt(composite);
		viewer = Tables.createViewer(composite, LABELS.ALL);
		viewer.setLabelProvider(new ConnectionLabelProvider(this));
		Table table = viewer.getTable();
		createInternalModel();
		if (availableConnections.size() > 0)
			viewer.setInput(availableConnections.toArray(new AvailableConnection[availableConnections.size()]));
		CellEditor[] editors = new CellEditor[5];
		editors[1] = new CheckboxCellEditor(table);
		editors[2] = new CheckboxCellEditor(table);
		viewer.setColumnProperties(LABELS.ALL);
		viewer.setCellModifier(new ConnectionCellModifier(this));
		viewer.setCellEditors(editors);
		toolkit.paintBordersFor(composite);
		return container;
	}

	private void createInternalModel() {
		availableConnections.clear();
		long flowId = exchange.flow.id;
		String query = "SELECT id, f_owner FROM tbl_exchanges "
				+ "WHERE f_flow = " + flowId + " AND is_input = " + (selectProvider ? 0 : 1);
		List<Pair<Long, Long>> exchanges = new ArrayList<>();
		Set<Long> processIds = new HashSet<>();
		try {
			NativeSql.on(Database.get()).query(query, (rs) -> {
				long id = rs.getLong("id");
				long pId = rs.getLong("f_owner");
				exchanges.add(Pair.of(id, pId));
				processIds.add(pId);
				return true;
			});
		} catch (Exception e) {
			log.error("Error loading available connections", e);
		}
		List<ProcessDescriptor> processes = new ProcessDao(Database.get()).getDescriptors(processIds);
		Map<Long, ProcessDescriptor> idToProcess = new HashMap<>();
		for (ProcessDescriptor process : processes)
			idToProcess.put(process.id, process);
		for (Pair<Long, Long> exchange : exchanges) {
			ProcessDescriptor process = idToProcess.get(exchange.getRight());
			boolean existing = existingProcesses.contains(process.id);
			boolean connected = false;
			boolean defaultProvider = this.exchange.defaultProviderId == process.id;
			if (existing)
				connected = isAlreadyConnected(process);
			availableConnections.add(new AvailableConnection(process, exchange.getLeft(), existing, connected,
					defaultProvider));
		}
		Collections.sort(availableConnections);
	}

	private boolean isAlreadyConnected(ProcessDescriptor process) {
		if (selectProvider)
			return isAlreadyProvider(process);
		return isAlreadyReceiver(process);
	}

	private boolean isAlreadyProvider(ProcessDescriptor process) {
		for (ProcessLink link : linkSearch.getProviderLinks(process.id)) {
			if (link.processId != processId)
				continue;
			if (link.flowId != exchange.flow.id)
				continue;
			return true;
		}
		return false;
	}

	private boolean isAlreadyReceiver(ProcessDescriptor process) {
		for (ProcessLink link : linkSearch.getConnectionLinks(process.id)) {
			if (link.providerId != processId)
				continue;
			if (link.exchangeId != exchange.id)
				continue;
			return true;
		}
		return false;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 500);
	}

	boolean canBeConnected(AvailableConnection connectable) {
		if (isConnected)
			return false;
		if (connectable.alreadyConnected)
			return false;
		if (selectProvider && userHasSelectedProvider())
			return false;
		if (!selectProvider && hasProvider(connectable))
			return false;
		return true;
	}

	private boolean hasProvider(AvailableConnection process) {
		for (ProcessLink link : linkSearch.getConnectionLinks(process.process.id))
			if (link.exchangeId == exchange.id)
				return true;
		return false;
	}

	private boolean userHasSelectedProvider() {
		for (AvailableConnection connectable : availableConnections)
			if (connectable.alreadyConnected || connectable.connect)
				return true;
		return false;
	}

	public List<ProcessDescriptor> toCreate() {
		List<ProcessDescriptor> toCreate = new ArrayList<>();
		for (AvailableConnection connectable : availableConnections)
			if (connectable.create)
				toCreate.add(connectable.process);
		return toCreate;
	}

	public List<Pair<ProcessDescriptor, Long>> toConnect() {
		List<Pair<ProcessDescriptor, Long>> toConnect = new ArrayList<>();
		for (AvailableConnection connectable : availableConnections)
			if (connectable.connect) {
				toConnect.add(Pair.of(connectable.process, connectable.exchangeId));
			}
		return toConnect;
	}

	class AvailableConnection implements Comparable<AvailableConnection> {

		final ProcessDescriptor process;
		final long exchangeId;
		final boolean alreadyExisting;
		final boolean alreadyConnected;
		final boolean defaultProvider;
		boolean connect;
		boolean create;

		AvailableConnection(ProcessDescriptor process, long exchangeId,
				boolean existing, boolean connected, boolean defaultProvider) {
			this.process = process;
			this.exchangeId = exchangeId;
			this.alreadyExisting = existing;
			this.alreadyConnected = connected;
			this.defaultProvider = defaultProvider;
		}

		@Override
		public boolean equals(final Object arg0) {
			if (!(arg0 instanceof AvailableConnection))
				return false;
			AvailableConnection other = (AvailableConnection) arg0;
			if (other.process == null)
				return process == null;
			return Objects.equals(other.process, process) && exchangeId == other.exchangeId;
		}

		@Override
		public int compareTo(AvailableConnection o) {
			String n1 = Labels.getDisplayName(process);
			String n2 = Labels.getDisplayName(o.process);
			return n1.toLowerCase().compareTo(n2.toLowerCase());
		}

	}
}
