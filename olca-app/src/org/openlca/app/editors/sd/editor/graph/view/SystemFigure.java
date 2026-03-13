package org.openlca.app.editors.sd.editor.graph.view;

import org.eclipse.draw2d.AbstractBorder;
import org.eclipse.draw2d.BorderLayout;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.swt.graphics.Color;
import org.openlca.app.components.graphics.themes.Theme;
import org.openlca.app.components.graphics.themes.Theme.Box;
import org.openlca.sd.model.SystemBinding;
import org.openlca.sd.model.VarBinding;

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
		setToolTip(new SystemToolTip(binding));
	}

	private static class SystemToolTip extends Figure {

		private SystemToolTip(SystemBinding binding) {
			var layout = new ToolbarLayout();
			layout.setSpacing(2);
			layout.setMinorAlignment(ToolbarLayout.ALIGN_TOPLEFT);
			setLayoutManager(layout);
			setBorder(new MarginBorder(5));

			add(new Label("amount := " + amountOf(binding)));
			if (!binding.varBindings().isEmpty()) {
				add(new Label(""));
			}
			for (var varBinding : binding.varBindings()) {
				if (varBinding == null) continue;
				add(new Label(lineOf(varBinding)));
			}
		}

		private String amountOf(SystemBinding binding) {
			var amountVar = binding.amountVar();
			return amountVar != null
				? amountVar.label()
				: Double.toString(binding.amount());
		}

		private String lineOf(VarBinding binding) {
			var varName = binding.varId() != null
				? binding.varId().label()
				: "";
			var parameter = binding.parameter() != null
				? binding.parameter()
				: "";
			return varName + " -> " + parameter;
		}
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
