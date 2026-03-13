package org.openlca.app.editors.sd.editor.graph.view;

import org.eclipse.draw2d.AbstractBorder;
import org.eclipse.draw2d.BorderLayout;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.swt.graphics.Color;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.components.graphics.themes.Theme.Box;
import org.openlca.sd.model.SystemBinding;

public class SystemFigure extends Figure {

	private final Label label = new Label();

	public SystemFigure(Theme theme) {
		setLayoutManager(new BorderLayout());
		setBorder(new DoubleLineBorder(theme.boxBorderColor(Box.SUB_SYSTEM)));
		setBackgroundColor(theme.boxBackgroundColor(Box.DEFAULT));
		label.setForegroundColor(theme.boxFontColor(Box.DEFAULT));
		label.setTextAlignment(PositionConstants.CENTER);
		add(label, BorderLayout.CENTER);
		setOpaque(true);
	}

	public void setBinding(SystemBinding binding) {
		if (binding == null || binding.system() == null) {
			label.setText("");
			setToolTip(null);
			return;
		}

		label.setText(binding.system().name());
		setToolTip(new Label(binding.toString()));
	}

	private static class DoubleLineBorder extends AbstractBorder {

		private final Color color;

		private DoubleLineBorder(Color color) {
			this.color = color;
		}

		@Override
		public Insets getInsets(IFigure figure) {
			return new Insets(4);
		}

		@Override
		public void paint(IFigure figure, Graphics graphics, Insets insets) {
			graphics.setForegroundColor(color);
			var outer = getPaintRectangle(figure, insets).getCopy();
			outer.width -= 1;
			outer.height -= 1;
			graphics.drawRectangle(outer);

			var inner = outer.getCopy();
			inner.shrink(3, 3);
			if (inner.width > 0 && inner.height > 0) {
				graphics.drawRectangle(inner);
			}
		}

		@Override
		public boolean isOpaque() {
			return true;
		}
	}
}
