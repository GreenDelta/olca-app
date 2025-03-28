package org.openlca.app.editors.graphical.search;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
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
import org.openlca.core.model.ProviderType;
import org.openlca.core.model.descriptors.RootDescriptor;

public class ConnectionDialog extends Dialog {

	private final boolean isDirty;

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
	private final List<LinkCandidate> candidates;
	TableViewer viewer;

	/**
	 * Creates a new dialog with candidate providers/recipients.
	 * @param exchangeItem the target exchange item
	 * @param isDirty true if the exchange's entity is not saved
	 */
	public ConnectionDialog(ExchangeItem exchangeItem, boolean isDirty) {
		super(UI.shell());
		setBlockOnOpen(true);
		exchange = new ModelExchange(exchangeItem);
		this.isDirty = isDirty;
		candidates = exchange.searchLinkCandidates(Database.get());
		setShellStyle(SWT.RESIZE);
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
		Tables.bindColumnWidths(viewer, 0.43, 0.04, 0.08, 0.15, 0.16, 0.14);
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
		return new Point(1200, 500);
	}

	boolean canBeConnected(LinkCandidate c) {
		return exchange.canConnect(c, candidates);
	}

	boolean canBeAdded(LinkCandidate c) {
		return exchange.canBeAdded(c, candidates);
	}

	public List<RootDescriptor> getNewProcesses() {
		List<RootDescriptor> processes = new ArrayList<>();
		for (LinkCandidate c : candidates) {
			if (c.doCreate) {
				processes.add(c.process);
			}
		}
		return processes;
	}

	public List<ProcessLink> getNewLinks() {
		List<ProcessLink> newLinks = new ArrayList<>();
		for (LinkCandidate c : candidates) {
			if (!c.doConnect)
				continue;
			var link = new ProcessLink();
			link.flowId = exchange.exchange.flow.id;
			if (exchange.isProvider) {
				link.providerId = exchange.process.id;
				link.providerType = ProviderType.of(exchange.process.type);
				link.processId = c.process.id;
				link.exchangeId = c.exchangeId;
			} else {
				link.providerId = c.process.id;
				link.providerType = ProviderType.of(c.process.type);
				// setting the exchange ID to the internal ID if the entity is dirty.
				link.exchangeId = isDirty
						? exchange.exchange.internalId
						: exchange.exchange.id;
				link.processId = exchange.process.id;
			}
			newLinks.add(link);
		}
		return newLinks;
	}
}
