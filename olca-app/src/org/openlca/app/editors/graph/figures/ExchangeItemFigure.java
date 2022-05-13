package org.openlca.app.editors.graph.figures;

import org.eclipse.draw2d.*;
import org.eclipse.swt.SWT;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;

public class ExchangeItemFigure extends Figure {

	public ExchangeItemFigure(Exchange exchange) {
		var layout = new GridLayout(4, false);
		setLayoutManager(layout);

		var image = new ImageFigure(Images.get(exchange.flow));
		add(image, new GridData(SWT.LEAD, SWT.TOP, false, false));

		var name = new Label(Labels.name(exchange.flow));
		name.setForegroundColor(ColorConstants.black);
		add(name, new GridData(SWT.FILL, SWT.TOP, true, false));
		var amount = new Label(Numbers.format(exchange.amount));
		amount.setForegroundColor(ColorConstants.black);
		add(amount, new GridData(SWT.TRAIL, SWT.TOP, true, false));
		var unit = new Label(Labels.name(exchange.unit));
		unit.setForegroundColor(ColorConstants.black);
		add(unit, new GridData(SWT.LEAD, SWT.TOP, true, false));
	}

}
