package org.openlca.app.editors.dq_systems;

import org.eclipse.swt.graphics.Color;
import org.openlca.app.util.Colors;

public class DQColors {

	public static Color get(int index, int total) {
		if (index == 1)
			return Colors.get(125, 250, 125);
		if (index == total)
			return Colors.get(250, 125, 125);
		int median = total / 2 + 1;
		if (index == median)
			return Colors.get(250, 250, 125);
		if (index < median) {
			int divisor = median - 1;
			int factor = index - 1;
			return Colors.get(125 + (125 * factor / divisor), 250, 125);
		}
		int divisor = median - 1;
		int factor = index - median;
		return Colors.get(250, 250 - (125 * factor / divisor), 125);
	}
}
