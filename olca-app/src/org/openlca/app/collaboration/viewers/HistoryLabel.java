package org.openlca.app.collaboration.viewers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.openlca.app.collaboration.util.Format;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.git.model.Commit;

class HistoryLabel extends OwnerDrawLabelProvider {

	private final int column;
	private final HistoryViewer viewer;
	
	HistoryLabel(HistoryViewer viewer, int column) {
		this.column = column;
		this.viewer = viewer;
	}

	@Override
	protected void measure(Event event, Object element) {
		var commit = (Commit) element;
		var image = getImage(commit);
		var width = image != null ? image.getBounds().width : 6;
		for (var badge : getBadges(commit)) {
			var font = event.gc.getFont();
			if (badge.font != null) {
				event.gc.setFont(badge.font);
			}
			width += event.gc.textExtent(badge.text).x + 12;
			event.gc.setFont(font);
		}
		var text = getText(commit);
		if (text != null) {
			width += event.gc.textExtent(text).x;
		}
		event.setBounds(new Rectangle(event.x, event.y, width, event.getBounds().height));
	}

	@Override
	protected void paint(Event event, Object element) {
		var commit = (Commit) element;
		var bounds = event.getBounds();
		var image = getImage(commit);
		var x = 6;
		event.gc.setAntialias(SWT.ON);
		if (image != null) {
			var iBounds = image.getBounds();
			var hFactor = (double) bounds.height / (double) iBounds.height;
			var width = (int) Math.floor(image.getBounds().width * hFactor);
			event.gc.drawImage(image, iBounds.x, iBounds.y, iBounds.width, iBounds.height, bounds.x, bounds.y, width, bounds.height);
			x = width;
		}
		for (var badge : getBadges(commit)) {
			var font = event.gc.getFont();
			if (badge.font != null) {
				event.gc.setFont(badge.font);
			}
			var width = event.gc.textExtent(badge.text).x;
			var bg = event.gc.getBackground();
			var fg = event.gc.getForeground();
			event.gc.setForeground(badge.border);
			event.gc.drawRoundRectangle(bounds.x + x, bounds.y, width + 9, bounds.height - 2, 8, 8);
			event.gc.setForeground(fg);
			event.gc.setBackground(badge.background);
			event.gc.fillRoundRectangle(bounds.x + x + 2, bounds.y + 2, width + 6, bounds.height - 5, 6, 6);
			event.gc.setBackground(bg);
			event.gc.drawText(badge.text, bounds.x + x + 5, bounds.y + 2, true);
			event.gc.setFont(font);
			x += width + 12;
		}
		var text = getText(commit);
		if (text != null) {
			event.gc.drawText(text, bounds.x + x, bounds.y + 2, true);
		}
	}

	protected void erase(Event event, Object element) {
	}

	private Image getImage(Commit commit) {
		if (column != 1)
			return null;
		if (commit == null)
			return null;
		var index = viewer.commits.indexOf(commit);
		return viewer.images.get(index);
	}

	private List<HistoryLabel.Badge> getBadges(Commit commit) {
		if (column != 1)
			return new ArrayList<>();
		var badges = new ArrayList<HistoryLabel.Badge>();
		if (commit.id.equals(viewer.localCommitId)) {
			badges.add(new HistoryLabel.Badge("Local", Colors.get(188, 220, 188), Colors.get(0, 128, 0), UI.boldFont()));
		}
		if (commit.id.equals(viewer.remoteCommitId)) {
			badges.add(new HistoryLabel.Badge("Remote", Colors.get(225, 225, 225), Colors.get(80, 80, 80), null));
		}
		return badges;
	}

	private String getText(Commit commit) {
		switch (column) {
			case 0:
				return commit.id;
			case 1:
				return commit.message;
			case 2:
				return commit.user;
			case 3:
				return Format.commitDate(commit.timestamp);
		}
		return null;
	}

	private record Badge(String text, Color background, Color border, Font font) {
	}

}