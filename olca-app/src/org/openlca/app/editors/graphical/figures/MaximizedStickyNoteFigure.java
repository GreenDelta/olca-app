package org.openlca.app.editors.graphical.figures;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.text.FlowPage;
import org.eclipse.draw2d.text.ParagraphTextLayout;
import org.eclipse.draw2d.text.TextFlow;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.model.StickyNote;
import org.openlca.app.tools.graphics.figures.GridPos;
import org.openlca.app.tools.graphics.themes.Theme;

public class MaximizedStickyNoteFigure extends StickyNoteFigure {

	public MaximizedStickyNoteFigure(StickyNote note) {
		super(note);
		var theme = note.getGraph().getConfig().getTheme();
		var box = Theme.Box.STICKY_NOTE;

		var layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayoutManager(layout);

		var header = new StickyNoteHeader();
		add(header, new GridData(SWT.FILL, SWT.FILL, true, false));

		var contentPaneLayout = new StackLayout();
		var contentPane = new Figure();
		contentPane.setLayoutManager(contentPaneLayout);

		var flowPage = new FlowPage();
		flowPage.setBorder(new MarginBorder(8));
		var textFlow = new TextFlow(note.content);
		textFlow.setLayoutManager(new ParagraphTextLayout(textFlow,
				ParagraphTextLayout.WORD_WRAP_SOFT));
		flowPage.add(textFlow);
		flowPage.setHorizontalAligment(PositionConstants.LEFT);
		contentPane.add(flowPage, GridPos.leadTop());

		add(contentPane, GridPos.fill());

		setForegroundColor(theme.boxFontColor(box));
		setBackgroundColor(theme.boxBackgroundColor(box));
		setOpaque(true);
	}

}
