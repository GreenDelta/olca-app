package org.openlca.app.tools.graphics.themes;

import java.io.File;
import java.util.EnumMap;
import java.util.Optional;

import com.helger.css.decl.CSSStyleRule;
import com.helger.css.reader.CSSReader;
import com.helger.css.reader.CSSReaderSettings;
import org.eclipse.swt.graphics.Color;
import org.openlca.app.util.Colors;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;
import org.openlca.core.model.descriptors.ResultDescriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

public class Theme {

	private final String file;
	private final String name;
	private final Integer version;

	private boolean isDark;
	private Color backgroundColor;
	private Color defaultLinkColor;
	private Color defaultLinkColorSelection;
	private Color infoLabelColor;

	private final EnumMap<FlowType, Color> flowLabelColors;
	private final EnumMap<FlowType, Color> linkColors;
	private final EnumMap<Box, BoxConfig> boxConfigs;

	private Theme(String file, String name, Integer version) {
		this.file = file;
		this.name = name;
		this.version = version;
		this.flowLabelColors = new EnumMap<>(FlowType.class);
		this.linkColors = new EnumMap<>(FlowType.class);
		this.boxConfigs = new EnumMap<>(Box.class);
	}

	public static Theme defaults(String file, String name, Integer version) {
		return new Theme(file, name, version);
	}

	/**
	 * Returns the file name of a theme. All themes are located in a single folder.
	 * Thus, the file name can be used as the ID of the theme.
	 *
	 * @return the file name of the theme
	 */
	public String file() {
		return file;
	}

	public String name() {
		return name;
	}

	public Integer version() {
		return version;
	}

	public boolean isDark() {
		return isDark;
	}

	public Color backgroundColor() {
		return backgroundColor == null
				? Colors.white()
				: backgroundColor;
	}

	public Color boxFontColor(Box box) {
		var config = boxConfigs.get(box);
		return config != null && config.fontColor != null
				? config.fontColor
				: Colors.black();
	}

	public Color boxBackgroundColor(Box box) {
		var config = boxConfigs.get(box);
		return config != null && config.backgroundColor != null
				? config.backgroundColor
				: backgroundColor();
	}

	public Color boxBorderColor(Box box) {
		var config = boxConfigs.get(box);
		return config != null && config.borderColor != null
				? config.borderColor
				: Colors.black();
	}

	public int boxBorderWidth(Box box) {
		var config = boxConfigs.get(box);
		return config != null
				? Math.max(1, config.borderWidth)
				: 1;
	}

	public Color linkColor() {
		return defaultLinkColor == null
				? Colors.black()
				: defaultLinkColor;
	}

	public Color linkColorSelected() {
		return defaultLinkColorSelection == null
				? Colors.darkGray()
				: defaultLinkColorSelection;
	}

	public Color linkColor(FlowType flowType) {
		var color = linkColors.get(flowType);
		return color != null
				? color
				: linkColor();
	}

	public Color infoLabelColor() {
		return infoLabelColor == null
				? Colors.black()
				: infoLabelColor;
	}

	public Color labelColor(FlowType flowType) {
		var type = flowType == null
				? FlowType.PRODUCT_FLOW
				: flowType;
		return flowLabelColors.getOrDefault(
				type,boxFontColor(Box.DEFAULT));
	}

	public static Optional<Theme> loadFrom(File file, String id) {
		if (file == null)
			return Optional.empty();

		var settings = new CSSReaderSettings();
		var css = CSSReader.readFromFile(file, settings);
		if (css == null)
			return Optional.empty();

		// select the theme name
		var name = Css.themeNameOf(css).orElse(null);
		if (name == null || name.isBlank()) {
			name = file.getName();
			if (name.endsWith(".css")) {
				name = name.substring(0, name.length() - 4);
			}
		}

		// set the version of the theme
		var version = Css.themeVersionOf(css).orElse(0);

		var theme = defaults(file.getName(), name, version);

		theme.isDark = Css.hasDarkMode(css);

		for (int i = 0; i < css.getStyleRuleCount(); i++) {
			var rule = css.getStyleRuleAtIndex(i);
			if (Css.asId(rule, id)) {

				// box config
				Css.boxOf(rule)
						.ifPresent(box -> theme.styleBox(box, rule));

				// root config
				if (Css.isBody(rule)) {
					Css.getBackgroundColor(rule)
							.ifPresent(color -> theme.backgroundColor = color);
				}

				// links
				if (Css.isLink(rule)) {
					var flowType = Css.flowTypeOf(rule);
					var selection = Css.isSelection(rule);
					Css.getColor(rule).ifPresent(color -> {
						if (selection)
							theme.defaultLinkColorSelection = color;
						else if (flowType.isPresent()) {
							theme.linkColors.put(flowType.get(), color);
						} else {
							theme.defaultLinkColor = color;
						}
					});
				}

				// labels
				if (Css.isLabel(rule)) {
					Css.getColor(rule).ifPresent(color -> {
						if (Css.isInfo(rule)) {
							theme.infoLabelColor = color;
						}
						Css.flowTypeOf(rule).ifPresent(flowType -> theme.flowLabelColors.put(flowType, color));
					});
				}
			}
		}
		return Optional.of(theme);
	}

	private void styleBox(Box box, CSSStyleRule rule) {
		var config = boxConfigs.computeIfAbsent(box, $ -> new BoxConfig());
		Css.getColor(rule)
				.ifPresent(color -> config.fontColor = color);
		Css.getBackgroundColor(rule)
				.ifPresent(color -> config.backgroundColor = color);
		Css.getBorderColor(rule)
				.ifPresent(color -> config.borderColor = color);
		Css.getBorderWidth(rule)
				.ifPresent(width -> config.borderWidth = width);
	}

	public enum Box {
		DEFAULT,
		REFERENCE_PROCESS,
		UNIT_PROCESS,
		SYSTEM_PROCESS,
		SUB_SYSTEM,
		LIBRARY_PROCESS,
		RESULT,
		STICKY_NOTE;

		public static Box of(RootDescriptor descriptor, boolean isReference) {
			if (descriptor == null)
				return DEFAULT;
			if (descriptor.isFromLibrary())
				return LIBRARY_PROCESS;
			if (isReference)
				return REFERENCE_PROCESS;
			if (descriptor instanceof ProcessDescriptor p)
				return p.processType == ProcessType.UNIT_PROCESS
						? UNIT_PROCESS
						: SYSTEM_PROCESS;
			if (descriptor instanceof ResultDescriptor)
				return RESULT;
			if (descriptor instanceof ProductSystemDescriptor)
				return SUB_SYSTEM;
			return DEFAULT;
		}
	}

	private static class BoxConfig {
		Color fontColor;
		Color backgroundColor;
		Color borderColor;
		int borderWidth;
	}

}
