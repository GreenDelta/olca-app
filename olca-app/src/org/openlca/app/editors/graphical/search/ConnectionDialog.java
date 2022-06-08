package org.openlca.app.editors.graphical.search;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.RootDescriptor;

import java.util.ArrayList;
import java.util.List;

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

	/** The exchange for which we search possible connection candidates. */
	private final ModelExchange exchange;
	private final List<Candidate> candidates;
	TableViewer viewer;

	public ConnectionDialog(ExchangeItem exchangeItem) {
		super(UI.shell());
		setBlockOnOpen(true);
		exchange = new ModelExchange(exchangeItem);
		candidates = exchange.searchCandidates(Database.get());
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(exchange.isInput()
				? M.SelectProviders
				: M.SelectRecipients);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		UI.gridLayout(container, 1);
		viewer = Tables.createViewer(container, LABELS.ALL);
		double w = 1 / 6d;
		Tables.bindColumnWidths(viewer, w, w, w, w, w, w);
		viewer.setLabelProvider(new ConnectionLabelProvider(this));
		Table table = viewer.getTable();
		viewer.setInput(candidates);
		CellEditor[] editors = new CellEditor[5];
		editors[1] = new CheckboxCellEditor(table);
		editors[2] = new CheckboxCellEditor(table);
		viewer.setColumnProperties(LABELS.ALL);
		viewer.setCellModifier(new ConnectionCellModifier(this));
		viewer.setCellEditors(editors);
		return container;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 500);
	}

	boolean canBeConnected(Candidate c) {
		return exchange.canConnect(c, candidates);
	}

	public List<RootDescriptor> getNewProcesses() {
		List<RootDescriptor> processes = new ArrayList<>();
		for (Candidate c : candidates) {
			if (c.doCreate) {
				processes.add(c.process);
			}
		}
		return processes;
	}

	public List<ProcessLink> getNewLinks() {
		List<ProcessLink> newLinks = new ArrayList<>();
		for (Candidate c : candidates) {
			if (!c.doConnect)
				continue;
			var link = new ProcessLink();
			link.flowId = exchange.exchange.flow.id;
			if (exchange.isProvider()) {
				link.providerId = exchange.process.id;
				link.setProviderType(exchange.process.type);
				link.processId = c.process.id;
				link.exchangeId = c.exchangeId;
			} else {
				link.providerId = c.process.id;
				link.setProviderType(c.process.type);
				link.exchangeId = exchange.exchange.id;
				link.processId = exchange.process.id;

			}
			newLinks.add(link);
		}
		return newLinks;
	}
}
