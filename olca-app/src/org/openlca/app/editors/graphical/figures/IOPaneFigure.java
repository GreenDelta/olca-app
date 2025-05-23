package org.openlca.app.editors.graphical.figures;

import static org.openlca.app.editors.graphical.figures.ExchangeFigure.getPreferredAmountLabelSize;
import static org.openlca.app.editors.graphical.figures.ExchangeFigure.getPreferredUnitLabelSize;
import static org.openlca.app.editors.graphical.model.Node.INPUT_PROP;

import java.util.List;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.IOPane;
import org.openlca.app.components.graphics.figures.ComponentFigure;
import org.openlca.app.components.graphics.figures.GridPos;

public class IOPaneFigure extends ComponentFigure {

	private final Figure contentPane = new Figure();
	private final IOPane pane;
	public final AddExchangeButton addExchangeButton = new AddExchangeButton();
	private Dimension amountLabelSize;
	private Dimension unitLabelSize;

	public IOPaneFigure(IOPane pane) {
		super(pane);
		this.pane = pane;
		var theme = pane.getGraph().getEditor().getTheme();
		var node = pane.getNode();
		var box = node.getThemeBox();

		var layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		setLayoutManager(layout);

		if (!pane.getExchangeItems().isEmpty() || node.isEditable()) {

			add(new Header(), GridPos.fillTop());

			var contentPaneLayout = new GridLayout(1, false);
			contentPaneLayout.marginHeight = 0;
			contentPaneLayout.marginWidth = 0;
			contentPane.setLayoutManager(contentPaneLayout);
			add(contentPane, GridPos.fill());

			if (node.isEditable()) {
				var alignment = pane.isForInputs() ? SWT.LEAD : SWT.TRAIL;
				add(addExchangeButton, new GridData(alignment, SWT.BOTTOM, false, false));
			}

			setToolTip(new Label(pane.isForInputs() ? M.InputFlows : M.OutputFlows));
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
		return (List<ExchangeFigure>) super.getChildren();
	}

	public Dimension getAmountLabelSize() {
		if (amountLabelSize == null) {
			for (ExchangeItem item : pane.getExchangeItems()) {
				var preferredSize = getPreferredAmountLabelSize(item);
				setAmountLabelSize(preferredSize, false);
			}
		}
		return amountLabelSize;
	}

	public void setAmountLabelSize(Dimension size, boolean isRemoval) {
		if (amountLabelSize == null) {
			amountLabelSize = size;
			return;
		}

		if (isRemoval) {
			amountLabelSize = null;
		} else {
			var newSize = new Dimension();
			newSize.width = Math.max(amountLabelSize.width, size.width);
			newSize.height = Math.max(amountLabelSize.height, size.height);
			amountLabelSize = newSize;
		}
	}

	public Dimension getUnitLabelSize() {
		if (unitLabelSize == null) {
			for (ExchangeItem item : pane.getExchangeItems()) {
				var preferredSize = getPreferredUnitLabelSize(item);
				setUnitLabelSize(preferredSize, false);
			}
		}
		return unitLabelSize;
	}

	public void setUnitLabelSize(Dimension size, boolean isRemoval) {
		if (unitLabelSize == null) {
			unitLabelSize = size;
			return;
		}

		if (isRemoval) {
			// Resetting the variable such that it is computed again when getting it.
			unitLabelSize = null;
		} else {
			var newSize = new Dimension();
			newSize.width = Math.max(unitLabelSize.width, size.width);
			newSize.height = Math.max(unitLabelSize.height, size.height);
			unitLabelSize = newSize;
		}
	}

	private class Header extends Figure {

		Header() {
			var layout = new GridLayout(1, false);
			layout.marginHeight = 4;
			layout.marginWidth = 2;
			setLayoutManager(layout);

			var theme = pane.getGraph().getEditor().getTheme();
			Label label = new Label(
				pane.isForInputs() ? "⇨ " + M.InputFlows : M.OutputFlows + " ⇨");
			label.setForegroundColor(theme.infoLabelColor());
			var alignment = pane.isForInputs() ? SWT.LEAD : SWT.TRAIL;
			add(label, new GridData(alignment, SWT.TOP, true, false));
		}

		@Override
		public void paint(Graphics g) {
			if (!pane.isForInputs()) {
				var inputIOPane = pane.getNode().getIOPanes().get(INPUT_PROP);
				if (!inputIOPane.getExchangeItems().isEmpty()) {
					var theme = pane.getGraph().getEditor().getTheme();
					var node = pane.getNode();
					var box = node.getThemeBox();
					var location = getLocation();
					var size = getNodeFigure().getSize();
					g.setForegroundColor(theme.boxBorderColor(box));
					g.drawLine(location.x, location.y,
						location.x + size.width, location.y);
					g.restoreState();
				}
			}
			super.paint(g);
		}

	}

}
