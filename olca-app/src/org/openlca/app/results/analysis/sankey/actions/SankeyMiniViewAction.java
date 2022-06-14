package org.openlca.app.results.analysis.sankey.actions;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
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
import org.openlca.app.M;
import org.openlca.app.editors.graphical.actions.ActionIds;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.analysis.sankey.SankeyDiagram;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;

public class SankeyMiniViewAction extends Action {

	private final SankeyDiagram diagram;

	public SankeyMiniViewAction(SankeyDiagram diagram) {
		setId(ActionIds.OPEN_MINIATURE_VIEW);
		setText(M.OpenMiniatureView);
		setImageDescriptor(Icon.MINIATURE_VIEW.descriptor());
		this.diagram = diagram;
	}

	@Override
	public void run() {
		MiniView view = new MiniView(UI.shell(), diagram);
		view.open();
	}

	private static class MiniView extends ApplicationWindow {

		private boolean closed = false;

		private final GraphicalViewer viewer;
		private final ScalableRootEditPart part;

		public MiniView(Shell shell, SankeyDiagram diagram) {
			super(shell);
			viewer = diagram.getGraphicalViewer();
			part = (ScalableRootEditPart) viewer.getRootEditPart();
		}

		@Override
		protected Control createContents(Composite parent) {
			Composite composite = createForm(parent);
			createScale(composite);
			Canvas canvas = new Canvas(composite, SWT.BORDER);
			canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			LightweightSystem lws = new LightweightSystem(canvas);
			ScrollableThumbnail thumbnail = createThumbnail();
			lws.setContents(thumbnail);
			viewer.getControl().addDisposeListener((e) -> {
				if (!closed)
					close();
			});
			return super.createContents(parent);
		}

		private ScrollableThumbnail createThumbnail() {
			Viewport port = (Viewport) part.getFigure();
			IFigure figure = part.getLayer(LayerConstants.PRINTABLE_LAYERS);
			ScrollableThumbnail thumbnail = new ScrollableThumbnail(port);
			thumbnail.setSource(figure);
			return thumbnail;
		}

		private Composite createForm(Composite parent) {
			FormToolkit toolkit = new FormToolkit(Display.getCurrent());
			ScrolledForm form = toolkit.createScrolledForm(parent);
			Composite body = form.getBody();
			body.setLayout(new FillLayout());
			toolkit.paintBordersFor(body);
			SashForm sash = new SashForm(body, SWT.VERTICAL);
			toolkit.adapt(sash, true, true);
			Section section = toolkit.createSection(sash,
					ExpandableComposite.NO_TITLE | ExpandableComposite.EXPANDED);
			section.setText("");
			Composite composite = toolkit.createComposite(section, SWT.NONE);
			composite.setLayout(new GridLayout());
			section.setClient(composite);
			toolkit.paintBordersFor(composite);
			return composite;
		}

		private void createScale(Composite composite) {
			Scale scale = new Scale(composite, SWT.NONE);
			scale.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			double[] values = new double[] { 0.25, 0.5, 0.75, 1.0, 1.5,
					2.0, 2.5, 3.0, 4.0, 5.0, 10.0, 20.0 };
			scale.setIncrement(9);
			scale.setMinimum(0);
			scale.setMaximum(99);
			Controls.onSelect(scale, (e) -> {
				ZoomManager zoom = part.getZoomManager();
				zoom.setZoom(values[scale.getSelection() / 9]);
			});
			scale.setSelection(33);
		}

		@Override
		protected boolean showTopSeperator() {
			return false;
		}

		@Override
		public boolean close() {
			closed = true;
			return super.close();
		}
	}
}
