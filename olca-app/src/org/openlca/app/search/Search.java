package org.openlca.app.search;

import org.openlca.app.navigation.ModelTypeOrder;
import org.openlca.app.util.Labels;
import org.openlca.core.database.Daos;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class Search implements Runnable {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final IDatabase db;

	private ModelType typeFilter;
	private final String rawTerm;
	private final List<String> words = new ArrayList<>();
	private final List<String> tags = new ArrayList<>();
	private final List<Descriptor> result = new ArrayList<>();

	Search(IDatabase db, String term) {
		this.db = db;
		this.rawTerm = term == null ? "" : term.toLowerCase().strip();
		var parts = rawTerm.split("\\s+");
		for (var part : parts) {
			if (Strings.nullOrEmpty(part))
				continue;
			if (part.startsWith("#")) {
				if (part.length() > 1) {
					tags.add(part.substring(1));
				}
			} else {
				words.add(part);
			}
		}
	}

	Search withTypeFilter(ModelType type) {
		this.typeFilter = type;
		return this;
	}

	public List<Descriptor> getResult() {
		return result;
	}

	@Override
	public void run() {
		result.clear();
		if (rawTerm.isEmpty())
			return;

		log.trace("run search with term {}", rawTerm);
		var types = typeFilter == null
				? ModelTypeOrder.getOrderedTypes()
				: new ModelType[]{typeFilter};
		var matches = new ArrayList<Match>();
		for (var type : types) {
			var all = type == ModelType.PARAMETER
					? new ParameterDao(db).getGlobalDescriptors()
					: Daos.root(db, type).getDescriptors();
			for (var d : all) {
				var match = Match.of(d, this);
				if (!match.isEmpty()) {
					matches.add(match);
				}
			}
		}

		matches.stream()
				.sorted((m1, m2) -> {
					int c = Double.compare(m2.factor, m1.factor);
					return c == 0
							? Strings.compare(
							Labels.name(m1.descriptor),
							Labels.name(m2.descriptor))
							: c;
				})
				.map(Match::descriptor)
				.forEach(result::add);

		log.trace("{} results found", result.size());
	}

	private record Match(Descriptor descriptor, double factor) {

		private static final Match _empty = new Match(null, 0);

		boolean isEmpty() {
			return descriptor == null;
		}

		static Match of(Descriptor d, Search s) {
			if (d == null)
				return _empty;

			// matching ref-ids
			if (s.words.size() == 1
					&& d.refId != null
					&& d.refId.equalsIgnoreCase(s.words.get(0)))
				return new Match(d, 1e7);

			// filter by tags
			if (!s.tags.isEmpty()) {
				if (Strings.nullOrEmpty(d.tags))
					return _empty;

				var tags = Arrays.stream(d.tags.split(","))
						.map(tag -> tag.strip().toLowerCase())
						.filter(Strings::notEmpty)
						.collect(Collectors.toSet());
				for (var tag : s.tags) {
					if (!tags.contains(tag))
						return _empty;
				}
			}

			double factor = 0;
			for (var word : s.words) {
				var nameMatch = wordMatch(Labels.name(d), word);
				var tagMatch = wordMatch(d.tags, word);
				if (nameMatch == 0 && tagMatch == 0)
					return _empty;
				factor += nameMatch + tagMatch ;
			}

			return new Match(d, factor);
		}

		private static double wordMatch(String phrase, String word) {
			if (Strings.nullOrEmpty(phrase))
				return 0;
			double pos = phrase.toLowerCase().indexOf(word);
			return pos >= 0
					? word.length() * Math.sqrt(1.0 / (42.0 + pos))
					: 0;
		}
	}
}
