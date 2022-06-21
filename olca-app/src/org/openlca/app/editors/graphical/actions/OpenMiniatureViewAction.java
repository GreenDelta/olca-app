package org.openlca.app.editors.graphical.actions;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.GraphRoot;
import org.openlca.app.editors.graphical.zoom.GraphZoomManager;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;

public class OpenMiniatureViewAction extends WorkbenchPartAction {

	private final GraphEditor editor;
	private Control control;
	private IFigure figure;
	private Viewport port;
	private MiniView window;
	private GraphZoomManager zoomManager;

	public OpenMiniatureViewAction(GraphEditor part) {
		super(part);
		this.editor = part;
		setId(ActionIds.OPEN_MINIATURE_VIEW);
		setText(M.OpenMiniatureView);
		setImageDescriptor(Icon.MINIATURE_VIEW.descriptor());
	}

	@Override
	public void run() {
		if (window == null) {
			window = new MiniView(UI.shell());
			window.open();
		} else
			window.refresh();
	}

	private void update(Viewport port, IFigure figure, Control control,
											GraphZoomManager zoomManager) {
		this.port = port;
		this.figure = figure;
		this.control = control;
		this.zoomManager = zoomManager;
		if (window != null)
			window.refresh();
	}

	@Override
	protected boolean calculateEnabled() {
		if (editor == null)
			return false;
		IFigure layer = getRootEditPart().getLayer(LayerConstants.PRINTABLE_LAYERS);
		var viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
		update(getViewport(), layer, viewer.getControl(), editor.getZoomManager());
		return true;
	}

	private class MiniView extends ApplicationWindow {

		private DisposeListener disposeListener;
		private LightweightSystem lws;
		private ScrollableThumbnail thumbnail;

		public MiniView(Shell parentShell) {
			super(parentShell);
		}

		private void refresh() {
			thumbnail = new ScrollableThumbnail(port);
			thumbnail.setSource(figure);
			lws.setContents(thumbnail);
		}

		@Override
		protected Control createContents(final Composite parent) {
			FormToolkit toolkit = new FormToolkit(Display.getCurrent());
			ScrolledForm scrolledForm = toolkit.createScrolledForm(parent);
			Composite body = scrolledForm.getBody();
			body.setLayout(new FillLayout());
			toolkit.paintBordersFor(body);

			SashForm sashForm = new SashForm(body, SWT.VERTICAL);
			toolkit.adapt(sashForm, true, true);

			Section categorySection = toolkit
					.createSection(sashForm, ExpandableComposite.NO_TITLE
							| ExpandableComposite.EXPANDED);
			categorySection.setText("");
			Composite composite = toolkit.createComposite(categorySection,
					SWT.NONE);
			composite.setLayout(new GridLayout());
			categorySection.setClient(composite);
			toolkit.paintBordersFor(composite);
			final Scale scale = new Scale(composite, SWT.NONE);
			scale.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			final double[] values = GraphEditor.ZOOM_LEVELS;
			final int increment = 100 / (values.length - 1);
			scale.setIncrement(increment);
			scale.setMinimum(0);
			scale.setMaximum(100);
			Controls.onSelect(scale, (e) -> {
				zoomManager.setZoom(values[scale.getSelection() / increment]);
			});
			scale.setSelection(increment * (values.length - 1) / 2);
			Canvas canvas = new Canvas(composite, SWT.BORDER);
			canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			lws = new LightweightSystem(canvas);
			thumbnail = new ScrollableThumbnail(port);
			thumbnail.setSource(figure);
			lws.setContents(thumbnail);
			disposeListener = new DisposeListener() {

				@Override
				public void widgetDisposed(final DisposeEvent e) {
					if (thumbnail != null) {
						thumbnail.deactivate();
						thumbnail = null;
					}
					if (control != null && !control.isDisposed())
						control.removeDisposeListener(disposeListener);
					close();
				}
			};
			control.addDisposeListener(disposeListener);
			return super.createContents(parent);
		}
		@Override
		protected Point getInitialSize() {
			return UI.initialSizeOf(this, 600, 350);
		}

		@Override
		protected boolean showTopSeperator() {
			return false;
		}

		@Override
		public boolean close() {
			window = null;
			return super.close();
		}

	}

	private Viewport getViewport() {
		return (Viewport) getRootEditPart().getFigure();
	}

	private GraphRoot getRootEditPart() {
		var viewer = (GraphicalViewer) editor.getAdapter(GraphicalViewer.class);
		return (GraphRoot) viewer.getRootEditPart();
	}
}
