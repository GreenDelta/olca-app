package org.openlca.app.components.charts;

import java.io.File;

import org.eclipse.birt.chart.computation.DataPointHints;
import org.eclipse.birt.chart.computation.LegendItemHints;
import org.eclipse.birt.chart.device.IDeviceRenderer;
import org.eclipse.birt.chart.device.IUpdateNotifier;
import org.eclipse.birt.chart.event.StructureSource;
import org.eclipse.birt.chart.event.StructureType;
import org.eclipse.birt.chart.exception.ChartException;
import org.eclipse.birt.chart.factory.GeneratedChartState;
import org.eclipse.birt.chart.factory.Generator;
import org.eclipse.birt.chart.factory.RunTimeContext;
import org.eclipse.birt.chart.model.Chart;
import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.attribute.ActionType;
import org.eclipse.birt.chart.model.attribute.Bounds;
import org.eclipse.birt.chart.model.attribute.TooltipValue;
import org.eclipse.birt.chart.model.attribute.impl.BoundsImpl;
import org.eclipse.birt.chart.model.impl.ChartWithAxesImpl;
import org.eclipse.birt.chart.render.IActionRenderer;
import org.eclipse.birt.chart.util.PluginSettings;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ISharedImages;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.util.ULocale;

/**
 * A SWT Composite for drawing/displaying a BIRT {@link Chart}
 */
public class ChartViewer extends Composite implements PaintListener,
		IUpdateNotifier, IActionRenderer {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private Chart chart;
	private RunTimeContext context;
	private Image image;
	private IDeviceRenderer renderer;
	private GeneratedChartState state;

	/**
	 * Constructor of ChartViewer
	 * 
	 * @param parent
	 *            the parent {@link Composite}
	 */
	public ChartViewer(Composite parent) {
		super(parent, SWT.NO_BACKGROUND);
		try {
			final PluginSettings ps = PluginSettings.instance();
			renderer = ps.getDevice("dv.SWT");
			renderer.setProperty(IDeviceRenderer.UPDATE_NOTIFIER, this);
		} catch (final ChartException pex) {
			log.error("Initializing chart viewer failed", pex);
		}
	}

	protected Chart getChart() {
		return chart;
	}

	protected Image getImage() {
		return image;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (renderer != null)
			renderer.dispose();
		if (image != null)
			image.dispose();
	}

	@Override
	public Chart getDesignTimeModel() {
		return chart;
	}

	@Override
	public Chart getRunTimeModel() {
		return state.getChartModel();
	}

	@Override
	public final void paintControl(final PaintEvent e) {
		if (chart == null) {
			// create dummy chart
			final ChartWithAxes chart = ChartWithAxesImpl.create();
			chart.getTitle().setVisible(false);
			chart.getPrimaryBaseAxes()[0].getLabel().setVisible(false);
			chart.getPrimaryOrthogonalAxis(chart.getBaseAxes()[0]).getLabel()
					.setVisible(false);
			this.chart = chart;
		}
		try {
			// prepare context
			context = Generator.instance().prepare(chart, null, null,
					ULocale.getDefault());
			context.setActionRenderer(this);
		} catch (final ChartException e1) {
			log.error("Prepare context failed", e1);
		}

		// create popup menu
		final MenuManager treeManager = new MenuManager();
		final Menu treeMenu = treeManager.createContextMenu(this);
		setMenu(treeMenu);
		treeManager.add(new SaveImageAction());
		final Rectangle rectangle = this.getClientArea();
		final Image chartImage = new Image(this.getDisplay(), rectangle);
		final GC gcImage = new GC(chartImage);
		renderer.setProperty(IDeviceRenderer.GRAPHICS_CONTEXT, gcImage);
		final Bounds bounds = BoundsImpl.create(0, 0, rectangle.width,
				rectangle.height);
		bounds.scale(72d / renderer.getDisplayServer().getDpiResolution());

		// get generator instance
		final Generator generator = Generator.instance();
		try {
			// build chart
			state = generator.build(renderer.getDisplayServer(), chart, bounds,
					null, context, null);
		} catch (final ChartException ce) {
			log.error("Building chart failed", ce);
		}
		try {
			// render chart
			generator.render(renderer, state);
			final GC gc = e.gc;
			gc.drawImage(chartImage, rectangle.x, rectangle.y);
			image = chartImage;
		} catch (final ChartException gex) {
			log.error("Rendering chart failed", gex);

		}
	}

	@Override
	public Object peerInstance() {
		return this;
	}

	@Override
	public void processAction(org.eclipse.birt.chart.model.data.Action action,
			StructureSource source, RunTimeContext arg2) {
		if (ActionType.SHOW_TOOLTIP_LITERAL.equals(action.getType())) {
			final TooltipValue tv = (TooltipValue) action.getValue();
			if (StructureType.SERIES_DATA_POINT.equals(source.getType())) {
				final DataPointHints dph = (DataPointHints) source.getSource();
				tv.setText(dph.getBaseDisplayValue() + " ("
						+ dph.getDisplayValue() + ")");
			} else if (StructureType.LEGEND_ENTRY.equals(source.getType())) {
				final LegendItemHints lih = (LegendItemHints) source
						.getSource();
				tv.setText(lih.getItemText());
			}
		}
	}

	@Override
	public void regenerateChart() {
		redraw();
	}

	@Override
	public void repaintChart() {
		redraw();
	}

	/**
	 * Updates the viewer with the new chart. Sets generation to true and
	 * redraws the image
	 * 
	 * @param chart
	 *            the {@link Chart} to be drawn
	 */
	public void updateChart(final Chart chart) {
		this.chart = chart;
		redraw();
		update();
	}

	private class SaveImageAction extends Action {

		@Override
		public ImageDescriptor getImageDescriptor() {
			return ImageType
					.getPlatformDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT);
		}

		@Override
		public String getText() {
			return Messages.SaveAsImage;
		}

		@Override
		public void run() {
			final ImageLoader loader = new ImageLoader();
			loader.data = new ImageData[] { image.getImageData() };
			final FileDialog dialog = new FileDialog(UI.shell(), SWT.SAVE);
			dialog.setText(Messages.SaveAsImage);
			dialog.setFileName("chart.png");
			dialog.setFilterExtensions(new String[] { "*.png" });
			dialog.setFilterNames(new String[] { "*.png (Portable Network Graphics (PNG)" });
			final String fileName = dialog.open();
			if (fileName != null && fileName.length() > 0) {
				final File file = new File(fileName);
				boolean write = false;
				if (file.exists()) {
					write = MessageDialog.openQuestion(UI.shell(),
							Messages.FileAlreadyExists,
							Messages.OverwriteFileQuestion);
				} else {
					write = true;
				}
				if (write) {
					loader.save(fileName, SWT.IMAGE_PNG);
				}
			}
		}

	}

}
