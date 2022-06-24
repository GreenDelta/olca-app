package org.openlca.app.editors.graphical.figures;

import java.util.List;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.IOPane;
import org.openlca.app.editors.graphical.themes.Theme;

import static org.openlca.app.editors.graphical.figures.ExchangeFigure.getPreferredAmountLabelSize;
import static org.openlca.app.editors.graphical.model.GraphComponent.INPUT_PROP;

public class IOPaneFigure extends Figure {

	private final Figure contentPane = new Figure();
	private final IOPane pane;
	public final AddExchangeButton addExchangeButton = new AddExchangeButton();
	private Dimension amountLabelSize;
	private Dimension unitLabelSize;

	public IOPaneFigure(IOPane pane) {
		this.pane = pane;
		var theme = pane.getConfig().getTheme();
		var box = Theme.Box.of(pane.getNode());

		var layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		setLayoutManager(layout);

		if (!pane.getExchangesItems().isEmpty() || pane.getNode().isEditable()) {

			add(new Header(), GridPos.fillTop());

			var contentPaneLayout = new GridLayout(1, false);
			contentPaneLayout.marginHeight = 0;
			contentPaneLayout.marginWidth = 0;
			contentPane.setLayoutManager(contentPaneLayout);
			add(contentPane, GridPos.fill());

			if (pane.getNode().isEditable()) {
				var alignment = pane.isForInputs() ? SWT.LEAD : SWT.TRAIL;
				add(addExchangeButton, new GridData(alignment, SWT.BOTTOM, false, false));
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

	public Dimension getAmountLabelSize() {
		if (amountLabelSize == null) {
			for (ExchangeItem item : pane.getExchangesItems()) {
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

		var newSize = new Dimension();
		if (isRemoval) {
			newSize.width = Math.min(amountLabelSize.width, size.width);
			newSize.height = Math.min(amountLabelSize.height, size.height);
		} else {
			newSize.width = Math.max(amountLabelSize.width, size.width);
			newSize.height = Math.max(amountLabelSize.height, size.height);
		}
		amountLabelSize = newSize;
	}

	public Dimension getUnitLabelSize() {
		if (unitLabelSize == null) {
			for (ExchangeItem item : pane.getExchangesItems()) {
				var preferredSize = getPreferredAmountLabelSize(item);
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

		var newSize = new Dimension();
		if (isRemoval) {
			newSize.width = Math.min(unitLabelSize.width, size.width);
			newSize.height = Math.min(unitLabelSize.height, size.height);
		} else {
			newSize.width = Math.max(unitLabelSize.width, size.width);
			newSize.height = Math.max(unitLabelSize.height, size.height);
		}
		unitLabelSize = newSize;
	}

	private class Header extends Figure {

		Header() {
			var layout = new GridLayout(1, false);
			layout.marginHeight = 4;
			layout.marginWidth = 2;
			setLayoutManager(layout);

			var theme = pane.getConfig().getTheme();
			Label label = new Label(
				pane.isForInputs() ? ">> input flows" : "output flows >>");
			label.setForegroundColor(theme.infoLabelColor());
			var alignment = pane.isForInputs() ? SWT.LEAD : SWT.TRAIL;
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
					g.drawLine(location.x, location.y,
						location.x + size.width, location.y);
					g.restoreState();
				}
			}
			super.paint(g);
		}

	}

}
