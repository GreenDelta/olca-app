package org.openlca.app.results;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.openlca.app.util.Labels;
import org.openlca.commons.Strings;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.results.ResultItemOrder;

/// Utility functions for the result and simulation editors.
public final class ResultUtil {

	private ResultUtil() {
	}

	/// Sorts the items of the given result order by their names.
	public static void sort(ResultItemOrder items) {
		if (items == null)
			return;

		items.techFlows().sort((tf1, tf2) -> {
			int c = Strings.compareIgnoreCase(
				Labels.name(tf1.provider()), Labels.name(tf2.provider()));
			if (c != 0)
				return c;
			return Strings.compareIgnoreCase(
				Labels.name(tf1.flow()), Labels.name(tf2.flow()));
		});

		if (items.hasEnviFlows()) {
			items.enviFlows().sort((ef1, ef2) -> Strings.compareIgnoreCase(
				Labels.name(ef1), Labels.name(ef2)));
		}

		if (items.hasImpacts()) {
			items.impacts().sort(
				(i1, i2) -> Strings.compareIgnoreCase(i1.name, i2.name));
		}
	}

	/// Returns a short description of the given calculation setup, e.g. for
	/// the title of an editor: the calculation target, the target amount, and,
	/// if present, the impact assessment method, the allocation method, and
	/// the parameter set (see `parameterSetNameOf`).
	public static String nameOf(CalculationSetup setup) {
		if (setup == null)
			return "";
		var name = Labels.name(setup.target())
			+ "; " + amountText(setup.amount())
			+ " " + Labels.name(setup.unit());
		if (setup.impactMethod() != null) {
			name += "; " + Labels.name(setup.impactMethod());
		}
		if (setup.allocation() != null) {
			name += switch (setup.allocation()) {
				case CAUSAL -> "; causal alloc.";
				case ECONOMIC -> "; economic alloc.";
				case PHYSICAL -> "; physical alloc.";
				case USE_DEFAULT -> "; default alloc.";
				default -> "";
			};
		}
		var paramSet = parameterSetNameOf(setup);
		if (paramSet != null) {
			name += "; " + paramSet;
		}
		return name;
	}

	/// Returns the name of the parameter set of the given setup, but only if
	/// the product system has more than one parameter set. Otherwise, there
	/// was no choice: the calculation wizard then automatically uses the only
	/// (baseline) parameter set and showing its name would be just noise.
	/// Returns `null` if no parameter set name should be shown.
	public static String parameterSetNameOf(CalculationSetup setup) {
		if (setup == null
			|| !setup.hasProductSystem()
			|| Strings.isBlank(setup.parameterSetName()))
			return null;
		var system = setup.productSystem();
		return system != null && system.parameterSets.size() > 1
			? setup.parameterSetName()
			: null;
	}

	/// Formats the absolute value of the given target amount without
	/// unnecessary decimal places, e.g. `1`, `100`, or `0.25`. Very small
	/// amounts are shown in scientific notation.
	public static String amountText(double amount) {
		var abs = Math.abs(amount);
		if (abs != 0 && abs < 1e-4)
			return Double.toString(abs);
		var format = new DecimalFormat(
			"0.######", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		return format.format(abs);
	}
}
