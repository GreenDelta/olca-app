package org.openlca.app.editors.graph.figures;

import java.util.List;

import org.eclipse.draw2d.*;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graph.model.IOPane;
import org.openlca.app.editors.graph.themes.Theme;
import org.openlca.app.util.Colors;

public class IOPaneFigure extends Figure {

	private final ScrollPane scrollpane;
	private final IOPane pane;

	public IOPaneFigure(IOPane pane) {
		this.pane = pane;
		var layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayoutManager(layout);

		add(new Header(pane.getIsInput()), GridPos.fillTop());

		scrollpane = new PuristicScrollPane();
		var contentPane = new Figure();
		contentPane.setLayoutManager(new GridLayout(1, false));
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		add(scrollpane, GridPos.fill());
		scrollpane.setContents(contentPane);


		setToolTip(new Label(pane.getIsInput() ? "Input flows" : "Output flows"));
		setForegroundColor(Colors.white());
		setOpaque(true);
	}

	public IFigure getContentPane() {
		return scrollpane.getContents();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ExchangeFigure> getChildren() {
		return super.getChildren();
	}

	private class Header extends Figure {

		Header(boolean forInputs) {
			var layout = new GridLayout(1, true);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			setLayoutManager(layout);

			var theme = pane.getConfig().getTheme();
			Label label = new Label(forInputs ? ">> input flows" : "output flows >>");
			label.setForegroundColor(theme.infoLabelColor());
			var alignment = forInputs ? SWT.LEFT : SWT.RIGHT;
			add(label, new GridData(alignment, SWT.TOP, true, false));
		}

		@Override
		public void paint(Graphics g) {
			var theme = pane.getConfig().getTheme();
			var location = getLocation();
			var size = getSize();
			g.setForegroundColor(theme.boxBorderColor(Theme.Box.of(pane.getNode())));
			g.drawLine(location.x, location.y, location.x + size.width, location.y);
			g.restoreState();
			super.paint(g);
		}

	}

}
