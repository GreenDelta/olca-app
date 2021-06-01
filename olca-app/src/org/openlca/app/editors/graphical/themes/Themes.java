package org.openlca.app.editors.graphical.themes;

import java.util.Objects;

public final class Themes {

	private static Theme[] themes;
	private static final Theme defaultTheme = new DefaultTheme();

	private Themes() {
	}

	public static Theme[] all() {
		if (themes != null)
			return themes;
		themes = new Theme[]{
			getDefault(),
			new GreyTheme(),
			new WhiteTheme(),
			new DarkTheme(),
		};
		return themes;
	}

	public static Theme getDefault() {
		return defaultTheme;
	}

	public static Theme get(String id) {
		for (var t : all()) {
			if (Objects.equals(id, t.id()))
				return t;
		}
		return getDefault();
	}
}
