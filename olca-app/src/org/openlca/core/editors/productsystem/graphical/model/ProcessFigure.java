/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.CompoundBorder;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Labels;
import org.openlca.core.editors.productsystem.graphical.actions.ExpandFoldCommand;
import org.openlca.core.editors.productsystem.graphical.actions.MaximizeCommand;
import org.openlca.core.editors.productsystem.graphical.actions.MinimizeCommand;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProductSystem;

/**
 * Figure of a {@link ProcessNode}
 * 
 * @author Sebastian Greve
 * 
 */
public class ProcessFigure extends Figure implements PropertyChangeListener {

	/**
	 * line color
	 */
	private static final Color lineColor = ColorConstants.gray;

	/**
	 * text color
	 */
	private static final Color textColor = ColorConstants.black;

	/**
	 * minimum height of a process figure (the title)
	 */
	public static int minimumHeight = 21;

	/**
	 * the minimum width of a process figure
	 */
	public static int minimumWidth = 125;

	/**
	 * Image figure for the expand / collapse button on the left
	 */
	private ImageFigure expandCollapseLeft = new ImageFigure();

	/**
	 * Image figure for the expand / collapse button on the right
	 */
	private ImageFigure expandCollapseRight = new ImageFigure();

	/**
	 * Indicates if the process node is expanded to the left
	 */
	private boolean expandedLeft = false;

	/**
	 * Indicates if the process node is expanded to the right
	 */
	private boolean expandedRight = false;

	/**
	 * Saves the size before minimizing the figure and reads it if maximizing
	 * the figure
	 */
	private Dimension oldSize;

	/**
	 * the minimum height of this figure including the exchange figures
	 */
	private int privateMinimumHeight = 21;

	/**
	 * The {@link ProcessNode} this figure belongs to
	 */
	private ProcessNode processNode;

	/**
	 * Indicates if the process is an unit process or an LCI result
	 */
	private boolean result;

	/**
	 * Internal visibility field. Needed to check if the setVisible() method can
	 * be applied
	 */
	private Boolean visibility = null;

	/**
	 * Constructor of a new ProcessFigure, sets the private height, calculates
	 * the initial size, sets the layout to {@link GridLayout}, adds the label
	 * with the process name to the title and adds a {@link MouseListener} for
	 * recognizing double click on figure to minimize/maximize
	 * 
	 * @param processNode
	 *            the {@link ProcessNode} this figure belongs to
	 */
	public ProcessFigure(final ProcessNode processNode) {
		if (processNode.getProcess().getProcessType() == ProcessType.LCI_Result) {
			result = true;
		}
		setToolTip(new Label(Labels.processType(processNode.getProcess())
				+ ": " + processNode.getName()));
		setBounds(new Rectangle(0, 0, 0, 0));
		this.processNode = processNode;
		initHeight(processNode);
		setSize(calculateSize());
		oldSize = calculateSize();
		final GridLayout layout = new GridLayout(1, true);
		layout.horizontalSpacing = 10;
		layout.verticalSpacing = ExchangeFigure.height - 15;
		layout.marginHeight = 2;
		layout.marginWidth = 5;
		setLayoutManager(layout);

		setForegroundColor(textColor);
		final Figure top = new Figure();
		expandCollapseLeft.setImage(ImageType.PLUS_ICON.get());
		expandCollapseRight.setImage(ImageType.PLUS_ICON.get());

		final GridLayout topLayout = new GridLayout(3, false);
		topLayout.horizontalSpacing = 0;
		topLayout.verticalSpacing = 0;
		topLayout.marginHeight = 0;
		topLayout.marginWidth = 0;
		top.setLayoutManager(topLayout);

		top.add(expandCollapseLeft, new GridData(SWT.LEFT, SWT.CENTER, false,
				false));
		top.add(new Label(processNode.getName()), new GridData(SWT.FILL,
				SWT.FILL, true, false));
		top.add(expandCollapseRight, new GridData(SWT.RIGHT, SWT.CENTER, false,
				false));
		add(top, new GridData(SWT.FILL, SWT.FILL, true, false));

		final GridData dummyGridData = new GridData(SWT.FILL, SWT.FILL, true,
				false);
		dummyGridData.heightHint = 30;
		add(new Figure(), dummyGridData);

		drawBorder();

		final ProductSystem productSystem = ((ProductSystemNode) processNode
				.getParent()).getProductSystem();
		int i = 0;
		boolean hasOutgoingLinks = false;
		boolean hasIncomingLinks = false;
		final ProcessLink[] links = productSystem.getProcessLinks(processNode
				.getProcess().getId());
		while ((!hasIncomingLinks || !hasOutgoingLinks) && i < links.length) {
			if (links[i].getProviderProcess().getId() == processNode
					.getProcess().getId()) {
				hasOutgoingLinks = true;
			} else if (links[i].getRecipientProcess().getId() == processNode
					.getProcess().getId()) {
				hasIncomingLinks = true;
			}
			i++;
		}
		if (!hasIncomingLinks) {
			expandCollapseLeft.setVisible(false);
		}
		if (!hasOutgoingLinks) {
			expandCollapseRight.setVisible(false);
		}
		if (productSystem.getReferenceProcess().getId() == processNode
				.getProcess().getId()) {
			setExpandedLeft(true);
		}

		initializeMouseListeners();
	}

	private void drawBorder() {
		if (result) {
			LineBorder outer = new LineBorder(lineColor, 1);
			LineBorder innerInner = new LineBorder(lineColor, 1);
			LineBorder innerOuter = new LineBorder(ColorConstants.white, 1);
			CompoundBorder inner = new CompoundBorder(innerOuter, innerInner);
			CompoundBorder border = new CompoundBorder(outer, inner);
			setBorder(border);
		} else {
			LineBorder border = new LineBorder(lineColor, 1);
			setBorder(border);
		}
	}

	private void initHeight(ProcessNode processNode) {
		int inputs = 0;
		int outputs = 0;
		for (Exchange e : processNode.getProcess().getExchanges()) {
			FlowType type = e.getFlow().getFlowType();
			if (type == FlowType.ELEMENTARY_FLOW)
				continue;
			if (e.isInput())
				inputs++;
			else
				outputs++;
		}
		final int length = Math.max(inputs, outputs);
		privateMinimumHeight = 55 + length * ExchangeFigure.height
				+ (result ? 3 : 0);
	}

	/**
	 * Expands the process node figures
	 * 
	 * @param visited
	 *            Already visited process nodes (in previous callings)
	 * @param processNodes
	 *            The nodes to expand
	 * @param startProcessId
	 *            The id of the process expanding started with
	 */
	private void callExpandRecursion(final List<ProcessNode> visited,
			final List<ProcessNode> processNodes, final long startProcessId) {
		for (final ProcessNode processNode : processNodes) {
			if (processNode.getProcess().getId() == startProcessId)
				continue;
			if (processNode.getFigure().expandedLeft)
				processNode.getFigure().expand(visited, true, startProcessId);
			if (processNode.getFigure().expandedRight)
				processNode.getFigure().expand(visited, false, startProcessId);
		}
	}

	/**
	 * Folds the process node figures
	 * 
	 * @param visited
	 *            Already visited process nodes (in previous callings)
	 * @param processNodes
	 *            The nodes to fold
	 * @param startProcessId
	 *            The id of the process folding started with
	 */
	private void callFoldRecursion(List<ProcessNode> visited,
			List<ProcessNode> processNodes, long startProcessId) {
		for (ProcessNode processNode : processNodes) {
			if (processNode.getProcess().getId() == startProcessId)
				continue;
			if (processNode.getFigure().expandedLeft)
				processNode.getFigure().fold(visited, true, startProcessId);
			if (processNode.getFigure().expandedRight)
				processNode.getFigure().fold(visited, false, startProcessId);
		}
	}

	/**
	 * Checks if any visible process that would be set invisible must stay
	 * visible because it is linked with other processes visible
	 * 
	 * @param left
	 *            Indicates if the left (recipient) side will be folded (if
	 *            false the providing right side will be folded)
	 * @param processNodes
	 *            The nodes relevant
	 * @param startProcessId
	 *            The id of the process the checking has started with
	 * @return A list of process nodes that can be set invisible
	 */
	private List<ProcessNode> checkIfNodesCanBeFolded(final boolean left,
			final List<ProcessNode> processNodes, final long startProcessId) {
		final List<ProcessNode> remainingProcessNodes = new ArrayList<>();
		// for each process node
		for (final ProcessNode processNode : processNodes) {
			final boolean hasToBeVisible = hasToBeVisible(processNode,
					startProcessId, left);
			// if not has to be visible
			if (!hasToBeVisible) {
				// set invisible
				processNode.getFigure().firePropertyChange("SELECT", true,
						false);
				processNode.getFigure().setVisibility(false);
				processNode.getFigure().setVisible(false);
				remainingProcessNodes.add(processNode);
			}
		}
		return remainingProcessNodes;
	}

	/**
	 * Search processes that have to be painted, paint them and link them to
	 * existing nodes
	 * 
	 * @param left
	 *            Indicates if the left (recipient) side will be folded (if
	 *            false the providing right side will be folded)
	 * @return A list of created nodes
	 */
	private List<ProcessNode> getAndShowNodesToExpand(final boolean left) {
		final List<ProcessNode> processNodes = new ArrayList<>();
		final ProductSystemNode psNode = (ProductSystemNode) processNode
				.getParent();
		// paint unpainted processes and links
		final ProcessLink[] links = left ? psNode.getProductSystem()
				.getIncomingLinks(processNode.getProcess().getId()) : psNode
				.getProductSystem().getOutgoingLinks(
						processNode.getProcess().getId());
		// for each process link
		for (final ProcessLink link : links) {
			final Process p = left ? link.getProviderProcess() : link
					.getRecipientProcess();
			ProcessNode node = psNode.getProcessNode(p.getId());
			if (node == null) {
				node = new ProcessNode(p, true);
				psNode.addChild(node);
			}
			final ProcessNode sourceNode = left ? node : processNode;
			final ProcessNode targetNode = left ? processNode : node;
			final ConnectionLink connectionLink = new ConnectionLink(
					sourceNode
							.getExchangeNode(link.getProviderOutput().getId()),
					targetNode
							.getExchangeNode(link.getRecipientInput().getId()),
					link);
			connectionLink.link();
		}

		// for each exchange node
		for (final ExchangeNode exchangeNode : processNode.getExchangeNodes()) {
			// for each connection link
			for (final ConnectionLink link : exchangeNode.getLinks()) {
				final ExchangeNode e1 = left ? link.getTargetNode() : link
						.getSourceNode();
				final ExchangeNode e2 = left ? link.getSourceNode() : link
						.getTargetNode();
				// if parent node equals process node
				if (e1.getParentProcessNode().getProcess().getId() == processNode
						.getProcess().getId()) {
					// if node process nodes contains parent of e2
					if (!processNodes.contains(e2.getParentProcessNode())) {
						// set visible
						e2.getParentProcessNode().getFigure()
								.setVisibility(true);
						e2.getParentProcessNode().getFigure().setVisible(true);
						processNodes.add(e2.getParentProcessNode());
					}
				}
			}
		}
		return processNodes;
	}

	/**
	 * Checks which processes have to be folded
	 * 
	 * @param left
	 *            Indicates if the left (recipient) side will be folded (if
	 *            false the providing right side will be folded)
	 * @param startProcessId
	 *            The id of the process the folding started with
	 * @return A list of process nodes to fold
	 */
	private List<ProcessNode> getNodesToFold(final boolean left,
			final long startProcessId) {
		final List<ProcessNode> processNodes = new ArrayList<>();
		// for each exchange node
		for (final ExchangeNode exchangeNode : processNode.getExchangeNodes()) {
			// for each connection link
			for (final ConnectionLink link : exchangeNode.getLinks()) {
				final ExchangeNode e1 = left ? link.getTargetNode() : link
						.getSourceNode();
				final ExchangeNode e2 = left ? link.getSourceNode() : link
						.getTargetNode();
				// if parent process node equals process node
				if (e1.getParentProcessNode().getProcess().getId() == processNode
						.getProcess().getId()) {
					// if not process node contains e2 parent node and e2's
					// parent node is not the start process
					if (!processNodes.contains(e2.getParentProcessNode())
							&& e2.getParentProcessNode().getProcess().getId() != startProcessId) {
						processNodes.add(e2.getParentProcessNode());
					}
				}
			}
		}
		return processNodes;
	}

	/**
	 * Checks if any visible process that would be set invisible must stay
	 * visible because it is linked with other processes visible
	 * 
	 * @param processNode
	 *            The node to check
	 * @param startProcessId
	 *            The id of the process the operation started with
	 * @param left
	 *            Indicates if the left (recipient) side will be folded (if
	 *            false the providing right side will be folded)
	 * @return True if the process node has to stay visible, false otherwise
	 */
	private boolean hasToBeVisible(final ProcessNode processNode,
			final long startProcessId, final boolean left) {
		int i = 0;
		boolean hasToBeVisible = false;
		final ExchangeNode[] exchangeNodes = processNode.getExchangeNodes();
		// while not hast to be visible and more nodes left
		while (!hasToBeVisible && i < exchangeNodes.length) {
			int j = 0;
			// while not hast to be visible and more links left
			while (!hasToBeVisible && j < exchangeNodes[i].getLinks().size()) {
				final ConnectionLink link = exchangeNodes[i].getLinks().get(j);
				final ExchangeNode e1 = left ? link.getSourceNode() : link
						.getTargetNode();
				final ExchangeNode e2 = left ? link.getTargetNode() : link
						.getSourceNode();
				// if e1's parent process node equals the process node
				if (e1.getParentProcessNode().getProcess().getId() == processNode
						.getProcess().getId()) {
					final ProcessNode pNode = e2.getParentProcessNode();
					// if not pNode is the start process, the figure is visible
					// and is expanded right/left
					if (pNode.getProcess().getId() != startProcessId
							&& pNode.getFigure().isVisible()
							&& (left ? pNode.getFigure().expandedLeft : pNode
									.getFigure().expandedRight)) {
						hasToBeVisible = true;
					}
				}
				j++;
			}
			i++;
		}
		return hasToBeVisible;
	}

	/**
	 * Initializes the mouse listeners on the expand/collapse images
	 */
	private void initializeMouseListeners() {
		expandCollapseLeft.addMouseListener(new MouseListener() {

			@Override
			public void mouseDoubleClicked(final MouseEvent me) {

			}

			@Override
			public void mousePressed(final MouseEvent me) {
				if (expandedLeft) {
					final Command foldCommand = new ExpandFoldCommand(
							ProcessFigure.this, true, true);
					((ProductSystemNode) processNode.getParent()).getEditor()
							.getCommandStack().execute(foldCommand);
				} else {
					final Command expandCommand = new ExpandFoldCommand(
							ProcessFigure.this, true, false);
					((ProductSystemNode) processNode.getParent()).getEditor()
							.getCommandStack().execute(expandCommand);
				}
			}

			@Override
			public void mouseReleased(final MouseEvent me) {

			}

		});

		expandCollapseRight.addMouseListener(new MouseListener() {

			@Override
			public void mouseDoubleClicked(final MouseEvent me) {

			}

			@Override
			public void mousePressed(final MouseEvent me) {
				if (expandedRight) {
					final Command foldCommand = new ExpandFoldCommand(
							ProcessFigure.this, false, true);
					((ProductSystemNode) processNode.getParent()).getEditor()
							.getCommandStack().execute(foldCommand);
				} else {
					final Command expandCommand = new ExpandFoldCommand(
							ProcessFigure.this, false, false);
					((ProductSystemNode) processNode.getParent()).getEditor()
							.getCommandStack().execute(expandCommand);
				}
			}

			@Override
			public void mouseReleased(final MouseEvent me) {

			}

		});

		addMouseListener(new MouseListener() {

			private boolean firstClick = true;

			@Override
			public void mouseDoubleClicked(final MouseEvent arg0) {
			}

			@Override
			public void mousePressed(final MouseEvent arg0) {
				if (arg0.button == 1) {
					if (firstClick) {
						firstClick = false;
						final TimerTask timerTask = new TimerTask() {

							@Override
							public void run() {
								firstClick = true;
							}

						};
						final Timer timer = new Timer();
						timer.schedule(timerTask, 250);
					} else {
						Command command = null;
						if (processNode.isMinimized()) {
							command = new MaximizeCommand(processNode);
						} else {
							command = new MinimizeCommand(processNode);
						}
						((ProductSystemNode) processNode.getParent())
								.getEditor().getCommandStack().execute(command);
					}
				}
			}

			@Override
			public void mouseReleased(final MouseEvent me) {

			}

		});
	}

	/**
	 * Paints the table the exchanges are listed in
	 * 
	 * @param graphics
	 *            The graphics object performing the painting
	 */
	private void paintTable(final Graphics graphics) {
		graphics.setForegroundColor(lineColor);
		graphics.drawLine(new Point(getLocation().x + 5, getLocation().y + 42),
				new Point(getLocation().x + getSize().width - 5,
						getLocation().y + 42));

		if (getLocation().y + 26 < getLocation().y + getSize().height - 5) {
			graphics.drawLine(new Point(getLocation().x + getSize().width / 2
					- 1, getLocation().y + 26), new Point(getLocation().x
					+ getSize().width / 2 - 1, getLocation().y
					+ getSize().height - 5));
		}

		int divisor1 = 5;
		int divisor2 = 5;
		if (getSize().width < 175) {
			divisor1 = 6;
			divisor2 = 10;
		}

		graphics.setForegroundColor(textColor);
		graphics.drawText(Messages.Inputs, new Point(getLocation().x
				+ getSize().width / divisor1, getLocation().y + 25));
		graphics.drawText(Messages.Outputs, new Point(getLocation().x
				+ getSize().width / 2 + getSize().width / divisor2,
				getLocation().y + 25));
		graphics.setForegroundColor(ColorConstants.black);
	}

	/**
	 * paints the background of the title
	 * 
	 * @param graphics
	 *            The graphics object performing the painting
	 */
	private void paintTop(final Graphics graphics) {
		Image file = null;
		if (processNode.getProcess().getProcessType() == ProcessType.LCI_Result) {
			file = ImageType.PROCESS_BG_LCI.get();
		} else {
			file = ImageType.PROCESS_BG.get();
		}
		for (int i = 0; i < getSize().width - 20; i++) {
			graphics.drawImage(file, new Point(getLocation().x + i,
					getLocation().y));
		}
		graphics.setForegroundColor(lineColor);
		graphics.drawLine(new Point(getLocation().x, getLocation().y + 21),
				new Point(getLocation().x + getSize().width - 1,
						getLocation().y + 21));
		graphics.setForegroundColor(textColor);
	}

	/**
	 * Sets all links of the given process nodes invisible
	 * 
	 * @param processNodes
	 *            The nodes which links should be set invisible
	 */
	private void setLinksInvisible(final List<ProcessNode> processNodes) {
		for (final ProcessNode processNode : processNodes) {
			final ExchangeNode[] exchangeNodes = processNode.getExchangeNodes();
			for (final ExchangeNode exchangeNode : exchangeNodes) {
				for (final ConnectionLink link : exchangeNode.getLinks()) {
					link.getFigure().setVisible(false);
				}
			}
		}
	}

	/**
	 * Sets all links of the given process nodes visible
	 * 
	 * @param left
	 *            Indicates if the left (recipient) side will be set visible (if
	 *            false the providing right side will be set visible)
	 * @param processNodes
	 *            The nodes which links should be set visible
	 */
	private void setLinksVisible(final boolean left,
			final List<ProcessNode> processNodes) {
		// for each process node
		for (final ProcessNode processNode : processNodes) {
			// for each exchange node
			for (final ExchangeNode exchangeNode : processNode
					.getExchangeNodes()) {
				// for each connection link
				for (final ConnectionLink link : exchangeNode.getLinks()) {
					final ExchangeNode e1 = left ? link.getSourceNode() : link
							.getTargetNode();
					final ExchangeNode e2 = left ? link.getTargetNode() : link
							.getSourceNode();
					// check if process nodes are visible
					if (e1.getParentProcessNode().getFigure().isVisible()
							&& e2.getParentProcessNode().getFigure()
									.isVisible()) {
						// set visible
						link.getFigure().setVisible(true);
					}
				}
			}
		}
	}

	/**
	 * Minimizes the figure to the title size and calls the link to refresh
	 * their anchors
	 * 
	 * @param value
	 *            The new value
	 */
	private void setMinimized(final boolean value) {
		if (value) {
			oldSize = getSize();
			final Dimension p = calculateSize();
			int width = p.width;
			final int height = p.height;
			if (getSize().width > width) {
				width = getSize().width;
			}
			getParent().setConstraint(
					ProcessFigure.this,
					new Rectangle(getLocation().x - 1, getLocation().y - 1,
							width, height));
		} else {
			getParent().setConstraint(
					ProcessFigure.this,
					new Rectangle(getLocation().x - 1, getLocation().y - 1,
							oldSize.width, oldSize.height));
		}
		for (final ExchangeNode node : processNode.getExchangeNodes()) {
			for (final ConnectionLink link : node.getLinks()) {
				if (node.getExchange().isInput()) {
					link.refreshTargetAnchor();
				} else {
					link.refreshSourceAnchor();
				}
			}
		}
	}

	@Override
	protected void paintChildren(final Graphics graphics) {
		if (!processNode.isMinimized()) {
			super.paintChildren(graphics);
		} else {
			IFigure child;

			final Rectangle clip = Rectangle.SINGLETON;
			for (int i = 0; i < getChildren().size(); i++) {
				if (!(getChildren().get(i) instanceof ExchangeContainerFigure)) {
					child = (IFigure) getChildren().get(i);
					if (child.isVisible()
							&& child.intersects(graphics.getClip(clip))) {
						graphics.clipRect(child.getBounds());
						child.paint(graphics);
						graphics.restoreState();
					}
				}
			}
		}
	}

	@Override
	protected void paintFigure(final Graphics graphics) {
		graphics.pushState();
		graphics.setBackgroundColor(ColorConstants.white);
		graphics.fillRectangle(new Rectangle(getLocation().x, getLocation().y,
				getSize().width, getSize().height));
		paintTop(graphics);
		paintTable(graphics);
		graphics.popState();
		super.paintFigure(graphics);
	}

	/**
	 * Calculates the actual size
	 * 
	 * @return size of this figure
	 */
	public Dimension calculateSize() {
		int x = minimumWidth + (result ? 3 : 0);
		if (getSize() != null && getSize().width > x) {
			x = getSize().width;
		}
		int y = minimumHeight + (result ? 3 : 0);
		if (processNode != null && !processNode.isMinimized()) {
			y = privateMinimumHeight;
		}
		return new Dimension(x, y);
	}

	/**
	 * Disposes the figure and its children
	 */
	public void dispose() {
		processNode = null;
		expandCollapseLeft = null;
		expandCollapseRight = null;
		for (final Object figure : getChildren()) {
			if (figure instanceof ExchangeContainerFigure) {
				((ExchangeContainerFigure) figure).dispose();
			}
		}
		getChildren().clear();
	}

	/**
	 * Expands this node
	 * 
	 * @param visited
	 *            The processes already visited
	 * @param left
	 *            Indicates if the left (recipient) side will be expanded (if
	 *            false the providing right side will be expanded)
	 * @param startProcessId
	 *            The id of the process which started the expanding procedure
	 */
	public void expand(List<ProcessNode> visited, boolean left,
			long startProcessId) {
		if (!visited.contains(processNode)) {
			visited.add(processNode);
			List<ProcessNode> processNodes = getAndShowNodesToExpand(left);
			setLinksVisible(left, processNodes);
			callExpandRecursion(visited, processNodes, startProcessId);
		}
	}

	/**
	 * Folds this node
	 * 
	 * @param visited
	 *            The processes already visited
	 * @param left
	 *            Indicates if the left (recipient) side will be folded (if
	 *            false the providing right side will be folded)
	 * @param startProcessId
	 *            The id of the process which started the folding procedure
	 */
	public void fold(final List<ProcessNode> visited, final boolean left,
			final long startProcessId) {
		if (!visited.contains(processNode)) {
			visited.add(processNode);
			List<ProcessNode> processNodes = getNodesToFold(left,
					startProcessId);
			processNodes = checkIfNodesCanBeFolded(left, processNodes,
					startProcessId);
			setLinksInvisible(processNodes);
			callFoldRecursion(visited, processNodes, startProcessId);
		}
	}

	/**
	 * Gets the figures which belong to the exchanges
	 * 
	 * @return The exchange figure children of the process figure
	 */
	public ExchangeFigure[] getExchangeFigures() {
		final List<ExchangeFigure> figures = new ArrayList<>();
		for (final Object o : getChildren()) {
			for (final Object o2 : ((IFigure) o).getChildren()) {
				if (o2 instanceof ExchangeFigure) {
					figures.add((ExchangeFigure) o2);
				}
			}
		}
		final ExchangeFigure[] result = new ExchangeFigure[figures.size()];
		figures.toArray(result);
		return result;
	}

	/**
	 * Getter of {@link #privateMinimumHeight}
	 * 
	 * @return privateMinimumHeight
	 */
	public int getMinimumHeight() {
		return privateMinimumHeight;
	}

	@Override
	public Dimension getPreferredSize(final int hint, final int hint2) {
		final Dimension cSize = calculateSize();
		if (cSize.height > getSize().height || cSize.width > getSize().width
				|| processNode.isMinimized()) {
			return cSize;
		}
		return getSize();
	}

	/**
	 * Getter of {@link #processNode}
	 * 
	 * @return the ProcessNode
	 */
	public ProcessNode getProcessNode() {
		return processNode;
	}

	/**
	 * Indicates if the figure is expanded to the left (recipient) side
	 * 
	 * @return True if the figure is expanded to the left (recipient) side
	 */
	public boolean isExpandedLeft() {
		return expandedLeft;
	}

	/**
	 * Indicates if the figure is expanded to the right (providing) side
	 * 
	 * @return True if the figure is expanded to the right(providing) side
	 */
	public boolean isExpandedRight() {
		return expandedRight;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(ProcessNode.RESIZE_FIGURE)) {
			setMinimized((Boolean) evt.getNewValue());
		}
	}

	/**
	 * Sets the expand/collapse image's visibility
	 * 
	 * @param left
	 *            Indicates if the left (recipient) side's image's visibility
	 *            should be set
	 * @param value
	 *            The new value
	 */
	public void setExpandCollapseFigureVisible(final boolean left,
			final boolean value) {
		if (left) {
			expandCollapseLeft.setVisible(value);
		} else {
			expandCollapseRight.setVisible(value);
		}
	}

	/**
	 * Sets the left (recipient) side expanded (the image on the figure)
	 * 
	 * @param expandedLeft
	 *            The new value
	 */

	public void setExpandedLeft(final boolean expandedLeft) {
		this.expandedLeft = expandedLeft;
		if (expandedLeft) {
			expandCollapseLeft.setImage(ImageType.MINUS_ICON.get());
		} else {
			expandCollapseLeft.setImage(ImageType.PLUS_ICON.get());
		}
	}

	/**
	 * Sets the right (providing) side expanded (the image on the figure)
	 * 
	 * @param expandedRight
	 *            The new value
	 */
	public void setExpandedRight(final boolean expandedRight) {
		this.expandedRight = expandedRight;
		if (expandedRight) {
			expandCollapseRight.setImage(ImageType.MINUS_ICON.get());
		} else {
			expandCollapseRight.setImage(ImageType.PLUS_ICON.get());
		}
	}

	/**
	 * Setter of the internal visibility field
	 * 
	 * @param visibility
	 *            The visibility of the figure
	 */
	public void setVisibility(final Boolean visibility) {
		this.visibility = visibility;
	}

	@Override
	public void setVisible(final boolean visible) {
		if (visibility != null && visible == visibility) {
			super.setVisible(visible);
			visibility = null;
		}
	}
}
