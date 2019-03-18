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
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
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
		String[] ALL = new String[] { NAME, CREATE, CONNECT,
				EXISTS, CONNECTED, DEFAULT_PROVIDER };
	}

	private static final Logger log = LoggerFactory.getLogger(ConnectionDialog.class);
	private final Exchange exchange;
	private final long processId;
	private final Set<Long> existingProcesses;
	private final MutableProcessLinkSearchMap linkSearch;

	/**
	 * Indicates whether we search a connection for an input or output.
	 */
	private final boolean forInput;

	private final List<Candidate> candidates = new ArrayList<>();
	private final boolean isConnected;
	TableViewer viewer;

	public ConnectionDialog(ExchangeNode enode) {
		super(UI.shell());
		setShellStyle(SWT.BORDER | SWT.TITLE);
		setBlockOnOpen(true);
		exchange = enode.exchange;
		processId = enode.parent().process.id;
		ProductSystemNode systemNode = enode.parent().parent();
		existingProcesses = systemNode.getProductSystem().processes;
		linkSearch = systemNode.linkSearch;
		forInput = enode.exchange.isInput;
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
		FormToolkit tk = new FormToolkit(Display.getCurrent());
		container.addDisposeListener(e -> {
			tk.dispose();
		});
		Section section = tk.createSection(container,
				ExpandableComposite.TITLE_BAR
						| ExpandableComposite.FOCUS_TITLE);
		section.setText(forInput ? M.SelectProviders : M.SelectRecipients);
		Composite comp = tk.createComposite(section, SWT.NONE);
		UI.gridLayout(comp, 1);
		section.setClient(comp);
		tk.adapt(comp);

		viewer = Tables.createViewer(comp, LABELS.ALL);
		double w = 1 / 6d;
		Tables.bindColumnWidths(viewer, w, w, w, w, w, w);
		viewer.setLabelProvider(new ConnectionLabelProvider(this));
		Table table = viewer.getTable();
		searchCandidates();
		if (candidates.size() > 0) {
			viewer.setInput(candidates.toArray(
					new Candidate[candidates.size()]));
		}
		CellEditor[] editors = new CellEditor[5];
		editors[1] = new CheckboxCellEditor(table);
		editors[2] = new CheckboxCellEditor(table);
		viewer.setColumnProperties(LABELS.ALL);
		viewer.setCellModifier(new ConnectionCellModifier(this));
		viewer.setCellEditors(editors);
		tk.paintBordersFor(comp);
		return container;
	}

	private void searchCandidates() {
		candidates.clear();
		IDatabase db = Database.get();

		// search for processes that have an exchange
		// with the flow on the opposite side
		List<LongPair> exchanges = new ArrayList<>();
		Set<Long> exchangeIds = new HashSet<>();
		Set<Long> processIds = new HashSet<>();
		try {
			String sql = "SELECT id, f_owner FROM tbl_exchanges "
					+ "WHERE f_flow = " + exchange.flow.id
					+ " AND is_input = " + (forInput ? 0 : 1);
			NativeSql.on(db).query(sql, r -> {
				long exchangeID = r.getLong("id");
				long processID = r.getLong("f_owner");
				exchanges.add(LongPair.of(
						exchangeID, processID));
				exchangeIds.add(exchangeID);
				processIds.add(processID);
				return true;
			});
		} catch (Exception e) {
			log.error("Error loading connection candidates", e);
		}

		List<ProcessDescriptor> procs = new ProcessDao(db)
				.getDescriptors(processIds);
		Map<Long, CategorizedDescriptor> processes = new HashMap<>();
		for (ProcessDescriptor p : procs) {
			processes.put(p.id, p);
		}

		// add product systems that have the same exchanges as
		// the process candidates as reference flows (these can
		// be connected as sub-systems
		Set<Long> systemIds = new HashSet<Long>();
		try {
			String sql = "SELECT id, f_reference_exchange FROM"
					+ " tbl_product_systems";
			NativeSql.on(db).query(sql, r -> {
				long exchangeID = r.getLong(2);
				if (exchangeIds.contains(exchangeID)) {
					long systemID = r.getLong(1);
					exchanges.add(LongPair.of(exchangeID, systemID));
					systemIds.add(systemID);
				}
				return true;
			});
		} catch (Exception e) {
			log.error("Error loading connection candidates", e);
		}

		if (!systemIds.isEmpty()) {
			new ProductSystemDao(db).getDescriptors(systemIds)
					.forEach(d -> {
						processes.put(d.id, d);
					});
		}

		for (LongPair e : exchanges) {
			CategorizedDescriptor p = processes.get(e.second);
			if (p == null)
				continue;
			boolean existing = existingProcesses.contains(p.id);
			boolean connected = false;
			boolean defaultProvider = this.exchange.defaultProviderId == p.id;
			if (existing) {
				connected = forInput
						? isAlreadyProvider(p)
						: isAlreadyReceiver(p);
			}
			candidates.add(new Candidate(
					p, e.first,
					existing, connected,
					defaultProvider));
		}

		Collections.sort(candidates);
	}

	private boolean isAlreadyProvider(CategorizedDescriptor p) {
		for (ProcessLink link : linkSearch.getProviderLinks(p.id)) {
			if (link.processId != processId)
				continue;
			if (link.flowId != exchange.flow.id)
				continue;
			return true;
		}
		return false;
	}

	private boolean isAlreadyReceiver(CategorizedDescriptor p) {
		for (ProcessLink link : linkSearch.getConnectionLinks(p.id)) {
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

	boolean canBeConnected(Candidate con) {
		if (isConnected)
			return false;
		if (con.alreadyConnected)
			return false;
		if (forInput && userHasSelectedProvider())
			return false;
		if (!forInput && hasProvider(con))
			return false;
		return true;
	}

	private boolean hasProvider(Candidate process) {
		for (ProcessLink link : linkSearch.getConnectionLinks(process.process.id))
			if (link.exchangeId == exchange.id)
				return true;
		return false;
	}

	private boolean userHasSelectedProvider() {
		for (Candidate connectable : candidates)
			if (connectable.alreadyConnected || connectable.connect)
				return true;
		return false;
	}

	public List<CategorizedDescriptor> toCreate() {
		List<CategorizedDescriptor> toCreate = new ArrayList<>();
		for (Candidate connectable : candidates)
			if (connectable.create)
				toCreate.add(connectable.process);
		return toCreate;
	}

	public List<Pair<CategorizedDescriptor, Long>> toConnect() {
		List<Pair<CategorizedDescriptor, Long>> toConnect = new ArrayList<>();
		for (Candidate connectable : candidates)
			if (connectable.connect) {
				toConnect.add(Pair.of(connectable.process, connectable.exchangeId));
			}
		return toConnect;
	}

	/** Contains the data of a possible connection candidate. */
	class Candidate implements Comparable<Candidate> {

		final CategorizedDescriptor process;
		final long exchangeId;
		final boolean alreadyExisting;
		final boolean alreadyConnected;
		final boolean defaultProvider;
		boolean connect;
		boolean create;

		Candidate(CategorizedDescriptor process, long exchangeId,
				boolean existing, boolean connected, boolean defaultProvider) {
			this.process = process;
			this.exchangeId = exchangeId;
			this.alreadyExisting = existing;
			this.alreadyConnected = connected;
			this.defaultProvider = defaultProvider;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Candidate))
				return false;
			Candidate other = (Candidate) obj;
			if (other.process == null)
				return process == null;
			return Objects.equals(other.process, process)
					&& exchangeId == other.exchangeId;
		}

		@Override
		public int compareTo(Candidate o) {
			String n1 = Labels.getDisplayName(process);
			String n2 = Labels.getDisplayName(o.process);
			return n1.toLowerCase().compareTo(n2.toLowerCase());
		}

	}
}
