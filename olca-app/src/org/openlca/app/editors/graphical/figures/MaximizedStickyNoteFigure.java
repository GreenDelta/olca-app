package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.text.FlowPage;
import org.eclipse.draw2d.text.TextFlow;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.model.StickyNote;
import org.openlca.app.tools.graphics.figures.GridPos;
import org.openlca.app.tools.graphics.figures.RoundBorder;
import org.openlca.app.tools.graphics.themes.Theme;

public class MaximizedStickyNoteFigure extends StickyNoteFigure {

	public MaximizedStickyNoteFigure(StickyNote note) {
		super(note);
		var theme = note.getGraph().getConfig().getTheme();
		var box = Theme.Box.STICKY_NOTE;

		var layout = new GridLayout(1, false);
		layout.marginHeight = 2;
		layout.marginWidth = 2;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		setLayoutManager(layout);

		var borderWidth = theme.boxBorderWidth(box);
		// TODO (francois): should be defined in CSS (with probably box.top).
		var topBorderWidth = theme.boxBorderWidth(box);

		var arcDifference = new Dimension(
			2 * layout.marginWidth + (borderWidth + topBorderWidth) / 2,
			2 * layout.marginHeight + (borderWidth + topBorderWidth) / 2);

		var roundedCorners = RoundBorder.Corners
			.fullRoundedCorners(HEADER_ARC_SIZE.getExpanded(arcDifference));
		var border = new RoundBorder(borderWidth, roundedCorners);
		border.setColor(theme.boxBorderColor(box));
		setBorder(border);

		var header = new StickyNoteHeader();
		var topRoundedCorners = RoundBorder.Corners
			.topRoundedCorners(HEADER_ARC_SIZE);
		var headerBorder = new RoundBorder(borderWidth, topRoundedCorners);
		headerBorder.setColor(theme.boxBorderColor(box));
		header.setBorder(headerBorder);
		add(header, new GridData(SWT.FILL, SWT.FILL, true, false));

		var contentPaneLayout = new ToolbarLayout();
		contentPaneLayout.setSpacing(5);
		var contentPane = new Figure();
		contentPane.setLayoutManager(contentPaneLayout);
		var flowPage = new FlowPage();
		flowPage.add(new TextFlow(note.content));
		flowPage.setHorizontalAligment(PositionConstants.LEFT);
		contentPane.add(flowPage, GridPos.leadTop());
		add(contentPane, GridPos.fill());

		setForegroundColor(theme.boxFontColor(box));
		setBackgroundColor(theme.boxBackgroundColor(box));
		setOpaque(true);
	}

}
