package org.openlca.app.editors.sd;

import java.io.ByteArrayInputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.sd.eqn.Simulator;
import org.openlca.sd.xmile.Xmile;
import org.openlca.sd.xmile.svg.Svg;

import com.github.weisj.jsvg.parser.SVGLoader;

class SdInfoPage extends FormPage {

	private final SdModelEditor editor;

	SdInfoPage(SdModelEditor editor) {
		super(editor, "SdModelInfoPage", M.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm, "System dynamics model: " + editor.modelName());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		infoSection(body, tk);
		imageSection(body, tk);
	}

	private void infoSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.GeneralInformation);
		UI.gridLayout(comp, 3);

		var nameText = UI.labeledText(comp, tk, M.Name);
		nameText.setEditable(false);
		nameText.setText(editor.modelName());
		UI.filler(comp, tk);

		var specs = SimSpecs.of(editor.xmile());

		var methodText = UI.labeledText(comp, tk, "Solver method");
		methodText.setEditable(false);
		methodText.setText(specs.method);
		UI.filler(comp, tk);

		var startText = UI.labeledText(comp, tk, "Start time");
		startText.setEditable(false);
		startText.setText(Double.toString(specs.start));
		UI.label(comp, tk, specs.timeUnit);

		var endText = UI.labeledText(comp, tk, "Stop time");
		endText.setEditable(false);
		endText.setText(Double.toString(specs.stop));
		UI.label(comp, tk, specs.timeUnit);

		var dtText = UI.labeledText(comp, tk, "Δt");
		dtText.setEditable(false);
		dtText.setText(Double.toString(specs.dt));
		UI.label(comp, tk, specs.timeUnit);

		UI.filler(comp, tk);
		var btn = UI.button(comp, tk, "Run simulation");
		btn.setImage(Icon.RUN.get());
		Controls.onSelect(btn, e -> runSimulation());
	}

	private void imageSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Model graph");
		UI.gridLayout(comp, 1);

		var svg = Svg.xmlOf(editor.xmile());
		if (svg.hasError()) {
			UI.label(comp, tk, "No model graph available");
			return;
		}

		String xml = svg.value();
		if (xml == null || xml.trim().isEmpty()) {
			UI.label(comp, tk, "No model graph available");
			return;
		}

		// Create canvas for SVG rendering
		var canvas = new Canvas(comp, SWT.BORDER);
		UI.gridData(canvas, true, true).minimumHeight = 300;
		canvas.setBackground(Colors.white());

		// Render SVG
		canvas.addPaintListener(e -> renderSvg(e.gc, xml, canvas.getBounds().width, canvas.getBounds().height));
	}

	private void runSimulation() {
		var sim = Simulator.of(editor.xmile());
		if (sim.hasError()) {
			MsgBox.error("Failed to create simulator", sim.error());
			return;
		}
		SdResultEditor.open(editor.modelName(), sim.value());
	}

	private void renderSvg(GC gc, String svgXml, int canvasWidth, int canvasHeight) {
		try {
			// Parse SVG using JSVG
			var loader = new SVGLoader();
			var document = loader.load(new ByteArrayInputStream(svgXml.getBytes("UTF-8")));
			if (document == null) {
				drawErrorMessage(gc, "Failed to parse SVG", canvasWidth, canvasHeight);
				return;
			}

			// Get SVG dimensions
			var size = document.size();
			if (size == null) {
				drawErrorMessage(gc, "Invalid SVG dimensions", canvasWidth, canvasHeight);
				return;
			}

			// Calculate scaling to fit canvas while maintaining aspect ratio
			double svgWidth = size.getWidth();
			double svgHeight = size.getHeight();

			if (svgWidth <= 0 || svgHeight <= 0) {
				drawErrorMessage(gc, "Invalid SVG size", canvasWidth, canvasHeight);
				return;
			}

			double scaleX = (canvasWidth - 20.0) / svgWidth; // 10px margin on each side
			double scaleY = (canvasHeight - 20.0) / svgHeight; // 10px margin top/bottom
			double scale = Math.min(scaleX, scaleY);

			// Center the image
			int scaledWidth = (int) (svgWidth * scale);
			int scaledHeight = (int) (svgHeight * scale);
			int x = (canvasWidth - scaledWidth) / 2;
			int y = (canvasHeight - scaledHeight) / 2;

			// Create AWT BufferedImage for JSVG rendering
			var bufferedImage = new java.awt.image.BufferedImage(
					scaledWidth, scaledHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB);
			var graphics2D = bufferedImage.createGraphics();

			// Enable antialiasing
			graphics2D.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
					java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
					java.awt.RenderingHints.VALUE_RENDER_QUALITY);

			// Scale and render SVG
			graphics2D.scale(scale, scale);
			document.render(null, graphics2D);
			graphics2D.dispose();

			// Convert BufferedImage to SWT Image
			var swtImage = convertToSwtImage(bufferedImage);
			if (swtImage != null) {
				gc.drawImage(swtImage, x, y);
				swtImage.dispose();
			} else {
				drawErrorMessage(gc, "Failed to convert image", canvasWidth, canvasHeight);
			}

		} catch (Exception e) {
			ErrorReporter.on("Failed to render SVG", e);
			drawErrorMessage(gc, "Error rendering SVG: " + e.getMessage(), canvasWidth, canvasHeight);
		}
	}

	private void drawErrorMessage(GC gc, String message, int width, int height) {
		gc.setForeground(Colors.systemColor(SWT.COLOR_RED));
		var textExtent = gc.textExtent(message);
		int x = (width - textExtent.x) / 2;
		int y = (height - textExtent.y) / 2;
		gc.drawText(message, x, y, true);
	}

	private Image convertToSwtImage(java.awt.image.BufferedImage bufferedImage) {
		try {
			int width = bufferedImage.getWidth();
			int height = bufferedImage.getHeight();

			// Create SWT ImageData
			var imageData = new org.eclipse.swt.graphics.ImageData(width, height, 24,
					new org.eclipse.swt.graphics.PaletteData(0xFF0000, 0x00FF00, 0x0000FF));

			// Convert pixel by pixel
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int rgb = bufferedImage.getRGB(x, y);
					int alpha = (rgb >> 24) & 0xFF;
					int red = (rgb >> 16) & 0xFF;
					int green = (rgb >> 8) & 0xFF;
					int blue = rgb & 0xFF;

					// Convert to SWT RGB format
					int swtRgb = (red << 16) | (green << 8) | blue;
					imageData.setPixel(x, y, swtRgb);
					if (imageData.alphaData != null) {
						imageData.setAlpha(x, y, alpha);
					}
				}
			}

			return new Image(null, imageData);
		} catch (Exception e) {
			ErrorReporter.on("Failed to convert BufferedImage to SWT Image", e);
			return null;
		}
	}

	private record SimSpecs(
			double start,
			double stop,
			double dt,
			String timeUnit,
			String method
	) {

		static SimSpecs of(Xmile xmile) {
			if (xmile == null || xmile.simSpecs() == null)
				return new SimSpecs(0, 0, 0, "", "");
			var specs = xmile.simSpecs();
			double dt = 1;
			if (specs.dt() != null && specs.dt().value() != null) {
				dt = specs.dt().reciprocal() != null && specs.dt().reciprocal()
						? 1 / specs.dt().value()
						: specs.dt().value();
			}

			return new SimSpecs(
					specs.start() != null ? specs.start() : 0,
					specs.stop() != null ? specs.stop() : 0,
					dt,
					specs.timeUnits() != null ? specs.timeUnits() : "",
					specs.method() != null ? specs.method() : ""
			);
		}
	}

}
