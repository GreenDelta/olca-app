/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical.actions;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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

/**
 * Opens a new window with a miniature view of the graphical editor
 * 
 * @see Action
 * 
 * @author Sebastian Greve
 * 
 */
public class OpenMiniatureViewAction extends Action {

	/**
	 * The control of the graphical viewer
	 * 
	 * @see Control
	 */
	private Control control;

	/**
	 * The figure for the thumbnail
	 */
	private IFigure figure;

	/**
	 * The viewport of the graphical viewer
	 * 
	 * @see Viewport
	 */
	private Viewport port;

	/**
	 * The miniature view
	 */
	private MiniView window;

	/**
	 * The zoom manager of the graphical viewer
	 * 
	 * @see ZoomManager
	 */
	private ZoomManager zoomManager;

	/**
	 * Disposes the action and its window and control
	 */
	public void dispose() {
		port = null;
		if (window != null) {
			window.dispose();
			window = null;
		}
		figure = null;
		if (control != null) {
			control.dispose();
			control = null;
		}
		zoomManager = null;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.MINI_VIEW_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.Systems_OpenMiniatureViewAction_Text;
	}

	@Override
	public void run() {
		if (window == null) {
			window = new MiniView(UI.shell());
			window.open();
		} else {
			window.refresh();
		}
	}

	/**
	 * Updates the miniature view
	 * 
	 * @param port
	 *            The viewport of the graphical viewer
	 * @param figure
	 *            The figure for the thumbnail
	 * @param control
	 *            The control of the graphical viewer
	 * @param zoomManager
	 *            The zoom manager of the graphical viewer
	 */
	public void update(final Viewport port, final IFigure figure,
			final Control control, final ZoomManager zoomManager) {
		this.port = port;
		this.figure = figure;
		this.control = control;
		this.zoomManager = zoomManager;
		if (window != null) {
			window.refresh();
		}
	}

	/**
	 * Implementation of an application window
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class MiniView extends ApplicationWindow {

		/**
		 * The dispose listener
		 */
		private DisposeListener disposeListener;

		/**
		 * The light weight system
		 */
		private LightweightSystem lws;

		/**
		 * The thumbnail
		 */
		private ScrollableThumbnail thumbnail;

		/**
		 * Creates a new miniature view
		 * 
		 * @param parentShell
		 *            The parent shell
		 */
		public MiniView(final Shell parentShell) {
			super(parentShell);
		}

		/**
		 * Refreshes the thumbnail and light weight system
		 */
		private void refresh() {
			thumbnail = new ScrollableThumbnail(port);
			thumbnail.setSource(figure);
			lws.setContents(thumbnail);
		}

		@Override
		protected Control createContents(final Composite parent) {
			final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
			final ScrolledForm scrolledForm = toolkit
					.createScrolledForm(parent);
			final Composite body = scrolledForm.getBody();
			body.setLayout(new FillLayout());
			toolkit.paintBordersFor(body);

			final SashForm sashForm = new SashForm(body, SWT.VERTICAL);
			toolkit.adapt(sashForm, true, true);

			final Section categorySection = toolkit
					.createSection(sashForm, ExpandableComposite.NO_TITLE
							| ExpandableComposite.EXPANDED);
			categorySection.setText("");
			final Composite composite = toolkit.createComposite(
					categorySection, SWT.NONE);
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
			scale.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetDefaultSelected(final SelectionEvent e) {
					// no action on default selection
				}

				@Override
				public void widgetSelected(final SelectionEvent e) {
					zoomManager.setZoom(values[scale.getSelection() / 9]);
				}

			});
			scale.setSelection(33);
			final Canvas canvas = new Canvas(composite, SWT.BORDER);
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
					if (control != null && !control.isDisposed()) {
						control.removeDisposeListener(disposeListener);
					}
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

		/**
		 * Disposes the window
		 */
		public void dispose() {
			lws = null;
			disposeListener = null;
			if (thumbnail != null) {
				thumbnail.deactivate();
				thumbnail = null;
			}
		}

	}
}
