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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.core.editors.productsystem.graphical.model.ConnectionLink;
import org.openlca.core.editors.productsystem.graphical.model.ExchangeNode;
import org.openlca.core.editors.productsystem.graphical.model.ProcessFigure;

/**
 * Lays out the product system diagram as a minimal tree
 * 
 * @author Sebastian Greve
 * 
 */
public class MinimalTreeLayout {

	/**
	 * The heights of the process nodes
	 */
	private final Map<Integer, Integer> heights = new HashMap<>();

	/**
	 * The widths of the process nodes
	 */
	private final Map<Integer, Integer> widths = new HashMap<>();

	/**
	 * Applies the layout to the given process figures
	 * 
	 * @param processFigures
	 *            The figures to be layed out
	 * @param graph
	 *            The internal graph build from the process figures
	 */
	private void applyLayout(final ProcessFigure[] processFigures,
			final Graph graph) {
		final Map<String, ProcessFigure> figures = new HashMap<>();
		// for each process figure
		for (final ProcessFigure figure : processFigures) {
			// create a map entry with process id and figure
			figures.put(figure.getProcessNode().getProcess().getId(), figure);
		}
		// build "width-map"
		for (int x = graph.minimumX; x <= graph.maximumX; x++) {
			widths.put(x, 0);
			for (int y = graph.minimumY; y <= graph.maximumY; y++) {
				final Node node = graph.coordinateSystem.get(new Point(x, y));
				if (node != null) {
					widths.put(x, Math.max(widths.get(x), figures.get(node.key)
							.getSize().width));
				}
			}
		}
		// build "height-map"
		for (int y = graph.minimumY; y <= graph.maximumY; y++) {
			heights.put(y, 0);
			for (int x = graph.minimumX; x <= graph.maximumX; x++) {
				final Node node = graph.coordinateSystem.get(new Point(x, y));
				if (node != null) {
					heights.put(y, Math.max(heights.get(y),
							figures.get(node.key).getSize().height));
				}
			}
		}
		int xPosition = 25;
		for (int x = graph.minimumX; x <= graph.maximumX; x++) {
			if (x > graph.minimumX) {
				if (widths.get(x - 1) > 0) {
					xPosition += widths.get(x - 1)
							+ GraphLayoutManager.horizontalSpacing;
				}
			}
			int yPosition = 25;
			for (int y = graph.minimumY; y <= graph.maximumY; y++) {
				final Node node = graph.coordinateSystem.get(new Point(x, y));
				if (y > graph.minimumY) {
					if (heights.get(y - 1) > 0) {
						yPosition += heights.get(y - 1)
								+ GraphLayoutManager.verticalSpacing;
					}
				}
				if (node != null) {
					final ProcessFigure figure = figures.get(node.key);
					figure.getProcessNode().setXyLayoutConstraints(
							new Rectangle(xPosition, yPosition, figure
									.getSize().width, figure.getSize().height));
				}
			}
		}
	}

	/**
	 * Builds a graph for the given figures
	 * 
	 * @param processFigures
	 *            The process figures a graph should be build for
	 * @return The builded graph
	 */
	private Graph buildGraph(final ProcessFigure[] processFigures) {
		final Graph graph = new Graph();
		for (final ProcessFigure processFigure : processFigures) {
			graph.nodes.put(
					processFigure.getProcessNode().getProcess().getId(),
					new Node(processFigure.getProcessNode().getProcess()
							.getId()));
		}
		for (final ProcessFigure processFigure : processFigures) {
			for (final ExchangeNode exchangeNode : processFigure
					.getProcessNode().getExchangeNodes()) {
				for (final ConnectionLink link : exchangeNode.getLinks()) {
					if (graph.nodes.get(link.getSourceNode()
							.getParentProcessNode().getProcess().getId()) != null
							&& graph.nodes.get(link.getTargetNode()
									.getParentProcessNode().getProcess()
									.getId()) != null) {
						// add edge to source node
						graph.nodes.get(link.getSourceNode()
								.getParentProcessNode().getProcess().getId()).outgoingEdges
								.add(new Edge(graph.nodes.get(link
										.getSourceNode().getParentProcessNode()
										.getProcess().getId()), graph.nodes
										.get(link.getTargetNode()
												.getParentProcessNode()
												.getProcess().getId())));
						// add edge to target node
						graph.nodes.get(link.getTargetNode()
								.getParentProcessNode().getProcess().getId()).incomingEdges
								.add(new Edge(graph.nodes.get(link
										.getSourceNode().getParentProcessNode()
										.getProcess().getId()), graph.nodes
										.get(link.getTargetNode()
												.getParentProcessNode()
												.getProcess().getId())));
					}
				}
			}
		}
		return graph;
	}

	/**
	 * Applies the layout to the given figures
	 * 
	 * @param processFigures
	 *            The figures the layout should be applied to
	 */
	public void layout(final ProcessFigure[] processFigures) {
		final Graph graph = buildGraph(processFigures);
		graph.initLocations();
		graph.optimize();
		applyLayout(processFigures, graph);
		widths.clear();
		heights.clear();
	}

	/**
	 * Edge in the graph
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	class Edge {

		/**
		 * End node of the edge
		 */
		private final Node end;

		/**
		 * Start node of the edge
		 */
		private final Node start;

		/**
		 * Creates a new edge
		 * 
		 * @param start
		 *            The start node of the edge
		 * @param end
		 *            The end node of the edge
		 */
		public Edge(final Node start, final Node end) {
			this.start = start;
			this.end = end;
		}

		/**
		 * Getter of the weight of the edge
		 * 
		 * @return The weight of the edge
		 */
		int getWeight() {
			@SuppressWarnings("deprecation")
			int weight = start.location.getDistanceOrthogonal(end.location);
			if (start.location.x > end.location.x) {
				weight += 2;
			}
			return weight;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj instanceof Edge) {
				if (start.equals(((Edge) obj).start)) {
					if (end.equals(((Edge) obj).end)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	/**
	 * Graph
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	class Graph {

		/**
		 * Coordinate system
		 */
		private final Map<Point, Node> coordinateSystem = new HashMap<>();

		/**
		 * The nodes of the graph
		 */
		private final Map<String, Node> nodes = new HashMap<>();

		/**
		 * Upper end of the coordinate systems x-axis
		 */
		int maximumX = 0;

		/**
		 * Upper end of the coordinate systems y-axis
		 */
		int maximumY = 0;

		/**
		 * Lower end of the coordinate systems x-axis
		 */
		int minimumX = 0;

		/**
		 * Lower end of the coordinate systems y-axis
		 */
		int minimumY = 0;

		/**
		 * Sets the location of a node in the coordinate system
		 * 
		 * @param node
		 *            The node that location should be set
		 * @param xStart
		 *            The x coordinate that is preferred
		 * @param yStart
		 *            The y coordinate that is preferred
		 */
		private void setLocation(final Node node, final int xStart,
				final int yStart) {
			final int x = xStart;
			int y = yStart;
			int addition = 1;
			boolean add = false;
			while (coordinateSystem.get(new Point(x, y)) != null) {
				if (!add) {
					y = yStart - addition;
					add = true;
				} else {
					y = yStart + addition;
					add = false;
					addition++;
				}
			}
			node.location = new Point(x, y);
			checkNewMinimumsAndMaximums(x, y);
			coordinateSystem.put(new Point(x, y), node);
			for (final Edge edge : node.incomingEdges) {
				if (edge.start.location == null) {
					setLocation(edge.start, x - 1, y);
				}
			}
			for (final Edge edge : node.outgoingEdges) {
				if (edge.end.location == null) {
					setLocation(edge.end, x + 1, y);
				}
			}
		}

		/**
		 * Actualizes the maximum and minimum of the coordinate system
		 * 
		 * @param x
		 *            The new x coordinate
		 * @param y
		 *            The new y coordinate
		 */
		void checkNewMinimumsAndMaximums(final int x, final int y) {
			if (x < minimumX) {
				minimumX = x;
			} else if (x > maximumX) {
				maximumX = x;
			}
			if (y < minimumY) {
				minimumY = y;
			} else if (y > maximumY) {
				maximumY = y;
			}
		}

		/**
		 * Initializes all nodes with location (0,0)
		 */
		void initLocations() {
			for (final Node node : nodes.values()) {
				if (node.location == null) {
					setLocation(node, 0, 0);
				}
			}
		}

		/**
		 * Optimizes the graph
		 */
		void optimize() {
			boolean stopOptimizing = false;
			while (!stopOptimizing) {
				stopOptimizing = true;
				// for each node
				for (final Node node : nodes.values()) {
					boolean stop = false;
					while (!stop) {
						final Edge heaviestEdge = node.getHeaviestEdge();
						if (heaviestEdge != null) {
							// the old weight sum of the edge
							final int oldWeightSum = node.getEdgesWeightSum();
							// the old location
							final Point oldLocation = node.location.getCopy();
							// the lightest edge
							final Edge lightestEdge = node.getLightestEdge();
							final boolean nodeIsStart = heaviestEdge.start
									.equals(node);
							final Node n = nodeIsStart ? heaviestEdge.end
									: heaviestEdge.start;
							int x = n.location.x + (nodeIsStart ? 1 : -1);
							int y = n.location.y;
							boolean stopTrying = false;
							boolean canMove = false;
							int divisor = 2;
							while (!stopTrying) {
								// the nearest node
								final Node nearest = lightestEdge.start.key
										.equals(node.key) ? lightestEdge.end
										: lightestEdge.start;
								// the farest node
								final Node farest = heaviestEdge.start.key
										.equals(node.key) ? heaviestEdge.end
										: heaviestEdge.start;
								// if not lightest edge = heaviest edge
								if (!lightestEdge.equals(heaviestEdge)) {
									// reset location
									x = Math.min(nearest.location.x,
											farest.location.x);
									x += Math.abs(nearest.location.x
											- farest.location.x)
											/ divisor;
									y = Math.min(nearest.location.y,
											farest.location.y);
									y += Math.abs(nearest.location.y
											- farest.location.y)
											/ divisor;
								}

								final int tempY = y;
								int addition = 1;
								boolean add = true;
								// find empty location
								while (coordinateSystem.get(new Point(x, y)) != null) {
									if (add) {
										y = tempY + addition;
										add = false;
									} else {
										y = tempY - addition;
										add = true;
										addition++;
									}
								}

								node.location = new Point(x, y);
								final int newWeightSum = node
										.getEdgesWeightSum();
								// if old weight > new weight we found a better
								// location
								if (oldWeightSum > newWeightSum) {
									stopTrying = true;
									canMove = true;
								}
								divisor++;
								if (divisor >= Math.abs(nearest.location.y
										- farest.location.y)) {
									stopTrying = true;
								}
							}
							if (canMove) {
								// move nodes
								coordinateSystem.put(node.location.getCopy(),
										node);
								coordinateSystem.put(oldLocation.getCopy(),
										null);
								checkNewMinimumsAndMaximums(x, y);
								stopOptimizing = false;
							} else {
								// no better spot found, stop optimizing, best
								// location is already set
								node.location = oldLocation.getCopy();
								stop = true;
							}
						} else {
							stop = true;
						}
					}
				}
			}
		}

	}

	/**
	 * Node of a graph
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	class Node {

		/**
		 * Incoming edges
		 */
		private final List<Edge> incomingEdges = new ArrayList<>();

		/**
		 * Identifier of the node
		 */
		private final String key;

		/**
		 * Location in the coordinate system of the graph
		 */
		private Point location;

		/**
		 * Outgoing edges
		 */
		private final List<Edge> outgoingEdges = new ArrayList<>();

		/**
		 * Creates a new node
		 * 
		 * @param key
		 *            The identifier of the node
		 */
		public Node(final String key) {
			this.key = key;
		}

		/**
		 * Get the sum of the weight of the edges of the node
		 * 
		 * @return The sum of the weight of the edges of the node
		 */
		int getEdgesWeightSum() {
			int weightSum = 0;
			for (final Edge edge : incomingEdges) {
				weightSum += edge.getWeight();
			}
			for (final Edge edge : outgoingEdges) {
				weightSum += edge.getWeight();
			}
			return weightSum;
		}

		/**
		 * Searches for the heaviest edge of the node
		 * 
		 * @return The heaviest edge of the node
		 */
		Edge getHeaviestEdge() {
			Edge heaviestEdge = null;
			int heaviestWeight = 0;
			for (final Edge edge : incomingEdges) {
				final int weight = edge.getWeight();
				if (weight > heaviestWeight) {
					heaviestWeight = weight;
					heaviestEdge = edge;
				}
			}
			for (final Edge edge : outgoingEdges) {
				final int weight = edge.getWeight();
				if (weight > heaviestWeight) {
					heaviestWeight = weight;
					heaviestEdge = edge;
				}
			}
			return heaviestEdge;
		}

		/**
		 * Searches for the lightest edge of the node
		 * 
		 * @return The lightest edge of the node
		 */
		Edge getLightestEdge() {
			Edge lightestEdge = null;
			int lightestWeight = Integer.MAX_VALUE;
			for (final Edge edge : incomingEdges) {
				final int weight = edge.getWeight();
				if (weight < lightestWeight) {
					lightestWeight = weight;
					lightestEdge = edge;
				}
			}
			for (final Edge edge : outgoingEdges) {
				final int weight = edge.getWeight();
				if (weight < lightestWeight) {
					lightestWeight = weight;
					lightestEdge = edge;
				}
			}
			return lightestEdge;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj instanceof Node) {
				if (key.equals(((Node) obj).key)) {
					return true;
				}
			}
			return false;
		}

	}

}
