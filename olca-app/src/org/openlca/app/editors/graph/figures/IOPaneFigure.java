package org.openlca.app.editors.graph.figures;

import java.util.List;

import org.eclipse.draw2d.*;
import org.eclipse.swt.SWT;
import org.openlca.app.util.Colors;

public class IOPaneFigure extends Figure {

	private final ScrollPane scrollpane;

	public IOPaneFigure() {
		var layout = new GridLayout(1, false);
		setLayoutManager(layout);

		add(new Header(true), new GridData(SWT.LEAD, SWT.TOP, false, false));

		scrollpane = new PuristicScrollPane();
		var contentPane = new Figure();
		contentPane.setLayoutManager(new GridLayout(1, false));
		add(scrollpane);
		scrollpane.setContents(contentPane);

		setToolTip(new Label("IOPane"));
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

	private static class Header extends Figure {

		Header(boolean forInputs) {
			var layout = new GridLayout(1, true);
			layout.marginHeight = 3;
			layout.marginWidth = 5;
			setLayoutManager(layout);
			Label label = new Label(forInputs ? ">> input flows" : "output flows >>");
			label.setForegroundColor(ColorConstants.green);
			var alignment = forInputs ? SWT.LEFT : SWT.RIGHT;
			add(label, new GridData(alignment, SWT.TOP, true, false));
		}

	}

}
