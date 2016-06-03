package org.openlca.app.results.analysis.sankey.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.draw2d.geometry.Rectangle;
import org.openlca.app.util.Labels;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ProcessNode extends Node {

	public static String CONNECTION = "Connection";

	public final ProcessDescriptor process;
	public ProcessFigure figure;
	public final List<ConnectionLink> links = new ArrayList<>();
	public double upstreamResult;
	public double upstreamContribution;
	public double directResult;
	public double directContribution;

	private Rectangle xyLayoutConstraints;

	public ProcessNode(ProcessDescriptor process) {
		this.process = process;
	}

	public void add(ConnectionLink link) {
		links.add(link);
		listeners.firePropertyChange(CONNECTION, null, link);
	}

	public List<ConnectionLink> getIncomingLinks() {
		List<ConnectionLink> incomingPositive = new ArrayList<>();
		List<ConnectionLink> incomingNegative = new ArrayList<>();
		for (ConnectionLink link : links) {
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

		List<ConnectionLink> incoming = new ArrayList<>();
		incoming.addAll(incomingPositive);
		incoming.addAll(incomingNegative);
		return incoming;
	}

	@Override
	public String getName() {
		return Labels.getDisplayName(process);
	}

	public List<ConnectionLink> getOutgoingLinks() {
		List<ConnectionLink> outgoingPositive = new ArrayList<>();
		List<ConnectionLink> outgoingNegative = new ArrayList<>();
		for (ConnectionLink link : links) {
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

		List<ConnectionLink> outgoing = new ArrayList<>();
		outgoing.addAll(outgoingPositive);
		outgoing.addAll(outgoingNegative);
		return outgoing;
	}

	public Rectangle getXyLayoutConstraints() {
		return xyLayoutConstraints;
	}

	public void setXyLayoutConstraints(Rectangle xyLayoutConstraints) {
		this.xyLayoutConstraints = xyLayoutConstraints;
		listeners.firePropertyChange(Node.PROPERTY_LAYOUT, null, "not null");
	}

	private class LinkComparator implements Comparator<ConnectionLink> {

		/**
		 * Indicates if the links compared are source connections or target
		 * connections
		 */
		private boolean source;

		private LinkComparator(boolean source) {
			this.source = source;
		}

		@Override
		public int compare(ConnectionLink o1, ConnectionLink o2) {
			int result = 0;
			if (source) {
				result = Double.compare(o1.getSourceNode().figure
						.getLocation().x, o2.getSourceNode().figure
								.getLocation().x);
			} else {
				result = Double.compare(o1.getTargetNode().figure
						.getLocation().x, o2.getTargetNode().figure
								.getLocation().x);
			}
			if (result == 0) {
				if (source) {
					result = Double.compare(o1.getSourceNode().figure
							.getLocation().y, o2.getSourceNode().figure
									.getLocation().y);
				} else {
					result = Double.compare(o1.getTargetNode().figure
							.getLocation().y, o2.getTargetNode().figure
									.getLocation().y);
				}
			}
			return result;
		}
	}
}
