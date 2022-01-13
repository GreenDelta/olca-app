package org.openlca.app.tools.mapping.generator;

import java.util.Map;
import java.util.stream.Collectors;

import org.openlca.app.tools.mapping.model.DBProvider;
import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.app.util.Labels;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.LocationDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.maps.FlowRef;
import org.openlca.text.CompartmentStemmer;
import org.openlca.text.PhraseParser;
import org.openlca.text.PhraseSimilarity;
import org.openlca.text.WordBuffer;
import org.openlca.util.Categories;

class Matcher {

	private final IDatabase db;
	private final Map<String, FlowRef> targetFlows;

	final CompartmentStemmer compartmentStemmer;

	private final PhraseSimilarity similarity;
	private final PhraseParser parser;
	private final WordBuffer phrase1;
	private final WordBuffer phrase2;


	// helper structures for collecting provider information
	private Categories.PathBuilder categories;
	private Map<Long, String> locations;

	Matcher(IProvider targetSystem) {
		db = targetSystem instanceof DBProvider
			? ((DBProvider) targetSystem).db()
			: null;
		this.targetFlows = targetSystem.getFlowRefs().stream()
			.filter(f -> f.flow != null && f.flow.refId != null)
			.collect(Collectors.toMap(f -> f.flow.refId, f -> f));

		this.compartmentStemmer = new CompartmentStemmer();
		this.similarity = new PhraseSimilarity();
		this.parser = new PhraseParser();
		this.phrase1 = new WordBuffer();
		this.phrase2 = new WordBuffer();
	}

	double similarityOf(String s1, String s2) {
		if (s1 == null || s2 == null)
			return 0;
		parser.parseInto(phrase1, s1);
		parser.parseInto(phrase2, s2);
		return similarity.get(phrase1, phrase2);
	}

	FlowRef find(FlowRef s) {
		if (s == null
			|| s.flow == null
			|| s.flow.refId == null)
			return null;

		// test whether there is a direct match based on the reference IDs
		var t = targetFlows.get(s.flow.refId);
		if (t != null) {
			checkAddProvider(s, t);
			return t;
		}

		var score = Score.noMatch();
		for (var candidate : targetFlows.values()) {
			var nextScore = Score.compute(this, s, candidate);
			if (nextScore.betterThan(score)) {
				score = nextScore;
				t = candidate;
			}
		}

		if (t == null)
			return null;

		checkAddProvider(s, t);
		return t;
	}

	private void checkAddProvider(FlowRef s, FlowRef t) {
		if (db == null || t == null || t.flow == null)
			return;
		if (t.flow.flowType == FlowType.ELEMENTARY_FLOW)
			return;
		ProcessDescriptor prov = findProvider(db, s, t);
		if (prov == null)
			return;
		t.provider = prov;
		if (categories == null) {
			categories = Categories.pathsOf(db);
		}
		t.providerCategory = categories.pathOf(prov.category);
		if (locations == null) {
			locations = new LocationDao(db).getCodes();
		}
		if (prov.location != null) {
			t.providerLocation = locations.get(prov.location);
		}
	}

	private ProcessDescriptor findProvider(IDatabase db, FlowRef s,	FlowRef t) {

		long tid = t.flow.id;
		var processIDs = t.flow.flowType == FlowType.WASTE_FLOW
			? new FlowDao(db).getWhereInput(tid)
			: new FlowDao(db).getWhereOutput(tid);
		if (processIDs.isEmpty())
			return null;

		var candidates = new ProcessDao(db).getDescriptors(processIDs);
		if (candidates.isEmpty())
			return null;
		if (candidates.size() == 1)
			return candidates.get(0);

		ProcessDescriptor cand = null;
		double score = 0.0;
		parser.parseInto(phrase1, s.flow.name);
		for (var d : candidates) {
			// include possible location codes; location codes are
			// often added to process names
			var processName = Labels.name(d);
			parser.parseInto(phrase2, processName);
			double sim = similarity.get(phrase1, phrase2);
			if (cand == null || sim > score) {
				cand = d;
				score = sim;
			}
		}
		return cand;
	}
}
