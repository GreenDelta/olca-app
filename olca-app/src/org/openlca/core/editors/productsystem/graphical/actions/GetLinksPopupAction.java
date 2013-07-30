/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.openlca.app.UI;
import org.openlca.app.resources.ImageType;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.editors.productsystem.graphical.SelectPossibleProcessesDialog;
import org.openlca.core.editors.productsystem.graphical.model.ExchangeNode;
import org.openlca.core.editors.productsystem.graphical.model.ProcessNode;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

/**
 * Opens a {@link SelectPossibleProcessesDialog} for the selected
 * {@link ProcessNode}
 * 
 * @author Sebastian Greve
 * 
 */
public class GetLinksPopupAction extends Action {

	/**
	 * The id of the action
	 */
	public static final String ID = "org.openlca.core.editors.productsystem.graphical.actions.GetLinksPopupAction";

	/**
	 * The database
	 */
	private final IDatabase database;

	/**
	 * The menu of the action
	 */
	private Menu menu;

	/**
	 * The menu creator for the sub menu
	 */
	private final MenuCreator menuCreator;

	/**
	 * Indicates if providers or recipient are requested
	 */
	private boolean provider = false;

	/**
	 * Creates a new get links action
	 * 
	 * @param provider
	 *            Indicates if providers or recipient are requested
	 * @param database
	 *            The database
	 */
	public GetLinksPopupAction(final boolean provider, final IDatabase database) {
		super(provider ? Messages.Systems_GetLinksAction_ProviderText
				: Messages.Systems_GetLinksAction_RecipientText,
				IAction.AS_DROP_DOWN_MENU);
		this.provider = provider;
		this.database = database;
		menuCreator = new MenuCreator();
		setMenuCreator(menuCreator);
	}

	/**
	 * Creates a new sub menu for the specific process node
	 * 
	 * @param processNode
	 *            The selected process node
	 */
	private void createMenu(final ProcessNode processNode) {
		if (menu != null) {
			// for each menu item
			for (final MenuItem item : menu.getItems()) {
				// dispose
				item.dispose();
			}

			final List<Exchange> exchanges = new ArrayList<>();
			// for each exchange
			for (final Exchange exchange : processNode.getProcess()
					.getExchanges()) {
				// if exchange is non elementary and input(provider) or
				// output(recipient)
				if ((exchange.getFlow().getFlowType() == FlowType.PRODUCT_FLOW || exchange
						.getFlow().getFlowType() == FlowType.WASTE_FLOW)
						&& exchange.isInput() == provider) {
					// add to list
					exchanges.add(exchange);
				}
			}
			// sort exchanges
			Collections.sort(exchanges, new Comparator<Exchange>() {

				@Override
				public int compare(final Exchange o1, final Exchange o2) {
					// compare flow name
					return o1.getFlow().getName().toLowerCase()
							.compareTo(o2.getFlow().getName().toLowerCase());
				}

			});

			// for each exchange
			for (final Exchange exchange : exchanges) {
				// create menu item
				final MenuItem item = new MenuItem(menu, SWT.NONE);
				item.setText(exchange.getFlow().getName());
				ProductSystem productSystem = ((ProductSystemNode) processNode
						.getParent()).getProductSystem();
				if (isConnected(exchange, processNode.getProcess().getId(),
						productSystem)) {
					item.setImage(ImageType.PROCESS_CONNECTED.get());
				} else if (isExisting(exchange, productSystem)) {
					item.setImage(ImageType.PROCESS_EXISTING.get());
				}
				// add menu selection action
				item.addSelectionListener(new SelectionListener() {

					@Override
					public void widgetDefaultSelected(final SelectionEvent e) {
						// no action on default selection
					}

					@Override
					public void widgetSelected(final SelectionEvent e) {
						executeRequest(processNode, exchange);
					}
				});
			}
		} else {
			menuCreator.setFirstNode(processNode);
		}
	}

	/**
	 * Checks is the exchange is already connected
	 * 
	 * @param exchange
	 *            The exchange to check
	 * @param processId
	 *            the id of the process containing the exchange
	 * @param productSystem
	 *            The product system
	 * @return True if the exchange is already connected to another process
	 *         input/output, false otherwise
	 */
	private boolean isConnected(Exchange exchange, long processId,
			ProductSystem productSystem) {
		boolean connected = false;
		ProcessLink[] links = exchange.isInput() ? productSystem
				.getIncomingLinks(processId) : productSystem
				.getOutgoingLinks(processId);

		int i = 0;
		while (!connected && i < links.length) {
			long id = exchange.isInput() ? links[i].getRecipientInput().getId()
					: links[i].getProviderOutput().getId();
			if (id == exchange.getId()) {
				connected = true;
			} else {
				i++;
			}
		}

		return connected;
	}

	/**
	 * Checks is a process exists in the product system delivering/receiving the
	 * flow of the exchange
	 * 
	 * @param exchange
	 *            The exchange to check
	 * @param productSystem
	 *            The product system
	 * @return True if a process exists in the product system
	 *         delivering/receiving the flow of the exchange, false otherwise
	 */
	private boolean isExisting(Exchange exchange, ProductSystem productSystem) {
		boolean existing = false;
		int i = 0;
		while (!existing && i < productSystem.getProcesses().size()) {
			Process process = productSystem.getProcesses().get(i);
			Exchange[] exchanges = exchange.isInput() ? process.getOutputs()
					: process.getInputs();
			int j = 0;
			while (!existing && j < exchanges.length) {
				if (exchanges[j].getFlow().getId() == exchange.getFlow()
						.getId()) {
					existing = true;
				} else {
					j++;
				}
			}
			i++;
		}
		return existing;
	}

	/**
	 * Executes the request for the given node and exchange
	 * 
	 * @param processNode
	 *            The process node with the process containing the exchange
	 * @param exchange
	 *            The exchange to get the links for
	 */
	private void executeRequest(final ProcessNode processNode,
			final Exchange exchange) {
		// select processes to connect
		final SelectPossibleProcessesDialog multiObjectDialog = new SelectPossibleProcessesDialog(
				UI.shell(),
				((ProductSystemNode) processNode.getParent())
						.getProductSystem(), processNode.getProcess(),
				exchange, database);
		multiObjectDialog.open();
		if (multiObjectDialog.getReturnCode() == Window.OK) {
			// get processes to create
			final Process[] processesToCreate = multiObjectDialog
					.getProcessesToCreate();
			// get providers to connect
			final Process[] providersToConnect = multiObjectDialog
					.getProvidersToConnect();
			// get recipients to connect
			final Process[] recipientsToConnect = multiObjectDialog
					.getRecipientsToConnect();
			Command commandChain = null;
			final List<ProcessNode> createdProcessNodes = new ArrayList<>();
			// for each process to create
			for (final Process p : processesToCreate) {
				// create process create command
				final ProcessCreateCommand command = new ProcessCreateCommand();
				final ProcessNode pNode = new ProcessNode(p, false);
				command.setProcessNode(pNode);
				command.setProductSystemNode((ProductSystemNode) processNode
						.getParent());
				createdProcessNodes.add(pNode);

				// if first command
				if (commandChain == null) {
					// create chain
					commandChain = command;
				} else {
					// add to chain
					commandChain = commandChain.chain(command);
				}
			}

			// for each provider to connect
			for (final Process p : providersToConnect) {
				// create process link create command
				final ProcessLinkCreateCommand command = new ProcessLinkCreateCommand();
				final ProductSystemNode psNode = (ProductSystemNode) processNode
						.getParent();
				ProcessNode pNode = null;

				// for each created process node
				for (final ProcessNode node : createdProcessNodes) {
					if (node.getProcess().getId() == p.getId()) {
						// find process node for p
						pNode = node;
						break;
					}
				}
				if (pNode == null) {
					// process node already existed
					pNode = psNode.getProcessNode(p.getId());
				}

				ExchangeNode outputNode = null;
				// for each output
				for (final Exchange e : p.getOutputs()) {
					if (e.getId().equals(
							multiObjectDialog.getProviderExchangeId(p.getId()))) {
						// find exchange node for e
						outputNode = pNode.getExchangeNode(e.getId());
					}
				}
				command.setSourceNode(outputNode);
				ExchangeNode inputNode = null;
				// for each exchange node
				for (final ExchangeNode e : processNode.getExchangeNodes()) {
					// if e is input and matches output node
					if (e.getExchange().isInput() && e.matches(outputNode)) {
						inputNode = e;
					}
				}
				command.setTargetNode(inputNode);
				if (commandChain == null) {
					commandChain = command;
				} else {
					commandChain = commandChain.chain(command);
				}
			}

			// for each recipient to connect
			for (final Process p : recipientsToConnect) {
				// create process link create command
				final ProcessLinkCreateCommand command = new ProcessLinkCreateCommand();
				final ProductSystemNode psNode = (ProductSystemNode) processNode
						.getParent();
				ProcessNode pNode = null;

				// for each created process node
				for (final ProcessNode node : createdProcessNodes) {
					if (node.getProcess().getId().equals(p.getId())) {
						// get matching process node
						pNode = node;
						break;
					}
				}
				if (pNode == null) {
					// process node already existed
					pNode = psNode.getProcessNode(p.getId());
				}

				ExchangeNode inputNode = null;
				// for each input
				for (final Exchange e : p.getInputs()) {
					if (e.getId()
							.equals(multiObjectDialog.getRecipientExchangeKey(p
									.getId()))) {
						inputNode = pNode.getExchangeNode(e.getId());
					}
				}
				command.setTargetNode(inputNode);

				ExchangeNode outputNode = null;
				// for each exchange node
				for (final ExchangeNode e : processNode.getExchangeNodes()) {
					// if e is output and matches input node
					if (!e.getExchange().isInput() && e.matches(inputNode)) {
						outputNode = e;
					}
				}
				command.setSourceNode(outputNode);
				if (commandChain == null) {
					commandChain = command;
				} else {
					commandChain = commandChain.chain(command);
				}
			}
			((ProductSystemNode) processNode.getParent()).getEditor()
					.getCommandStack().execute(commandChain);
		}
	}

	/**
	 * Disposes the menus in the action
	 */
	public void dispose() {
		if (menuCreator != null) {
			menuCreator.dispose();
		}
		if (menu != null) {
			menu.dispose();
		}
	}

	@Override
	public String getId() {
		return ID + provider;
	}

	@Override
	public void run() {
		// nothing to do (pop up menu)
	}

	/**
	 * Creates a new sub menu for the given process node
	 * 
	 * @param processNode
	 *            The actual selected process node
	 */
	public void setProcessNode(final ProcessNode processNode) {
		createMenu(processNode);
	}

	/**
	 * The menu creator for the sub menu
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class MenuCreator implements IMenuCreator {

		/**
		 * The initial node
		 */
		private ProcessNode firstNode;

		/**
		 * Creates a new sub menu
		 * 
		 * @param parent
		 *            The parent menu
		 * @return The new sub menu
		 */
		private Menu createMenu(final Menu parent) {
			menu = new Menu(parent);
			GetLinksPopupAction.this.createMenu(firstNode);
			return menu;
		}

		/**
		 * Setter of the firstNode-field
		 * 
		 * @param firstNode
		 *            The initial node
		 */
		protected void setFirstNode(final ProcessNode firstNode) {
			this.firstNode = firstNode;
		}

		@Override
		public void dispose() {
			// nothing to dispose
		}

		/**
		 * Getter of the sub menu
		 * 
		 * @param control
		 *            The control of the menu
		 * @return the new sub menu
		 */
		@Override
		public Menu getMenu(final Control control) {
			final Menu menu = new Menu(control);
			createMenu(menu);
			control.setMenu(menu);
			return menu;
		}

		@Override
		public Menu getMenu(final Menu parent) {
			return createMenu(parent);
		}

	}
}
