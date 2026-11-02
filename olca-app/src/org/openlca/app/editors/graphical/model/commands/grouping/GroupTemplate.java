package org.openlca.app.editors.graphical.model.commands.grouping;

import java.util.List;

import org.openlca.core.model.AnalysisGroup;

/// Predefined sets of analysis groups, with names and colors, that can be
/// added to a product system in the [GroupDialog].
enum GroupTemplate {

	LCA("Life Cycle Stages", List.of(
		g("0 Total", "#D6B82F"),
		g("1 Raw Materials", "#A67C52"),
		g("2 Manufacturing", "#D98E36"),
		g("3 Distribution", "#3996A8"),
		g("4 Use", "#287C70"),
		g("5 Disposal", "#737B83"),
		g("6 Recycling", "#55A66E"))),

	EN_15804("EN 15804 Modules", List.of(
		g("A1-A3", "#311B92"),
		g("A1", "#673AB7"),
		g("A2", "#5E35B1"),
		g("A3", "#512DA8"),

		g("A4-A5", "#1A237E"),
		g("A4", "#3F51B5"),
		g("A5", "#3949AB"),

		g("B1-B7", "#004D40"),
		g("B1", "#80CBC4"),
		g("B2", "#4DB6AC"),
		g("B3", "#26C6DA"),
		g("B4", "#009688"),
		g("B5", "#00897B"),
		g("B6", "#00796B"),
		g("B7", "#00695C"),

		g("C1-C4", "#3E2723"),
		g("C1", "#795548"),
		g("C2", "#6D4C41"),
		g("C3", "#5D4037"),
		g("C4", "#4E342E"),

		g("D", "#455A64"))),

	KBOB("KBOB Groups", List.of(
		g("0 Total", "#D6B82F"),
		g("1 Production", "#3996A8"),
		g("2 Disposal", "#737B83")));

	final String label;
	final List<Entry> entries;

	GroupTemplate(String label, List<Entry> entries) {
		this.label = label;
		this.entries = entries;
	}

	record Entry(String name, String color) {
		AnalysisGroup toGroup() {
			var g = new AnalysisGroup();
			g.name = name;
			g.color = color;
			return g;
		}
	}

	private static Entry g(String name, String color) {
		return new Entry(name, color);
	}
}
