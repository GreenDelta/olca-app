package org.openlca.app.tools.graphics.frame;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
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
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;

import java.util.Arrays;
import java.util.Comparator;

public class Minimap extends Composite {

	protected static Dimension DEFAULT_SIZE = new Dimension(250, 250);
	private final MinimapLayout layout;

	private RootEditPart rootEditPart;
	private MinimapThumbnail thumbnail;
	private DisposeListener disposeListener;
	private Label scaleLabel;
	private Scale scale;
	private LightweightSystem lws;

	public Minimap(Composite parent, int style) {
		super(parent, style | SWT.BORDER);

		layout = new MinimapLayout();
		setLayout(layout);
	}

	public RootEditPart getRootEditPart() {
		return rootEditPart;
	}

	protected void initialize() {
		createZoomScale();
		createCanvas();
	}

	public void setRootEditPart(RootEditPart part) {
		rootEditPart = part;
	}

	protected void createZoomScale() {
		var zoomScale = new ZoomScale(this, SWT.NONE);
		setBackground(Colors.widgetBackground());
		setForeground(Colors.widgetForeground());

		var zoomScaleLayout = new GridLayout(2, false);
		zoomScaleLayout.marginHeight = 0;
		zoomScaleLayout.marginWidth = 0;
		zoomScale.setLayout(zoomScaleLayout);

		scale = UI.scale(zoomScale);
		scale.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		final double[] values = BasicGraphicalEditor.ZOOM_LEVELS;
		scale.setIncrement(1);
		scale.setMinimum(0);
		scale.setMaximum(values.length - 1);

		var zoomManager = rootEditPart.getZoomManager();
		Controls.onSelect(scale,
				(e) -> zoomManager.setZoom(values[scale.getSelection()], false));
		scale.setSelection(getIndex(values, zoomManager.getZoom()));

		int percentage = (int) (100 * zoomManager.getZoom());
		scaleLabel = UI.formLabel(zoomScale, percentage + "% ");
		scaleLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
	}

	protected void createCanvas() {
		var canvas = new Canvas(this, SWT.NONE);

		lws = new LightweightSystem(canvas);
		thumbnail = new MinimapThumbnail((Viewport) rootEditPart.getFigure());
		thumbnail.setSource(rootEditPart.getLayer(LayerConstants.PRINTABLE_LAYERS));
		lws.setContents(thumbnail);

		var viewer = rootEditPart.getViewer();
		disposeListener = e -> deactivate();
		viewer.getControl().addDisposeListener(disposeListener);
	}

	public void activate() {
		var viewer = getRootEditPart().getViewer();
		if (viewer != null) {
			disposeListener = e -> deactivate();
			viewer.getControl().addDisposeListener(disposeListener);
		}

		var zoomManager = getRootEditPart().getZoomManager();
		zoomManager.addZoomListener(zoom -> {
			int newPercentage = (int) (100 * zoom);
			scaleLabel.setText(newPercentage + "%");
			scale.setSelection(getIndex(BasicGraphicalEditor.ZOOM_LEVELS, zoom));
		});
	}

	private void deactivate() {
		if (thumbnail != null) {
			thumbnail.deactivate();
			thumbnail = null;
		}
		var viewer = rootEditPart.getViewer();
		if (viewer != null && viewer.getControl() != null)
			viewer.getControl().removeDisposeListener(disposeListener);
	}

	private int getIndex(double[] values, double zoom) {
		var list = Arrays.stream(values).boxed().toList();
		var zoomValue = list.stream()
				.min(Comparator.comparingDouble(i -> Math.abs(i - zoom)))
				.orElse(values[0]);
		return ArrayUtils.indexOf(values, zoomValue);
	}

	protected static class ZoomScale extends Composite {

		public ZoomScale(Composite parent, int style) {
			super(parent, style);
		}

	}

}
