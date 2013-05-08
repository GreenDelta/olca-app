/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog for selecting reciepents or providers of a process' product
 * 
 * @author Sebastian Greve
 * 
 */
public class SelectPossibleProcessesDialog extends Dialog {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * Column property 'Connect'
	 */
	private final String CONNECT = Messages.Systems_SelectPossibleProcessesDialog_Connect;

	/**
	 * Column property 'Connected'
	 */
	private final String CONNECTED = Messages.Systems_SelectPossibleProcessesDialog_IsConnected;

	/**
	 * Column property 'Create'
	 */
	private final String CREATE = Messages.Systems_SelectPossibleProcessesDialog_Create;

	/**
	 * The database
	 */
	private final IDatabase database;

	/**
	 * The exchange to link with
	 */
	private Exchange exchange;

	/**
	 * Column property 'Exchange'
	 */
	private final String EXCHANGE = Messages.Systems_SelectPossibleProcessesDialog_Exchange;

	/**
	 * Column property 'Exists'
	 */
	private final String EXISTS = Messages.Systems_SelectPossibleProcessesDialog_Exists;

	/**
	 * Column property 'Name'
	 */
	private final String NAME = Messages.Common_Name;

	/**
	 * The process to link to (containing the exchange to link to)
	 */
	private Process parentOfExchange;

	/**
	 * The product system edited in the graph
	 */
	private ProductSystem productSystem;

	/**
	 * The table column properties
	 */
	private final String[] PROPERTIES = new String[] { CREATE, CONNECT, NAME,
			EXCHANGE, EXISTS, CONNECTED };

	/**
	 * The table viewer displaying the possible providers/recipients
	 */
	private TableViewer viewer;

	/**
	 * A list of possible providers/recipients
	 */
	protected List<PossibleProcess> possibleProcesses;

	/**
	 * Creates the dialog.
	 * 
	 * @param parentShell
	 *            see {@link Dialog#getShell()}
	 * @param productSystem
	 *            The product system currently edited
	 * @param parentOfExchange
	 *            The process to link to (containing the exchange to link to)
	 * @param exchange
	 *            The exchange to link with
	 * @param database
	 *            The database
	 */
	public SelectPossibleProcessesDialog(final Shell parentShell,
			final ProductSystem productSystem, final Process parentOfExchange,
			final Exchange exchange, final IDatabase database) {
		super(parentShell);
		setShellStyle(SWT.BORDER | SWT.TITLE);
		this.database = database;
		this.exchange = exchange;
		this.parentOfExchange = parentOfExchange;
		this.productSystem = productSystem;
		setBlockOnOpen(true);
	}

	@Override
	protected Control createButtonBar(final Composite parent) {
		final Control c = super.createButtonBar(parent);
		c.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		parent.setBackground(Display.getCurrent().getSystemColor(
				SWT.COLOR_WHITE));
		return c;
	}

	/**
	 * Create contents of the button bar
	 * 
	 * @param parent
	 *            The parent composite
	 */
	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Create contents of the dialog
	 * 
	 * @param parent
	 *            The parent composite
	 */
	@Override
	protected Control createDialogArea(final Composite parent) {
		// create dialog container

		final List<Process> processes = new ArrayList<>();
		final ProgressMonitorDialog dialog = new ProgressMonitorDialog(
				UI.shell());
		try {
			dialog.run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(final IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try {
						monitor.beginTask(NLS.bind(
								Messages.Systems_LoadingPossible, exchange
										.isInput() ? Messages.Systems_Providers
										: Messages.Systems_Recipients),
								IProgressMonitor.UNKNOWN);
						final Object[] objs = database
								.query("SELECT p FROM Process p WHERE EXISTS(SELECT e.id FROM Exchange e WHERE e.flow.id = '"
										+ exchange.getFlow().getId()
										+ "' AND e MEMBER OF p.exchanges AND e.input = "
										+ (exchange.isInput() ? "false"
												: "true") + ")");
						for (final Object obj : objs) {
							final Process process = (Process) obj;
							if (!processes.contains(process)) {
								processes.add(process);
							}
						}
						monitor.done();
					} catch (final Exception e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (final Exception e) {
			log.error("Creating dialog area failed", e);
		}

		final Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new FillLayout());
		final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

		// create section and section client
		final Section selectObjectSection = toolkit
				.createSection(container, ExpandableComposite.TITLE_BAR
						| ExpandableComposite.FOCUS_TITLE);
		selectObjectSection
				.setText(exchange.isInput() ? Messages.Systems_SelectPossibleProcessesDialog_SelectProviders
						: Messages.Systems_SelectPossibleProcessesDialog_SelectRecipients);
		final Composite composite = toolkit.createComposite(
				selectObjectSection, SWT.NONE);
		final GridLayout gridLayout = new GridLayout();
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

		// create table and its viewer
		viewer = new TableViewer(composite, SWT.FULL_SELECTION | SWT.BORDER);
		final Table table = viewer.getTable();
		viewer.setContentProvider(new PossibleProcessContentProvider());
		viewer.setLabelProvider(new PossibleProcessLabelProvider(exchange
				.isInput()));
		table.setLinesVisible(false);
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		toolkit.adapt(table, true, true);

		for (final String p : PROPERTIES) {
			final TableColumn c = new TableColumn(table, SWT.NULL);
			c.setText(p);
		}
		// set viewer input
		possibleProcesses = new ArrayList<>();
		for (int i = 0; i < processes.size(); i++) {
			final Process process = processes.get(i);
			final boolean exisistAlready = productSystem.getProcess(process
					.getId()) != null;
			final PossibleProcess pp = new PossibleProcess(process,
					exisistAlready);
			if (exisistAlready) {
				if (exchange.isInput()) {
					for (final ProcessLink link : productSystem
							.getOutgoingLinks(process.getId())) {
						if (link.getRecipientProcess().getId()
								.equals(parentOfExchange.getId())
								&& link.getRecipientInput().getId()
										.equals(exchange.getId())) {
							pp.setOutput(link.getProviderOutput());
							break;
						}
					}
				} else {
					for (final ProcessLink link : productSystem
							.getIncomingLinks(process.getId())) {
						if (link.getProviderProcess().getId()
								.equals(parentOfExchange.getId())
								&& link.getProviderOutput().getId()
										.equals(exchange.getId())) {
							pp.setInput(link.getRecipientInput());
							break;
						}
					}
				}
			}
			possibleProcesses.add(pp);
		}
		if (possibleProcesses.size() > 0) {
			viewer.setInput(possibleProcesses
					.toArray(new PossibleProcess[possibleProcesses.size()]));
		}

		for (final TableColumn c : table.getColumns()) {
			c.pack();
		}

		final CellEditor[] editors = new CellEditor[6];
		editors[0] = new CheckboxCellEditor(table);
		editors[1] = new CheckboxCellEditor(table);
		editors[2] = new TextCellEditor(table);
		editors[3] = new TextCellEditor(table);
		editors[4] = new CheckboxCellEditor(table);
		editors[5] = new CheckboxCellEditor(table);

		viewer.setColumnProperties(PROPERTIES);
		viewer.setCellModifier(new PossibleProcessCellModifier());
		viewer.setCellEditors(editors);

		toolkit.paintBordersFor(composite);

		return container;
	}

	/**
	 * Return the initial size of the dialog
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(800, 500);
	}

	/**
	 * Getter of the 'Create'-marked processes
	 * 
	 * @return All processes that the user marked with 'Create'
	 */
	public Process[] getProcessesToCreate() {
		final List<Process> processes = new ArrayList<>();
		for (final PossibleProcess p : possibleProcesses) {
			if (p.create() && !p.existsInDiagram) {
				processes.add(p.getProcess());
			}
		}
		return processes.toArray(new Process[processes.size()]);
	}

	/**
	 * Get the id of the provider output
	 * 
	 * @param providerId
	 *            The id of the provider process
	 * @return The id of the provider output or null if no process found with
	 *         the given id
	 */
	public String getProviderExchangeId(final String providerId) {
		String id = null;
		int i = 0;
		while (i < possibleProcesses.size() && id == null) {
			final PossibleProcess pp = possibleProcesses.get(i);
			if (pp.getProcess().getId().equals(providerId)) {
				id = pp.getOutput().getId();
			} else {
				i++;
			}
		}
		return id;
	}

	/**
	 * Getter of the providers that the users checked with 'Connect'
	 * 
	 * @return An array of processes to connect in the graph
	 */
	public Process[] getProvidersToConnect() {
		final List<Process> processes = new ArrayList<>();
		for (final PossibleProcess p : possibleProcesses) {
			if (p.connectToOutput()) {
				processes.add(p.getProcess());
			}
		}
		final Process[] selection = new Process[processes.size()];
		processes.toArray(selection);
		return selection;
	}

	/**
	 * Get the id of the recipient input
	 * 
	 * @param recipientId
	 *            The id of the recipient process
	 * @return The id of the recipient input or null if no process found with
	 *         the given id
	 */
	public String getRecipientExchangeKey(final String recipientId) {
		String id = null;
		int i = 0;
		while (i < possibleProcesses.size() && id == null) {
			final PossibleProcess pp = possibleProcesses.get(i);
			if (pp.getProcess().getId().equals(recipientId)) {
				id = pp.getInput().getId();
			} else {
				i++;
			}
		}
		return id;
	}

	/**
	 * Getter of the recipients that the users checked with 'Connect'
	 * 
	 * @return An array of processes to connect in the graph
	 */
	public Process[] getRecipientsToConnect() {
		final List<Process> processes = new ArrayList<>();
		for (final PossibleProcess p : possibleProcesses) {
			if (p.connectToInput()) {
				processes.add(p.getProcess());
			}
		}
		final Process[] selection = new Process[processes.size()];
		processes.toArray(selection);
		return selection;
	}

	@Override
	public int open() {
		final int returnCode = super.open();
		productSystem = null;
		exchange = null;
		parentOfExchange = null;
		return returnCode;
	}

	/**
	 * Internal class providing the relevant information of processes that can
	 * be connected with the given exchange (specified at creation of the
	 * dialog)
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class PossibleProcess {

		/**
		 * If true this process should be connected (The product specified in
		 * the constructor is an input)
		 */
		private boolean connectToInput;

		/**
		 * If true this process should be connected (The product specified in
		 * the constructor is an output)
		 */
		private boolean connectToOutput;

		/**
		 * Indicates if the process should be created (added to the processes of
		 * the product system and painted in the graph)
		 */
		private boolean create;

		/**
		 * Indicates if the process already exists as a process node in the
		 * graph (diagram)
		 */
		private final boolean existsInDiagram;

		/**
		 * The input that can be connected (The product specified in the
		 * constructor is an output)
		 */
		private Exchange input;

		/**
		 * The output that can be connected (The product specified in the
		 * constructor is an input)
		 */
		private Exchange output;

		/**
		 * The process this "possible process" is representing
		 */
		private final Process process;

		/**
		 * Creates a new instance
		 * 
		 * @param process
		 *            The process this "possible process" is representing
		 * @param existsInDiagram
		 *            Indicates if the process already exists as a process node
		 *            in the graph (diagram)
		 */
		public PossibleProcess(final Process process,
				final boolean existsInDiagram) {
			this.process = process;
			this.existsInDiagram = existsInDiagram;
			connectToInput = false;
			connectToOutput = false;
			create = false;
		}

		/**
		 * Getter of the connect to input value
		 * 
		 * @return True if this process should be connected (The product
		 *         specified in the constructor is an input)
		 */
		public boolean connectToInput() {
			return connectToInput;
		}

		/**
		 * Getter of the connect to output value
		 * 
		 * @return True if this process should be connected (The product
		 *         specified in the constructor is an output)
		 */
		public boolean connectToOutput() {
			return connectToOutput;
		}

		/**
		 * Getter of the create value
		 * 
		 * @return True if the process should be created (added to the processes
		 *         of the product system and painted in the graph), false
		 *         otherwise
		 */
		public boolean create() {
			return create;
		}

		@Override
		public boolean equals(final Object arg0) {
			if (arg0 instanceof PossibleProcess) {
				return ((PossibleProcess) arg0).getProcess().equals(process);
			}
			return false;
		}

		/**
		 * Getter of the exists in diagram value
		 * 
		 * @return True if the process already exists as a process node in the
		 *         graph (diagram), false otherwise
		 */
		public boolean existsInDiagram() {
			return existsInDiagram;
		}

		/**
		 * Getter of the input
		 * 
		 * @return The input that can be connected (The product specified in the
		 *         constructor is an output)
		 */
		public Exchange getInput() {
			return input;
		}

		/**
		 * Getter of the output
		 * 
		 * @return The output that can be connected (The product specified in
		 *         the constructor is an input)
		 */
		public Exchange getOutput() {
			return output;
		}

		/**
		 * Getter of the process
		 * 
		 * @return The process this "possible process" is representing
		 */
		public Process getProcess() {
			return process;
		}

		/**
		 * Indicates if the process is already connected
		 * 
		 * @return True if the input is not null and if not connectToInput (this
		 *         would indicate that the user decided to connect), false
		 *         otherwise
		 */
		public boolean isConnectedToInput() {
			return input != null && !connectToInput;
		}

		/**
		 * Indicates if the process is already connected
		 * 
		 * @return True if the output is not null and if not connectToOutput
		 *         (this would indicate that the user decided to connect), false
		 *         otherwise
		 */
		public boolean isConnectedToOutput() {
			return output != null && !connectToOutput;
		}

		/**
		 * Setter of the connectToInput value
		 * 
		 * @param connect
		 *            The new value
		 */
		public void setConnectToInput(final boolean connect) {
			connectToInput = connect;
		}

		/**
		 * Setter of the connectToOutput value
		 * 
		 * @param connect
		 *            The new value
		 */
		public void setConnectToOutput(final boolean connect) {
			connectToOutput = connect;
		}

		/**
		 * Setter of the create value
		 * 
		 * @param create
		 *            The new value
		 */
		public void setCreate(final boolean create) {
			this.create = create;
		}

		/**
		 * Setter of the input
		 * 
		 * @param exchange
		 *            The new input
		 */
		public void setInput(final Exchange exchange) {
			input = exchange;
		}

		/**
		 * Setter of the output
		 * 
		 * @param exchange
		 *            The new output
		 */
		public void setOutput(final Exchange exchange) {
			output = exchange;
		}
	}

	/**
	 * Cell modifier for the table listing the possible providers/recipients
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class PossibleProcessCellModifier implements ICellModifier {

		@Override
		public boolean canModify(final Object element, final String property) {
			boolean canModify = false;
			if (element instanceof PossibleProcess) {
				final PossibleProcess process = (PossibleProcess) element;
				if (property.equals(CREATE) && !process.existsInDiagram()
						&& !process.connectToInput()
						&& !process.connectToOutput()) {
					canModify = true;
				}
				if (property.equals(CONNECT) && !exchange.isInput()
						&& !process.isConnectedToInput()) {
					canModify = true;
					for (final ProcessLink link : productSystem
							.getIncomingLinks(process.getProcess().getId())) {
						if (link.getRecipientInput().getFlow().getId()
								.equals(exchange.getFlow().getId())) {
							canModify = false;
							break;
						}
					}
				}
				if (property.equals(CONNECT) && exchange.isInput()
						&& !process.isConnectedToOutput()) {
					canModify = true;
					if (process.output == null) {
						// only on provider allowed per product
						for (final ProcessLink link : productSystem
								.getIncomingLinks(parentOfExchange.getId())) {
							if (link.getRecipientInput().getId()
									.equals(exchange.getId())) {
								canModify = false;
								break;
							}
						}
						if (canModify) {
							// check if another provider was already selected
							for (final PossibleProcess proc : possibleProcesses) {
								if (proc.getOutput() != null) {
									canModify = false;
									break;
								}
							}
						}
					}
				}
			}
			return canModify;
		}

		@Override
		public Object getValue(final Object element, final String property) {
			final PossibleProcess p = (PossibleProcess) element;
			Object value = null;
			if (property.equals(CREATE)) {
				value = p.create();
			} else if (property.equals(CONNECT) && !exchange.isInput()) {
				if (p.getInput() != null) {
					value = p.connectToInput();
				} else {
					value = false;
				}
			} else if (property.equals(CONNECT) && exchange.isInput()) {
				if (p.getOutput() != null) {
					value = p.connectToOutput();
				} else {
					value = false;
				}
			}
			return value;
		}

		@Override
		public void modify(final Object element, final String property,
				final Object value) {
			final TableItem item = (TableItem) element;
			final PossibleProcess p = (PossibleProcess) item.getData();
			if (property.equals(CREATE)) {
				p.setCreate(Boolean.parseBoolean(value.toString()));
			} else if (property.equals(CONNECT) && !exchange.isInput()) {
				p.setConnectToInput(Boolean.parseBoolean(value.toString()));
				if (p.connectToInput() && !p.existsInDiagram) {
					p.setCreate(true);
				}
				if (!p.connectToInput()) {
					p.setInput(null);
				} else {
					Exchange e = null;
					int i = 0;
					while (e == null && i < p.getProcess().getInputs().length) {
						if (p.getProcess().getInputs()[i].getFlow().getId()
								.equals(exchange.getFlow().getId())) {
							e = p.getProcess().getInputs()[i];
						} else {
							i++;
						}
					}
					p.setInput(e);
				}
			} else if (property.equals(CONNECT) && exchange.isInput()) {
				p.setConnectToOutput(Boolean.parseBoolean(value.toString()));
				if (p.connectToOutput() && !p.existsInDiagram) {
					p.setCreate(true);
				}
				if (!p.connectToOutput()) {
					p.setOutput(null);
				} else {
					Exchange e = null;
					int i = 0;
					while (e == null && i < p.getProcess().getOutputs().length) {
						if (p.getProcess().getOutputs()[i].getFlow().getId()
								.equals(exchange.getFlow().getId())) {
							e = p.getProcess().getOutputs()[i];
						} else {
							i++;
						}
					}
					p.setOutput(e);
				}
			}
			viewer.refresh();
		}
	}

	/**
	 * Content provider for the table that lists the possible
	 * providers/recipients
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class PossibleProcessContentProvider implements
			IStructuredContentProvider {

		@Override
		public void dispose() {

		}

		@Override
		public Object[] getElements(final Object inputElement) {
			return (PossibleProcess[]) inputElement;
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput,
				final Object newInput) {

		}

	}

	/**
	 * Label provider for the table that lists the possible providers/recipients
	 * 
	 * @author sg
	 * 
	 */
	private class PossibleProcessLabelProvider implements ITableLabelProvider {

		/**
		 * Indicates if the label provider is used for the provider table, if
		 * false the recipient table is used
		 */
		private final boolean providerTable;

		/**
		 * Creates a new instance
		 * 
		 * @param providerTable
		 *            Indicates if the label provider is used for the provider
		 *            table, if false the recipient table is used
		 */
		public PossibleProcessLabelProvider(final boolean providerTable) {
			this.providerTable = providerTable;
		}

		@Override
		public void addListener(final ILabelProviderListener listener) {

		}

		@Override
		public void dispose() {

		}

		@Override
		public Image getColumnImage(final Object element, final int columnIndex) {
			Image img = null;
			if (element instanceof PossibleProcess) {
				final PossibleProcess process = (PossibleProcess) element;
				if (columnIndex == 0) {
					if (process.create()) {
						img = ImageType.CHECK_TRUE.get();
					} else {
						img = ImageType.CHECK_FALSE.get();
					}
				} else if (columnIndex == 1) {
					if (process.connectToInput() && !providerTable
							|| process.connectToOutput() && providerTable) {
						img = ImageType.CHECK_TRUE.get();
					} else {
						img = ImageType.CHECK_FALSE.get();
					}
				} else if (columnIndex == 4) {
					if (process.existsInDiagram()) {
						img = ImageType.CHECK_TRUE.get();
					} else {
						img = ImageType.CHECK_FALSE.get();
					}
				} else if (columnIndex == 5) {
					if (process.isConnectedToInput() && !providerTable
							|| process.isConnectedToOutput() && providerTable) {
						img = ImageType.CHECK_TRUE.get();
					} else {
						img = ImageType.CHECK_FALSE.get();
					}
				}
			}
			return img;
		}

		@Override
		public String getColumnText(final Object element, final int columnIndex) {
			final PossibleProcess p = (PossibleProcess) element;
			String text = null;
			switch (columnIndex) {
			case 2:
				text = p.getProcess().getName();
				text += p.getProcess().getLocation() != null ? " ["
						+ p.getProcess().getLocation().getCode() + "]" : "";
				text += p.getProcess().isInfrastructureProcess() ? " _IP" : "";
				if (p.getProcess().getProcessType() == ProcessType.LCI_Result) {
					text += " _S";
				}
				break;
			case 3:
				Flow flow = null;
				if (p.getInput() != null && !providerTable) {
					flow = p.getInput().getFlow();
				} else if (p.getOutput() != null && providerTable) {
					flow = p.getOutput().getFlow();
				}
				if (flow != null) {
					text = flow.getName();
				} else {
					text = "";
				}
				break;
			}
			return text;
		}

		@Override
		public boolean isLabelProperty(final Object element,
				final String property) {
			return false;
		}

		@Override
		public void removeListener(final ILabelProviderListener listener) {

		}

	}

}
