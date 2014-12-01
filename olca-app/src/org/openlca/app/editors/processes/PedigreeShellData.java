package org.openlca.app.editors.processes;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.openlca.app.util.Colors;
import org.openlca.core.model.PedigreeMatrixRow;
import org.slf4j.LoggerFactory;

class PedigreeShellData {

	private Properties properties;

	public PedigreeShellData() {
		properties = new Properties();
		try {
			properties.load(getClass().getResourceAsStream(
					"matrix_shell_data.properties"));
		} catch (Exception e) {
			LoggerFactory.getLogger(getClass()).error(
					"Could not load properties matrix_shell_data", e);
		}
	}

	public String getRowLabel(PedigreeMatrixRow row) {
		return properties.getProperty(row.name());
	}

	public String getLabel(PedigreeMatrixRow row, int score) {
		return properties.getProperty(row.name() + "." + score);
	}

	public static Color[] getColors(Display display) {
		Color[] colors = new Color[5];
		colors[0] = Colors.getColor(210, 230, 185);
		colors[1] = Colors.getColor(233, 243, 199);
		colors[2] = Colors.getColor(251, 242, 209);
		colors[3] = Colors.getColor(246, 207, 191);
		colors[4] = Colors.getColor(246, 182, 174);
		return colors;
	}

	public static Map<PedigreeMatrixRow, Integer> defaultSelection() {
		Map<PedigreeMatrixRow, Integer> map = new HashMap<>();
		for (PedigreeMatrixRow row : PedigreeMatrixRow.values())
			map.put(row, 5);
		return map;
	}

}
