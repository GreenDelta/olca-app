package org.openlca.app.tools.graphics.figures;

import java.io.ByteArrayOutputStream;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gmf.runtime.draw2d.ui.internal.mapmode.DiagramMapModeUtil;
import org.eclipse.gmf.runtime.draw2d.ui.mapmode.MapModeTypes;
import org.eclipse.gmf.runtime.draw2d.ui.mapmode.MapModeUtil;
import org.eclipse.gmf.runtime.draw2d.ui.render.RenderInfo;
import org.eclipse.gmf.runtime.draw2d.ui.render.RenderedImage;
import org.eclipse.gmf.runtime.draw2d.ui.render.factory.RenderedImageFactory;
import org.eclipse.gmf.runtime.draw2d.ui.render.internal.RenderHelper;
import org.eclipse.gmf.runtime.draw2d.ui.render.internal.RenderingListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.RGB;

/**
 * <i>This class is a copy of
 *  {@link org.eclipse.gmf.runtime.draw2d.ui.render.figures.ScalableImageFigure}
 *  to have the ability to use default
 *  {@link  org.eclipse.gmf.runtime.draw2d.ui.mapmode.MapModeTypes}.</i>
 *
 * <p>
 * An implementation of {@link org.eclipse.draw2d.ImageFigure} that allows
 * scaling the underlying image to the containing Figure's bounds, rather than
 * being fixed to the image size.
 *
 * <p>
 * Any image that can be implemented inside the RenderedImage interface can be
 * supported.
 * </p>
 *
 * @author jcorchis / sshaw
 */
@SuppressWarnings("restriction")
public class ScalableImageFigure extends ImageFigure {

	private RenderingListenerImpl renderingListener = new RenderingListenerImpl();

	private class RenderingListenerImpl
			implements RenderingListener {

		public RenderingListenerImpl() {
			super();
		}

		@Override
		public void paintFigureWhileRendering(Graphics g) {
			ScalableImageFigure.this.paintFigureWhileRendering(g);
		}

		@Override
		public void imageRendered(RenderedImage rndImg) {
			if (ScalableImageFigure.this.getParent() != null) {
				ScalableImageFigure.this.setRenderedImage(rndImg);
				ScalableImageFigure.this.repaint();
			}
		}

		/**
		 * @return <code>IFigure</code> that the listener wraps
		 */
		public ScalableImageFigure getFigure() {
			return ScalableImageFigure.this;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof RenderingListenerImpl) {
				return ((RenderingListenerImpl) obj).getFigure().equals(
						getFigure());
			}

			return false;
		}

		@Override
		public int hashCode() {
			return ScalableImageFigure.this.hashCode();
		}

	}

	/** The preferred size of the image */
	private Dimension preferredSize = new Dimension(-1, -1);

	private static final int FLAG_USE_DEFAULT_IMAGESIZE = MAX_FLAG << 1,
			FLAG_MAINTAIN_ASPECT_RATIO = MAX_FLAG << 2,
			FLAG_ANTI_ALIAS = MAX_FLAG << 3,
			FLAG_USE_ORIGINAL_COLORS = MAX_FLAG << 4;

	/** The last rendered <code>RenderedImage</code> */
	private RenderedImage lastRenderedImage = null;

	/**
	 * Accessor to determine if the rendered image will be anti-aliased (if
	 * possible).
	 *
	 * @return <code>boolean</code> <code>true</code> if anti aliasing is
	 *         on, <code>false</code> otherwise.
	 */
	public boolean isAntiAlias() {
		return getFlag(FLAG_ANTI_ALIAS);
	}

	/**
	 * Sets a property to determine if the rendered image will be anti-aliased
	 * (if possible).
	 *
	 * @param antiAlias
	 *            <code>boolean</code> <code>true</code> if anti-aliasing is
	 *            to be turned on, <code>false</code> otherwise
	 */
	public void setAntiAlias(boolean antiAlias) {
		setFlag(FLAG_ANTI_ALIAS, antiAlias);
		invalidate();
	}

	/**
	 * Accessor to determine if the rendered image will respect the original
	 * aspect ratio of the default image when resized.
	 *
	 * @return <code>boolean</code> <code>true</code> if maintain aspect
	 *         ratio is on, <code>false</code> otherwise.
	 */
	public boolean isMaintainAspectRatio() {
		return getFlag(FLAG_MAINTAIN_ASPECT_RATIO);
	}

	/**
	 * Sets a property to determine if the rendered image will respect the
	 * original aspect ratio of the default image when resized.
	 *
	 * @param maintainAspectRatio
	 *            <code>boolean</code> <code>true</code> if maintain aspect
	 *            ratio is to be turned on, <code>false</code> otherwise
	 */
	public void setMaintainAspectRatio(boolean maintainAspectRatio) {
		setFlag(FLAG_MAINTAIN_ASPECT_RATIO, maintainAspectRatio);
		invalidate();
	}

	/**
	 * @param img
	 *            the <code>Image</code> to render
	 */
	public ScalableImageFigure(Image img) {
		ImageLoader imageLoader = new ImageLoader();
		ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
		imageLoader.data = new ImageData[] {img.getImageData()};
		imageLoader.logicalScreenWidth = img.getBounds().width;
		imageLoader.logicalScreenHeight = img.getBounds().height;
		imageLoader.save(byteOS, SWT.IMAGE_BMP);
		this.lastRenderedImage = RenderedImageFactory.getInstance(byteOS
				.toByteArray());

		setFlag(FLAG_USE_DEFAULT_IMAGESIZE, false);
		setFlag(FLAG_USE_ORIGINAL_COLORS, false);
		setFlag(FLAG_MAINTAIN_ASPECT_RATIO, true);
		setFlag(FLAG_ANTI_ALIAS, true);
	}

	public ScalableImageFigure(RenderedImage renderedImage) {
		this(renderedImage, false, false, true);
	}

	/**
	 * Constructor for meta image sources.
	 *
	 * @param renderedImage
	 *            the <code>RenderedImage</code> that is used for rendering
	 *            the image.
	 */
	public ScalableImageFigure(RenderedImage renderedImage, boolean antiAlias) {
		this(renderedImage, false, false, antiAlias);
	}

	/**
	 * Constructor for meta image sources.
	 *
	 * @param renderedImage
	 *            the <code>RenderedImage</code> that is used for rendering
	 *            the image.
	 * @param useDefaultImageSize
	 *            <code>boolean</code> indicating whether to initialize the
	 *            preferred size with the default image size. Otherwise, a set
	 *            default will be used instead.
	 * @param useOriginalColors
	 *            <code>boolean</code> indicating whether to use the original
	 *            colors of the <code>RenderedImage</code> or to replace black
	 *            with outline color and white with the fill color.
	 */
	public ScalableImageFigure(RenderedImage renderedImage,
														 boolean useDefaultImageSize, boolean useOriginalColors,
														 boolean antiAlias) {
		lastRenderedImage = renderedImage;
		setFlag(FLAG_USE_DEFAULT_IMAGESIZE, useDefaultImageSize);
		setFlag(FLAG_USE_ORIGINAL_COLORS, useOriginalColors);
		setFlag(FLAG_MAINTAIN_ASPECT_RATIO, true);
		setFlag(FLAG_ANTI_ALIAS, antiAlias);
	}

	/**
	 * Sets the preferred size of the image figure.
	 *
	 * @param w
	 *            the preferred width of the image
	 * @param h
	 *            the preferred height of the image
	 */
	public void setPreferredImageSize(int w, int h) {
		preferredSize = new Dimension(w, h);
	}

	/**
	 * Returns the size set specified by setPreferredImageSize() or the size
	 * specified by the image. In the case of meta-images a preferred size of
	 * 32x32 is returned.
	 */
	public Dimension getPreferredSize(int wHint, int hHint) {
		if (preferredSize.height == -1 && preferredSize.width == -1) {
			int extent = MapModeUtil.getMapMode(this).DPtoLP(32);
			preferredSize = new Dimension(extent, extent);
			if (getFlag(FLAG_USE_DEFAULT_IMAGESIZE)) {
				if (getRenderedImage() != null) {
					setRenderedImage(getRenderedImage(new Dimension(0, 0)));
					Image swtImage = null;
					if (getRenderedImage() != null)
						swtImage = getRenderedImage().getSWTImage();
					if (swtImage != null) {
						org.eclipse.swt.graphics.Rectangle imgRect = swtImage
								.getBounds();
						preferredSize.width = MapModeUtil.getMapMode(this)
								.DPtoLP(imgRect.width);
						preferredSize.height = MapModeUtil.getMapMode(this)
								.DPtoLP(imgRect.height);
					}
				}
			}
		}
		return preferredSize;
	}

	@Override
	public void setBounds(Rectangle rect) {
		Dimension devDim = new Dimension(rect.getSize());
		MapModeTypes.DEFAULT_MM.LPtoDP(devDim);

		if (getRenderedImage() != null) {
			setRenderedImage(getRenderedImage(devDim));
		}

		super.setBounds(rect);
	}

	/**
	 * Override to return an image that is scaled to fit the bounds of the
	 * figure.
	 */
	public Image getImage() {
		if (getRenderedImage() == null)
			return null;

		return getRenderedImage().getSWTImage();
	}

	/**
	 * Gets the <code>RenderedImage</code> that is the for the specified
	 * <code>Dimension</code>
	 *
	 * @return the <code>RenderedImage</code>
	 */
	private RenderedImage getRenderedImage(Dimension dim) {
		Color fill = getBackgroundColor();
		Color outline = getForegroundColor();
		RenderInfo newRenderInfo = getRenderedImage().getRenderInfo();
		newRenderInfo.setValues(dim.width,
				dim.height,
				isMaintainAspectRatio(), // maintain aspect ratio
				isAntiAlias(),
				useOriginalColors() ? null
						: new RGB(fill.getRed(), fill.getGreen(), fill.getBlue()),
				useOriginalColors() ? null
						: new RGB(outline.getRed(), outline.getGreen(), outline.getBlue())); // antialias

		return getRenderedImage().getNewRenderedImage(newRenderInfo);
	}
	/**
	 * @return a <code>boolean</code> <code>true</code> if the original
	 *         colors of the image should be used for rendering, or
	 *         <code>false</code> indicates that black and white colors can
	 *         replaced by the specified outline and fill colors respectively of
	 *         the <code>RenderInfo</code>.
	 */
	public boolean useOriginalColors() {
		return getFlag(FLAG_USE_ORIGINAL_COLORS);
	}

	/**
	 * If the rendering is occuring on a separate thread, this method is a hook to draw a temporary
	 * image onto the drawing surface.
	 *
	 * @param g the <code>Graphics</code> object to paint the temporary image to
	 */
	protected void paintFigureWhileRendering(Graphics g) {
		Rectangle area = getClientArea().getCopy();

		g.pushState();
		g.setBackgroundColor(ColorConstants.white);
		g.fillRectangle(area.x, area.y, area.width - 1, area.height - 1);
		g.setForegroundColor(ColorConstants.red);
		g.drawRectangle(area.x, area.y, area.width - 1, area.height - 1);
		g.setLineStyle(SWT.LINE_DOT);
		g.drawLine(area.x, area.y, area.x + area.width, area.y
				+ area.height);
		g.drawLine(area.x + area.width, area.y, area.x, area.y
				+ area.height);
		g.popState();
	}

	@Override
	protected void paintFigure(Graphics graphics) {
		Rectangle area = getClientArea().getCopy();

		RenderInfo rndInfo = getRenderedImage().getRenderInfo();
		if (!useOriginalColors()) {
			RGB backgroundColor = getBackgroundColor().getRGB();
			RGB foregroundColor = getForegroundColor().getRGB();
			if ((backgroundColor != null && !backgroundColor.equals(rndInfo.getBackgroundColor())) ||
					(foregroundColor != null && !foregroundColor.equals(rndInfo.getForegroundColor()))) {
				rndInfo.setValues(rndInfo.getWidth(), rndInfo.getHeight(),
						rndInfo.shouldMaintainAspectRatio(),
						rndInfo.shouldAntiAlias(), getBackgroundColor().getRGB(), getForegroundColor().getRGB());
				setRenderedImage(getRenderedImage().getNewRenderedImage(rndInfo));
			}
		}

		setRenderedImage(RenderHelper.getInstance(
				DiagramMapModeUtil.getScale(MapModeTypes.DEFAULT_MM), false,
				false, null).drawRenderedImage(graphics, getRenderedImage(), area,
				renderingListener));
	}



	/**
	 * Gets the <code>RenderedImage</code> that is being displayed by this
	 * figure.
	 *
	 * @return <code>RenderedImage</code> that is being displayed by this
	 *         figure.
	 */
	public RenderedImage getRenderedImage() {
		return lastRenderedImage;
	}

	/**
	 * Sets the <code>RenderedImage</code> that is to be displayed by this
	 * figure
	 *
	 * @param renderedImage <code>RenderedImage</code> that is to being displayed
	 *                      by this figure
	 */
	public void setRenderedImage(RenderedImage renderedImage) {
		if (this.lastRenderedImage != renderedImage) {
			this.lastRenderedImage = renderedImage;
			if (renderedImage == null || renderedImage.isRendered()) {
				notifyImageChanged();
			}
		}
	}

}
