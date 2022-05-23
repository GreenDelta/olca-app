package org.openlca.app.editors.graph.figures;

import java.util.List;

import org.eclipse.draw2d.*;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graph.model.IOPane;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.editors.graph.themes.Theme;

import static org.openlca.app.editors.graph.model.GraphComponent.INPUT_PROP;

public class IOPaneFigure extends Figure {

	private final Figure contentPane = new Figure();
	private final IOPane pane;

	public IOPaneFigure(IOPane pane) {
		this.pane = pane;
		var theme = pane.getConfig().getTheme();
		var box = Theme.Box.of(pane.getNode());

		var layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		setLayoutManager(layout);

		if (!pane.getExchangesItems().isEmpty() || pane.getNode().isEditable()) {

			add(new Header(), GridPos.fillTop());

			var contentPaneLayout = new GridLayout(1, false);
			contentPaneLayout.marginHeight = 0;
			contentPaneLayout.marginWidth = 0;
			contentPane.setLayoutManager(contentPaneLayout);
			add(contentPane, GridPos.fill());

			if (pane.getNode().isEditable()) {
				add(new ButtonMargin(pane.isForInputs(), pane.getNode()), GridPos.fillTop());
			}

			setToolTip(new Label(pane.isForInputs() ? "Input flows" : "Output flows"));
			setForegroundColor(theme.boxBackgroundColor(box));
			setOpaque(true);
		}
	}

	public IFigure getContentPane() {
		return contentPane;
	}

	public MaximizedNodeFigure getNodeFigure() {
		// The parent of an IOPaneFigure is MaximizedNodeFigure.contentPane.
		return (MaximizedNodeFigure) super.getParent().getParent();
	}

	@Override
	public void paint(Graphics g) {
		getParent().getLayoutManager().setConstraint(this, GridPos.fillTop());
		super.paint(g);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ExchangeFigure> getChildren() {
		return super.getChildren();
	}

	private class Header extends Figure {

		Header() {
			var layout = new GridLayout(1, false);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			setLayoutManager(layout);

			var theme = pane.getConfig().getTheme();
			Label label = new Label(pane.isForInputs() ? ">> input flows" : "output flows >>");
			label.setForegroundColor(theme.infoLabelColor());
			var alignment = pane.isForInputs() ? SWT.LEFT : SWT.RIGHT;
			add(label, new GridData(alignment, SWT.TOP, true, false));
		}

		@Override
		public void paint(Graphics g) {
			if (!pane.isForInputs()) {
				var inputIOPane = pane.getNode().getIOPanes().get(INPUT_PROP);
				if (!inputIOPane.getExchangesItems().isEmpty()) {
					var theme = pane.getConfig().getTheme();
					var box = Theme.Box.of(pane.getNode());
					var location = getLocation();
					var size = getNodeFigure().getSize();
					g.setForegroundColor(theme.boxBorderColor(box));
					g.drawLine(location.x, location.y, location.x + size.width, location.y);
					g.restoreState();
				}
			}
			super.paint(g);
		}

	}

	private static class ButtonMargin extends Figure {

		public ButtonMargin(boolean forInputs, Node node) {
			var button = new AddFlowButton(forInputs, node);
			var layout = new GridLayout(1, true);
			layout.marginHeight = 3;
			layout.marginWidth = 5;
			setLayoutManager(layout);
			var alignment = forInputs ? SWT.LEFT : SWT.RIGHT;
			add(button, new GridData(alignment, SWT.TOP, true, false));
		}

	}

}
