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

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.openlca.app.Messages;
import org.openlca.core.editors.productsystem.graphical.GraphLayoutManager;
import org.openlca.core.editors.productsystem.graphical.model.ConnectionLink;
import org.openlca.core.editors.productsystem.graphical.model.ExchangeNode;
import org.openlca.core.editors.productsystem.graphical.model.ProcessNode;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;
import org.openlca.core.editors.productsystem.graphical.outline.ProcessTreeEditPart;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;

/**
 * Hides or shows a specific process node
 * 
 * @author Sebastian Greve
 * 
 */
public class HideShowCommand extends Command {

	/**
	 * The product system node of the graphical viewer
	 */
	private final ProductSystemNode node;

	/**
	 * The actual selection in the outline
	 */
	private StructuredSelection selection;

	/**
	 * Indicates if a node should be shown or hidden
	 */
	private boolean show;

	/**
	 * The outline tree viewer
	 */
	private final TreeViewer viewer;

	/**
	 * Creates a new hide/show command
	 * 
	 * @param viewer
	 *            The outline tree viewer
	 * @param productSystemNode
	 *            The product system node of the graphical viewer
	 * @param show
	 *            Indicates if a node should be shown or hidden
	 */
	public HideShowCommand(final TreeViewer viewer,
			final ProductSystemNode productSystemNode, final boolean show) {
		this.viewer = viewer;
		this.show = show;
		this.node = productSystemNode;
	}

	/**
	 * Exexcutes the command
	 */
	private void exec() {
		for (final Object o : selection.toArray()) {
			if (o instanceof ProcessTreeEditPart) {
				final ProcessTreeEditPart editPart = (ProcessTreeEditPart) o;
				final Process process = (Process) editPart.getModel();
				ProcessNode processNode = node.getProcessNode(process.getId());
				if (show && processNode == null) {
					processNode = new ProcessNode(node.getProductSystem()
							.getProcess(process.getId()), true);
					node.addChild(processNode);
					for (final ProcessLink link : node.getProductSystem()
							.getProcessLinks(process.getId())) {
						final Process p = link.getRecipientProcess().getId()
								.equals(process.getId()) ? link
								.getProviderProcess() : link
								.getRecipientProcess();
						final ProcessNode newNode = node.getProcessNode(p
								.getId());
						if (newNode != null) {
							final ProcessNode sourceNode = link
									.getRecipientProcess().getId()
									.equals(process.getId()) ? newNode
									: processNode;
							final ProcessNode targetNode = link
									.getRecipientProcess().getId()
									.equals(process.getId()) ? processNode
									: newNode;
							final ConnectionLink connectionLink = new ConnectionLink(
									sourceNode.getExchangeNode(link
											.getProviderOutput().getId()),
									targetNode.getExchangeNode(link
											.getRecipientInput().getId()), link);
							connectionLink.link();
						}
					}

				}
				if (processNode != null) {
					if (!show) {
						viewer.setSelection(new StructuredSelection());
						for (final ExchangeNode eNode : processNode
								.getExchangeNodes()) {
							for (final ConnectionLink link : eNode.getLinks()) {
								link.getFigure().setVisible(false);
							}
						}
					}
					processNode.getFigure().setVisibility(show);
					processNode.getFigure().setVisible(show);
					if (show) {
						viewer.setSelection(selection);
						for (final ExchangeNode eNode : processNode
								.getExchangeNodes()) {
							for (final ConnectionLink link : eNode.getLinks()) {
								if (link.getTargetNode()
										.getParentProcessNode()
										.getProcess()
										.getId()
										.equals(processNode.getProcess()
												.getId())) {
									if (link.getSourceNode()
											.getParentProcessNode().getFigure()
											.isVisible()
											&& (link.getSourceNode()
													.getParentProcessNode()
													.getFigure()
													.isExpandedRight() || link
													.getTargetNode()
													.getParentProcessNode()
													.getFigure()
													.isExpandedLeft())) {
										link.getFigure().setVisible(true);
									}
								} else if (link
										.getSourceNode()
										.getParentProcessNode()
										.getProcess()
										.getId()
										.equals(processNode.getProcess()
												.getId())) {
									if (link.getTargetNode()
											.getParentProcessNode().getFigure()
											.isVisible()
											&& (link.getSourceNode()
													.getParentProcessNode()
													.getFigure()
													.isExpandedRight() || link
													.getTargetNode()
													.getParentProcessNode()
													.getFigure()
													.isExpandedLeft())) {
										link.getFigure().setVisible(true);
									}
								}
							}
						}
					}
				}
			}
		}
		((GraphLayoutManager) node.getFigure().getLayoutManager()).layout(
				node.getFigure(), node.getEditor().getLayoutType());
	}

	@Override
	public boolean canExecute() {
		return !viewer.getSelection().isEmpty();
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		selection = (StructuredSelection) viewer.getSelection();
		exec();
	}

	@Override
	public String getLabel() {
		return show ? Messages.Systems_HideShowCommand_ShowText
				: Messages.Systems_HideShowCommand_HideText;
	}

	@Override
	public void redo() {
		exec();
	}

	@Override
	public void undo() {
		show = !show;
		exec();
		show = !show;
	}

}
