package org.openlca.app.results.comparison.display;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;

public class ColorPaletteHelper {
	private static Map<Integer, RGB> map = initColorsMap();
	private static int NB_COLORS;
	

	private static Map<Integer, RGB> initColorsMap() {
		RGB colorsPalette[] = { new RGB(255, 82, 82), new RGB(255, 64, 129), new RGB(224, 64, 251),
				new RGB(124, 77, 255), new RGB(83, 109, 254), new RGB(68, 138, 255), new RGB(64, 196, 255),
				new RGB(24, 255, 255), new RGB(100, 255, 218), new RGB(105, 240, 174), new RGB(178, 255, 89),
				new RGB(238, 255, 65), new RGB(255, 255, 0), new RGB(255, 215, 64), new RGB(255, 171, 64),
				new RGB(255, 61, 0), new RGB(121, 85, 72), new RGB(96, 125, 139) };
		Map<Integer, RGB> map = new HashMap<>();
		for (int i = 0; i < colorsPalette.length; i++) {
			map.put(i, colorsPalette[i]);
		}
		NB_COLORS = map.size();
		return map;
	}
	
	public static RGB getColor(double value) {
		return map.get((int)(value*100)%NB_COLORS);
	}

}
