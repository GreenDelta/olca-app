package org.openlca.app.editors.graphical.action;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.GraphicalViewerConfigurator;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;

class OpenMiniatureViewAction extends EditorAction {

	private Control control;
	private IFigure figure;
	private Viewport port;
	private MiniView window;
	private ZoomManager zoomManager;

	OpenMiniatureViewAction() {
		setId(ActionIds.OPEN_MINIATURE_VIEW);
		setText(Messages.OpenMiniatureView);
		setImageDescriptor(ImageType.MINI_VIEW_ICON.getDescriptor());
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
			ZoomManager zoomManager) {
		this.port = port;
		this.figure = figure;
		this.control = control;
		this.zoomManager = zoomManager;
		if (window != null)
			window.refresh();
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
			final double[] values = GraphicalViewerConfigurator.ZOOM_LEVELS;
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
		protected boolean showTopSeperator() {
			return false;
		}

		@Override
		public boolean close() {
			window = null;
			return super.close();
		}

	}

	@Override
	protected boolean accept(ISelection selection) {
		if (getEditor() == null)
			return false;
		update(getViewport(),
				getRootEditPart().getLayer(LayerConstants.PRINTABLE_LAYERS),
				getEditor().getGraphicalViewer().getControl(), getEditor()
						.getZoomManager());
		return true;
	}

	private Viewport getViewport() {
		return (Viewport) getRootEditPart().getFigure();
	}

	private ScalableRootEditPart getRootEditPart() {
		return (ScalableRootEditPart) getEditor().getGraphicalViewer()
				.getRootEditPart();
	}
}
