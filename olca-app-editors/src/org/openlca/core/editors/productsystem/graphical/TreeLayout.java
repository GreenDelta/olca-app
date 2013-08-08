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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.core.editors.productsystem.graphical.model.ProcessFigure;
import org.openlca.core.editors.productsystem.graphical.model.ProcessNode;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

/**
 * Lays out the diagram as a tree
 * 
 * @author Sebastian Greve
 * 
 */
public class TreeLayout {

	/**
	 * Reminder
	 */
	private final Map<String, Integer> containing = new HashMap<>();

	/**
	 * Height of each column
	 */
	private final Map<Integer, Integer> heights = new HashMap<>();

	/**
	 * Locations of the nodes (key = x,y coordinates, value = process key)
	 */
	private final Map<Point, String> locations = new HashMap<>();

	/**
	 * Widths of each row
	 */
	private final Map<Integer, Integer> widths = new HashMap<>();

	/**
	 * Applies the layout
	 * 
	 * @param node
	 *            The node to be layed out
	 * @param addition
	 *            The height addition
	 * @param actualDepth
	 *            The actual deepth
	 */
	private void applyLayout(final Node node, int addition,
			final int actualDepth) {
		final int x = actualDepth;
		int y = node.getSize() / 2 + addition;
		while (locations.get(new Point(x, y)) != null) {
			y++;
			addition++;
		}
		locations.put(new Point(x, y), node.processId);
		for (int i = 0; i < node.leftChildren.size(); i++) {
			final Node child = node.leftChildren.get(i);
			int sizeAddition = 0;
			for (int j = 0; j < i; j++) {
				sizeAddition += node.leftChildren.get(j).getSize();
			}
			applyLayout(child, addition + sizeAddition, actualDepth - 1);
		}
		for (int i = 0; i < node.rightChildren.size(); i++) {
			final Node child = node.rightChildren.get(i);
			int sizeAddition = 0;
			for (int a = 0; a < node.leftChildren.size(); a++) {
				sizeAddition += node.leftChildren.get(a).getSize();
			}
			for (int j = 0; j < i; j++) {
				sizeAddition += node.rightChildren.get(j).getSize();
			}
			applyLayout(child, addition + sizeAddition, actualDepth + 1);
		}
	}

	/**
	 * Builds a graph for the product system
	 * 
	 * @param productSystem
	 *            The product system a graph should be build for
	 * @return a graph for the product system
	 */
	private Node build(final ProductSystem productSystem) {
		final Node node = new Node();
		node.processId = productSystem.getReferenceProcess().getId();
		build(productSystem, new Node[] { node });
		return node;
	}

	/**
	 * Builds a graph for the product system
	 * 
	 * @param productSystem
	 *            The product system a graph should be build for
	 * @param nodes
	 *            the nodes that should be build in
	 */
	private void build(final ProductSystem productSystem, final Node[] nodes) {
		final List<Node> children = new ArrayList<>();
		for (final Node node : nodes) {
			final String processId = node.processId;
			for (final ProcessLink link : productSystem
					.getProcessLinks(processId)) {
				if (link.getRecipientProcess().getId().equals(processId)) {
					if (containing.get(link.getProviderProcess().getId()) == null) {
						final Node child = new Node();
						child.processId = link.getProviderProcess().getId();
						node.leftChildren.add(child);
						containing.put(child.processId, 1);
						children.add(child);
					}
				}
			}
		}
		if (children.size() > 0) {
			build(productSystem, children.toArray(new Node[children.size()]));
		}
		children.clear();
		for (final Node node : nodes) {
			final String processId = node.processId;
			for (final ProcessLink link : productSystem
					.getProcessLinks(processId)) {
				if (link.getProviderProcess().getId().equals(processId)) {
					if (containing.get(link.getRecipientProcess().getId()) == null) {
						final Node child = new Node();
						child.processId = link.getRecipientProcess().getId();
						node.rightChildren.add(child);
						containing.put(child.processId, 1);
						children.add(child);
					}
				}
			}
		}
		if (children.size() > 0) {
			build(productSystem, children.toArray(new Node[children.size()]));
		}
	}

	/**
	 * Prepares the product system node. Collects all invisible nodes (they
	 * should not be regarded in the layout)
	 * 
	 * @param productSystemNode
	 *            The product system node the layout should be applied to
	 */
	private void prepare(final ProductSystemNode productSystemNode) {
		for (final Object node : productSystemNode.getChildrenArray()) {
			if (node instanceof ProcessNode) {
				final ProcessNode processNode = (ProcessNode) node;
				if (!processNode.getFigure().isVisible()) {
					containing.put(processNode.getProcess().getId(), 1);
					processNode.setXyLayoutConstraints(new Rectangle(0, 0,
							processNode.getFigure().getSize().width,
							processNode.getFigure().getSize().height));
				}
			}
		}
		for (final Process process : productSystemNode.getProductSystem()
				.getProcesses()) {
			if (productSystemNode.getProcessNode(process.getId()) == null) {
				containing.put(process.getId(), 1);
			}
		}
	}

	/**
	 * Getter of the heights map
	 * 
	 * @return The heights map
	 */
	public Map<Integer, Integer> getHeights() {
		return heights;
	}

	/**
	 * Getter of the widths map
	 * 
	 * @return The widths map
	 */
	public Map<Integer, Integer> getWidths() {
		return widths;
	}

	/**
	 * Applies the layout to the product system node
	 * 
	 * @param productSystemNode
	 *            the product system node the layout should be applied to
	 */
	public void layout(final ProductSystemNode productSystemNode) {
		prepare(productSystemNode);
		final List<Node> nodes = new ArrayList<>();
		final Node mainNode = build(productSystemNode.getProductSystem());
		mainNode.sort();
		nodes.add(mainNode);
		for (final Object o : productSystemNode.getChildrenArray()) {
			if (o instanceof ProcessNode) {
				final ProcessNode processNode = (ProcessNode) o;
				if (containing.get(processNode.getProcess().getId()) == null) {
					final Node node = new Node();
					node.processId = processNode.getProcess().getId();
					build(productSystemNode.getProductSystem(),
							new Node[] { node });
					node.sort();
					nodes.add(node);
				}
			}
		}
		int additionalHeight = 0;
		for (final Node node : nodes) {
			int newAdditionalHeight = 0;
			locations.clear();
			applyLayout(node, 0, node.getLeftDepth());

			int minimumX = Integer.MAX_VALUE;
			int maximumX = Integer.MIN_VALUE;
			int minimumY = Integer.MAX_VALUE;
			int maximumY = Integer.MIN_VALUE;

			for (final Point p : locations.keySet()) {
				if (p.x < minimumX) {
					minimumX = p.x;
				}
				if (p.x > maximumX) {
					maximumX = p.x;
				}
				if (p.y < minimumY) {
					minimumY = p.y;
				}
				if (p.y > maximumY) {
					maximumY = p.y;
				}
			}
			final Map<String, ProcessFigure> figures = new HashMap<>();
			for (final Object n : productSystemNode.getChildrenArray()) {
				if (n instanceof ProcessNode) {
					final ProcessFigure figure = ((ProcessNode) n).getFigure();
					figures.put(figure.getProcessNode().getProcess().getId(),
							figure);
				}
			}

			for (int x = minimumX; x <= maximumX; x++) {
				widths.put(x, 0);
				for (int y = minimumY; y <= maximumY; y++) {
					final String processId = locations.get(new Point(x, y));
					if (processId != null) {
						widths.put(x, Math.max(widths.get(x),
								figures.get(processId).getSize().width));
					}
				}
			}
			for (int y = minimumY; y <= maximumY; y++) {
				heights.put(y, 0);
				for (int x = minimumX; x <= maximumX; x++) {
					final String processId = locations.get(new Point(x, y));
					if (processId != null) {
						heights.put(y, Math.max(heights.get(y),
								figures.get(processId).getSize().height));
					}
				}
			}
			int xPosition = GraphLayoutManager.horizontalSpacing;
			for (int x = minimumX; x <= maximumX; x++) {
				if (x > minimumX) {
					if (widths.get(x - 1) > 0) {
						xPosition += widths.get(x - 1)
								+ GraphLayoutManager.horizontalSpacing;
					}
				}
				int yPosition = GraphLayoutManager.verticalSpacing;
				for (int y = minimumY; y <= maximumY; y++) {
					final String processId = locations.get(new Point(x, y));
					if (y > minimumY) {
						if (heights.get(y - 1) > 0) {
							yPosition += heights.get(y - 1)
									+ GraphLayoutManager.verticalSpacing;
						}
					}
					if (processId != null) {
						final ProcessFigure figure = figures.get(processId);
						figure.getProcessNode().setXyLayoutConstraints(
								new Rectangle(xPosition, yPosition
										+ additionalHeight,
										figure.getSize().width, figure
												.getSize().height));
						newAdditionalHeight = Math.max(newAdditionalHeight,
								yPosition + additionalHeight
										+ figure.getSize().height);
					}
				}
			}
			additionalHeight = newAdditionalHeight
					+ GraphLayoutManager.verticalSpacing;
		}
		containing.clear();
		widths.clear();
		heights.clear();
		locations.clear();
	}

	/**
	 * Graph node
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	class Node {

		/**
		 * The left children of the node
		 */
		List<Node> leftChildren = new ArrayList<>();

		/**
		 * The key of the process the node is representing
		 */
		String processId;

		/**
		 * The right children of the node
		 */
		List<Node> rightChildren = new ArrayList<>();

		/**
		 * Getter of the left-depth
		 * 
		 * @return The depth of the node to the left
		 */
		int getLeftDepth() {
			int depth = 0;
			if (leftChildren.size() > 0) {
				depth = 1;
				int depthAdd = 0;
				for (int i = 0; i < leftChildren.size(); i++) {
					depthAdd = Math.max(depthAdd, leftChildren.get(i)
							.getLeftDepth());
				}
				depth += depthAdd;
			}
			return depth;
		}

		/**
		 * Getter of the size of the node
		 * 
		 * @return The size of the node
		 */
		int getSize() {
			int size = 0;
			if (rightChildren.size() == 0 && leftChildren.size() == 0) {
				size = 1;
			} else {
				for (int i = 0; i < rightChildren.size(); i++) {
					size += rightChildren.get(i).getSize();
				}
				for (int i = 0; i < leftChildren.size(); i++) {
					size += leftChildren.get(i).getSize();
				}
			}
			return size;
		}

		/**
		 * Sorts the nodes children
		 */
		void sort() {
			final List<Node> temp = new ArrayList<>();
			temp.addAll(leftChildren);
			Collections.sort(temp, new Comparator<Node>() {

				@Override
				public int compare(final Node o1, final Node o2) {
					return ((Integer) o2.getSize()).compareTo(o1.getSize());
				}

			});
			leftChildren.clear();
			int count = 0;
			int i = 0;
			while (count < temp.size()) {
				leftChildren.add(temp.get(i));
				count++;
				if (count < temp.size()) {
					leftChildren.add(temp.get(temp.size() - i - 1));
					count++;
				}
				i++;
			}
		}

	}

}
