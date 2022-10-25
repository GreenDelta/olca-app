package org.openlca.app.results.analysis.sankey.model;

import org.openlca.app.tools.graphics.model.Component;
import org.openlca.app.tools.graphics.model.Link;

import java.util.Comparator;
import java.util.List;

import static org.eclipse.draw2d.PositionConstants.EAST;
import static org.eclipse.draw2d.PositionConstants.WEST;


public class SankeyLink extends Link {

	public static final int MAXIMUM_WIDTH = 70;

	public final double ratio;
	private int sourceAnchor;
	private int targetAnchor;

	public SankeyLink(SankeyNode source, SankeyNode target, double ratio) {
		this.ratio = ratio;
		reconnect(source, target);
	}

	public int getLineWidth() {
		int width = (int) Math.ceil(Math.abs(ratio * MAXIMUM_WIDTH));
		return width == 0 ? 1 : Math.min(width, MAXIMUM_WIDTH);
	}

	public void setSourceAnchor() {
		var links = source.getAllSourceConnections();
		links.sort(Comparator.comparing(
				link -> link.getTarget().getComparisonLabel()
		));

		var sankeyLinks = links.stream()
				.filter(link -> link instanceof SankeyLink)
				.map(link -> (SankeyLink) link)
				.toList();

		var sum = sankeyLinks.stream()
				.takeWhile(link -> !link.getTarget().equals(target))
				.map(SankeyLink::getLineWidth)
				.reduce(Integer::sum)
				.orElse(0);

		sourceAnchor = anchorOf(sankeyLinks, source, sum);
	}

	public void setTargetAnchor() {
		var links = target.getAllTargetConnections();

		links.sort(Comparator.comparing(Link::getSource));

		var sankeyLinks = links.stream()
				.filter(link -> link instanceof SankeyLink)
				.map(link -> (SankeyLink) link)
				.toList();

		var sum = sankeyLinks.stream()
				.takeWhile(link -> !link.getSource().equals(source))
				.map(SankeyLink::getLineWidth)
				.reduce(Integer::sum)
				.orElse(0);

		targetAnchor = anchorOf(sankeyLinks, target, sum);
	}

	private int anchorOf(List<SankeyLink> links, Component component, int sum) {
		var orientation = getSourceNode().getDiagram().orientation;
		var length = ((orientation & (EAST | WEST)) != 0)
				? component.getSize().height()
				: component.getSize().width();
		int totalSum = links.stream()
				.map(SankeyLink::getLineWidth)
				.reduce(Integer::sum)
				.orElse(0);

		if (length < totalSum)
			return 0;
		else {
			var space = (length - totalSum) / (links.size() + 1);
			return sum + space * (links.indexOf(this) + 1) + getLineWidth() / 2;
		}
	}

	public int getSourceAnchor() {
		if (sourceAnchor == 0)
			setSourceAnchor();
		return sourceAnchor;
	}

	public int getTargetAnchor() {
		if (targetAnchor == 0)
			setTargetAnchor();
		return targetAnchor;
	}

	@Override
	public SankeyNode getSourceNode() {
		return (SankeyNode) source;
	}

	@Override
	public SankeyNode getTargetNode() {
		return (SankeyNode) target;
	}

}
