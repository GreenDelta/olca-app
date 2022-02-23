package org.openlca.app.editors.projects.results;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import org.openlca.app.util.Labels;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.util.Pair;
import org.openlca.util.Strings;

/**
 * Contains the data of a cell entry in the contribution table.
 */
class Contribution {

	/**
	 * The process or product system of the result contribution. This is
	 * {@code null} when this is the "rest" ("others") contribution.
	 */
	final RootDescriptor process;

	/**
	 * The amount of the result contribution.
	 */
	final double amount;

	/**
	 * {@code true} if this is the "rest" contribution: if there are n processes
	 * and sub-systems in the system and the user selects k results, this contains
	 * the sum of the n-k other contributions.
	 */
	final boolean isRest;

	Contribution(RootDescriptor process, double amount) {
		this.process = process;
		this.amount = amount;
		this.isRest = process == null;
	}

	static Contribution restOf(double amount) {
		return new Contribution(null, amount);
	}

	static List<Contribution> select(
		List<Contribution> results, int count, String query) {
		if (results == null || results.isEmpty())
			return Collections.emptyList();
		return Strings.nullOrEmpty(query)
			? selectByAmount(results, count)
			: selectByQuery(results, count, query);
	}

	private static List<Contribution> selectByAmount(
		List<Contribution> results, int count) {
		results.sort(Comparator.comparingDouble(c -> -c.amount));
		var selected = new ArrayList<>(
			results.subList(0, Math.min(results.size(), count)));
		if (results.size() > count) {
			var rest = results.subList(count, results.size())
				.stream()
				.mapToDouble(cell -> cell.amount)
				.sum();
			if (rest != 0) {
				selected.add(Contribution.restOf(rest));
			}
		}
		return selected;
	}

	private static List<Contribution> selectByQuery(
		List<Contribution> results, int count, String query) {

		// calculate match factors: the smaller the value
		// the higher a result will be ranked. 0 means no match.
		var terms = Arrays.stream(query.split(" "))
			.map(s -> s.trim().toLowerCase())
			.filter(t -> !Strings.nullOrEmpty(t))
			.collect(Collectors.toSet());
		ToDoubleFunction<String> matcher = s -> {
			if (s == null)
				return 0;
			var f = s.toLowerCase();
			double i = 0;
			for (var term : terms) {
				double idx = f.indexOf(term);
				if (idx < 0)
					return 0;
				i -= term.length() / (idx + 1.0);
			}
			return i;
		};

		var sorted = results.stream()
			.map(c -> {
				var name = Labels.name(c.process);
				var factor = matcher.applyAsDouble(name);
				return Pair.of(c, factor);
			})
			.sorted((p1, p2) -> {
				var c = Double.compare(p1.second, p2.second);
				if (c != 0)
					return c;
				return Double.compare(p2.first.amount, p1.first.amount);
			})
			.collect(Collectors.toList());

		double rest = 0;
		var selected = new ArrayList<Contribution>(
			Math.min(sorted.size(), count + 1));
		for (int i = 0; i < sorted.size(); i++) {
			var next = sorted.get(i);
			if (i >= count || next.second == 0) {
				rest += next.first.amount;
			} else {
				selected.add(next.first);
			}
		}
		if (rest != 0) {
			selected.add(restOf(rest));
		}
		return selected;
	}
}
