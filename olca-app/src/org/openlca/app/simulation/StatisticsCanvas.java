/*******************************************************************************
 * Copyright (c) 2007, 2008, 2009 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.simulation;

import java.util.List;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.swt.widgets.Composite;

/**
 * A canvas for displaying uncertainty statistics.
 */
public class StatisticsCanvas extends FigureCanvas {

	private StatisticFigure plot;

	public StatisticsCanvas(Composite parent) {
		super(parent);
		plot = new StatisticFigure();
		setContents(plot);
	}

	public void setValues(List<Double> values) {
		plot.setData(values);
	}
}
