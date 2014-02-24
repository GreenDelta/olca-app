/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.results.analysis.sankey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.util.Labels;
import org.openlca.core.model.descriptors.ProcessDescriptor;

/**
 * Internal model representing a process in the graphical viewer
 * 
 * @author Sebastian Greve
 * 
 */
public class ProcessNode extends Node {

	/** String for the PropertyChangeEvent 'CONNECTION' */
	public static String CONNECTION = "Connection";

	private ProcessFigure figure;

	private List<ConnectionLink> links = new ArrayList<>();
	private ProcessDescriptor process;

	private double totalResult;
	private double singleResult;
	private double singleContribution;
	private double totalContribution;

	private Rectangle xyLayoutConstraints;

	public ProcessNode(ProcessDescriptor process) {
		this.process = process;
	}

	double getTotalResult() {
		return totalResult;
	}

	void setTotalResult(double totalResult) {
		this.totalResult = totalResult;
	}

	double getSingleResult() {
		return singleResult;
	}

	void setSingleResult(double singleResult) {
		this.singleResult = singleResult;
	}

	double getSingleContribution() {
		return singleContribution;
	}

	void setSingleContribution(double singleContribution) {
		this.singleContribution = singleContribution;
	}

	double getTotalContribution() {
		return totalContribution;
	}

	void setTotalContribution(double totalContribution) {
		this.totalContribution = totalContribution;
	}

	@Override
	public void dispose() {
	}

	public void add(ConnectionLink connectionLink) {
		links.add(connectionLink);
		listeners.firePropertyChange(CONNECTION, null, connectionLink);
	}

	public ProcessFigure getFigure() {
		return figure;
	}

	/**
	 * Getter of all incoming links
	 * 
	 * @return The incoming links
	 */
	public List<ConnectionLink> getIncomingLinks() {
		final List<ConnectionLink> incomingPositive = new ArrayList<>();
		final List<ConnectionLink> incomingNegative = new ArrayList<>();
		for (final ConnectionLink link : links) {
			if (link.getTargetNode() == this) {
				if (link.getRatio() >= 0) {
					incomingPositive.add(link);
				} else {
					incomingNegative.add(link);
				}
			}
		}

		Collections.sort(incomingPositive, new LinkComparator(true));
		Collections.sort(incomingNegative, new LinkComparator(true));

		final List<ConnectionLink> incoming = new ArrayList<>();
		incoming.addAll(incomingPositive);
		incoming.addAll(incomingNegative);
		return incoming;
	}

	/**
	 * Getter of {@link #links}
	 * 
	 * @return links
	 */
	public List<ConnectionLink> getLinks() {
		return links;
	}

	@Override
	public String getName() {
		return Labels.getDisplayName(process);
	}

	/**
	 * Getter of all outgoing links
	 * 
	 * @return The outgoing links
	 */
	public List<ConnectionLink> getOutgoingLinks() {
		final List<ConnectionLink> outgoingPositive = new ArrayList<>();
		final List<ConnectionLink> outgoingNegative = new ArrayList<>();
		for (final ConnectionLink link : links) {
			if (link.getSourceNode() == this) {
				if (link.getRatio() >= 0) {
					outgoingPositive.add(link);
				} else {
					outgoingNegative.add(link);
				}
			}
		}
		Collections.sort(outgoingPositive, new LinkComparator(false));
		Collections.sort(outgoingNegative, new LinkComparator(false));

		final List<ConnectionLink> outgoing = new ArrayList<>();
		outgoing.addAll(outgoingPositive);
		outgoing.addAll(outgoingNegative);
		return outgoing;
	}

	public ProcessDescriptor getProcess() {
		return process;
	}

	public Rectangle getXyLayoutConstraints() {
		return xyLayoutConstraints;
	}

	public void setFigure(ProcessFigure figure) {
		this.figure = figure;
	}

	public void setXyLayoutConstraints(Rectangle xyLayoutConstraints) {
		this.xyLayoutConstraints = xyLayoutConstraints;
		listeners.firePropertyChange(Node.PROPERTY_LAYOUT, null, "not null");
	}

	/**
	 * Compares to connection links (based on their nodes location)
	 * 
	 * @author Sebastian Greve
	 */
	private class LinkComparator implements Comparator<ConnectionLink> {

		/**
		 * Indicates if the links compared are source connections or target
		 * connections
		 */
		private final boolean source;

		/**
		 * Creates a new instance
		 * 
		 * @param source
		 *            Indicates if the links compared are source connections or
		 *            target connections
		 */
		private LinkComparator(final boolean source) {
			this.source = source;
		}

		@Override
		public int compare(final ConnectionLink o1, final ConnectionLink o2) {
			int result = 0;
			if (source) {
				result = Double.compare(o1.getSourceNode().getFigure()
						.getLocation().x, o2.getSourceNode().getFigure()
						.getLocation().x);
			} else {
				result = Double.compare(o1.getTargetNode().getFigure()
						.getLocation().x, o2.getTargetNode().getFigure()
						.getLocation().x);
			}
			if (result == 0) {
				if (source) {
					result = Double.compare(o1.getSourceNode().getFigure()
							.getLocation().y, o2.getSourceNode().getFigure()
							.getLocation().y);
				} else {
					result = Double.compare(o1.getTargetNode().getFigure()
							.getLocation().y, o2.getTargetNode().getFigure()
							.getLocation().y);
				}
			}
			return result;
		}
	}
}
