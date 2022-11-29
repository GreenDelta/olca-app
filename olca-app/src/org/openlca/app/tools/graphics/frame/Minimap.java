package org.openlca.app.tools.graphics.frame;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.gef.LayerConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.openlca.app.tools.graphics.BasicGraphicalEditor;
import org.openlca.app.tools.graphics.edit.RootEditPart;
import org.openlca.app.util.Controls;

import java.util.Arrays;
import java.util.Comparator;

public class Minimap extends Composite {

	protected static Dimension DEFAULT_SIZE = new Dimension(250, 250);

	private RootEditPart rootEditPart;
	private ScrollableThumbnail thumbnail;
	private DisposeListener disposeListener;

	public Minimap(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, true));
	}

	protected void initialize() {
		createZoomScale();
		createCanvas();
	}

	public void setRootEditPart(RootEditPart part) {
		rootEditPart = part;
	}

	protected void createZoomScale() {
		var zoomScale = new Composite(this, SWT.TRANSPARENT);
		zoomScale.setLayout(new GridLayout(2, false));
		zoomScale.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		var scale = new Scale(zoomScale, SWT.TRANSPARENT);
		scale.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		final double[] values = BasicGraphicalEditor.ZOOM_LEVELS;
		scale.setIncrement(1);
		scale.setMinimum(0);
		scale.setMaximum(values.length - 1);


		var zoomManager = rootEditPart.getZoomManager();
		Controls.onSelect(scale,
				(e) -> zoomManager.setZoom(values[scale.getSelection()], false));
		scale.setSelection(getIndex(values, zoomManager.getZoom()));

		var label = new Label(zoomScale, SWT.TRANSPARENT);
		label.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		int percentage = (int) (100 * zoomManager.getZoom());
		label.setText(percentage + "% ");

		zoomManager.addZoomListener(zoom -> {
			int newPercentage = (int) (100 * zoom);
			label.setText(newPercentage + "%");
			scale.setSelection(getIndex(values, zoom));
		});
	}

	protected void createCanvas() {
		var canvas = new Canvas(this, SWT.TRANSPARENT);
		canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		var lws = new LightweightSystem(canvas);
		thumbnail = new ScrollableThumbnail((Viewport) rootEditPart.getFigure());
		thumbnail.setSource(rootEditPart.getLayer(LayerConstants.PRINTABLE_LAYERS));
		lws.setContents(thumbnail);

		var viewer = rootEditPart.getViewer();
		disposeListener = e -> deactivate();
		viewer.getControl().addDisposeListener(disposeListener);
	}

	private void deactivate() {
		if (thumbnail != null) {
			thumbnail.deactivate();
			thumbnail = null;
		}
	}

	private int getIndex(double[] values, double zoom) {
		var list = Arrays.stream(values).boxed().toList();
		var zoomValue = list.stream()
				.min(Comparator.comparingDouble(i -> Math.abs(i - zoom)))
				.orElse(values[0]);
		return ArrayUtils.indexOf(values, zoomValue);
	}

}
