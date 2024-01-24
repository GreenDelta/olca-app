package org.openlca.app.results.slca.ui;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.openlca.app.util.Colors;
import org.openlca.core.model.RiskLevel;

class TreeGrid {

	private static final int OFFSET = 3;

	static RiskLevel levelOf(int col) {
		return switch (col - OFFSET) {
			case 0 -> RiskLevel.HIGH_OPPORTUNITY;
			case 1 -> RiskLevel.MEDIUM_OPPORTUNITY;
			case 2 -> RiskLevel.LOW_OPPORTUNITY;
			case 3 -> RiskLevel.NO_OPPORTUNITY;
			case 4 -> RiskLevel.NO_RISK;
			case 5 -> RiskLevel.VERY_LOW_RISK;
			case 6 -> RiskLevel.LOW_RISK;
			case 7 -> RiskLevel.MEDIUM_RISK;
			case 8 -> RiskLevel.HIGH_RISK;
			case 9 -> RiskLevel.VERY_HIGH_RISK;
			case 10 -> RiskLevel.NO_DATA;
			case 11 -> RiskLevel.NOT_APPLICABLE;
			default -> null;
		};
	}

	static String headerOf(RiskLevel level) {
		if (level == null)
			return "?";
		return switch (level) {
			case HIGH_OPPORTUNITY -> "HO";
			case MEDIUM_OPPORTUNITY -> "MO";
			case LOW_OPPORTUNITY -> "LO";
			case NO_OPPORTUNITY -> "NOP";
			case NO_RISK -> "NOR";
			case VERY_LOW_RISK -> "VLR";
			case LOW_RISK -> "LR";
			case MEDIUM_RISK -> "MR";
			case HIGH_RISK -> "HR";
			case VERY_HIGH_RISK -> "VHR";
			case NO_DATA -> "ND";
			case NOT_APPLICABLE -> "NA";
		};
	}

	static int columnOf(RiskLevel level) {
		if (level == null)
			return -1;
		return OFFSET + switch (level) {
			case HIGH_OPPORTUNITY -> 0;
			case MEDIUM_OPPORTUNITY -> 1;
			case LOW_OPPORTUNITY -> 2;
			case NO_OPPORTUNITY -> 3;
			case NO_RISK -> 4;
			case VERY_LOW_RISK -> 5;
			case LOW_RISK -> 6;
			case MEDIUM_RISK -> 7;
			case HIGH_RISK -> 8;
			case VERY_HIGH_RISK -> 9;
			case NO_DATA -> 10;
			case NOT_APPLICABLE -> 11;
		};
	}

	static Color colorOf(RiskLevel level) {
		if (level == null)
			return colorOf(RiskLevel.NOT_APPLICABLE);
		var rgb = switch (level) {
			/*
			 case HIGH_OPPORTUNITY -> new RGB(0, 105, 92);
			 case MEDIUM_OPPORTUNITY -> new RGB(38, 166, 154);
			 case LOW_OPPORTUNITY -> new RGB(128, 203, 196);
			*/
			case HIGH_OPPORTUNITY,
					MEDIUM_OPPORTUNITY,
					LOW_OPPORTUNITY -> new RGB(38, 166, 154);
			/*
			case VERY_LOW_RISK -> new RGB(244, 143, 177);
			case LOW_RISK -> new RGB(240, 98, 146);
			case MEDIUM_RISK -> new RGB(233, 30, 99);
			case HIGH_RISK -> new RGB(194, 24, 91);
			case VERY_HIGH_RISK -> new RGB(136, 14, 79);
			*/
			case VERY_LOW_RISK,
					LOW_RISK,
					MEDIUM_RISK,
					HIGH_RISK,
					VERY_HIGH_RISK -> new RGB(240, 98, 146);
			default -> new RGB(96, 125, 139);
		};
		return Colors.get(rgb);
	}
}
