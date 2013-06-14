package org.openlca.core.editors.process;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.openlca.core.model.PedigreeMatrixRow;
import org.openlca.ui.Colors;
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

	public static double getFactor(PedigreeMatrixRow row, int score) {
		switch (row) {
		case RELIABILITY:
			switch (score) {
			case 1:
				return 1;
			case 2:
				return 1.05;
			case 3:
				return 1.1;
			case 4:
				return 1.2;
			case 5:
				return 1.5;
			}
		case COMPLETENESS:
			switch (score) {
			case 1:
				return 1;
			case 2:
				return 1.02;
			case 3:
				return 1.05;
			case 4:
				return 1.1;
			case 5:
				return 1.2;
			}
		case TIME:
			switch (score) {
			case 1:
				return 1;
			case 2:
				return 1.03;
			case 3:
				return 1.1;
			case 4:
				return 1.2;
			case 5:
				return 1.5;
			}
		case GEOGRAPHY:
			switch (score) {
			case 1:
				return 1d;
			case 2:
				return 1.01;
			case 3:
				return 1.02;
			case 4:
				return 1.02;
			case 5:
				return 1.1;
			}
		case TECHNOLOGY:
			switch (score) {
			case 1:
				return 1d;
			case 2:
				return 1.1;
			case 3:
				return 1.2;
			case 4:
				return 1.5;
			case 5:
				return 2.0;
			}
		}
		throw new IllegalArgumentException("Row = " + row + " and score = "
				+ score + " is not a valid input");
	}

	public static double calculateSigmaG(
			Map<PedigreeMatrixRow, Integer> selection, double baseUncertainty) {
		double tempSum = 0;
		for (PedigreeMatrixRow row : PedigreeMatrixRow.values()) {
			Integer selectedScore = selection.get(row);
			int score = selectedScore == null ? 5 : selectedScore.intValue();
			double factor = getFactor(row, score);
			tempSum += Math.pow(Math.log(factor), 2);
		}
		tempSum += Math.pow(Math.log(baseUncertainty), 2);
		return Math.exp(Math.sqrt(tempSum));
	}

	public static Map<PedigreeMatrixRow, Integer> defaultSelection() {
		Map<PedigreeMatrixRow, Integer> map = new HashMap<>();
		for (PedigreeMatrixRow row : PedigreeMatrixRow.values())
			map.put(row, 5);
		return map;
	}

}
