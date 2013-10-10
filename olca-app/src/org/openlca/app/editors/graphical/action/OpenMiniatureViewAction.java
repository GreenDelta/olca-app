package org.openlca.app.editors.graphical.action;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;

public class OpenMiniatureViewAction extends Action {

	private Control control;
	private IFigure figure;
	private Viewport port;
	private MiniView window;
	private ZoomManager zoomManager;

	OpenMiniatureViewAction() {
		setId(ActionIds.OPEN_MINIATURE_VIEW);
		setText(Messages.Systems_OpenMiniatureViewAction_Text);
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

	public void update(Viewport port, IFigure figure, Control control,
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
			final double[] values = new double[] { 0.25, 0.5, 0.75, 1.0, 1.5,
					2.0, 2.5, 3.0, 4.0, 5.0, 10.0, 20.0 };
			scale.setIncrement(9);
			scale.setMinimum(0);
			scale.setMaximum(99);
			scale.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(final SelectionEvent e) {
					zoomManager.setZoom(values[scale.getSelection() / 9]);
				}

			});
			scale.setSelection(33);
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
}
