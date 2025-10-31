package org.openlca.app.tools.mapping.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openlca.app.M;
import org.openlca.app.tools.mapping.model.FlowProvider;
import org.openlca.core.io.maps.FlowMap;
import org.openlca.core.io.maps.FlowMapEntry;
import org.openlca.core.io.maps.FlowRef;
import org.openlca.core.io.maps.MappingStatus;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Try to find matching flows in a target system for the flows in a source
 * system that do not have a mapping assigned.
 */
public class Generator implements Runnable {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final FlowProvider sourceSystem;
	private final FlowProvider targetSystem;
	private final FlowMap mapping;

	public Generator(FlowProvider sourceSystem, FlowProvider targetSystem,
	                 FlowMap mapping) {
		this.sourceSystem = sourceSystem;
		this.targetSystem = targetSystem;
		this.mapping = mapping;
	}

	@Override
	public void run() {
		try {
			log.info("generate mappings {} -> {}", sourceSystem, targetSystem);
			log.info("load source flows");
			// we only generate mappings for flows that are not already mapped
			var sourceFlows = getCandidateFlows();
			if (sourceFlows.isEmpty()) {
				log.info("found no unmapped source flows");
				return;
			}

			log.info("match unmapped flows");
			var matcher = new Matcher(targetSystem);
			for (var sourceFlow : sourceFlows) {
				var source = sourceFlow.copy();
				source.status = MappingStatus.ok();
				FlowRef matched = matcher.find(source);
				FlowRef target = null;
				if (matched != null) {
					target = matched.copy();
					target.status = getStatus(source, target);
				}
				mapping.entries.add(new FlowMapEntry(source, target, 1.0));
			}

		} catch (Exception e) {
			log.error("Generation of flow mappings failed", e);
		}
	}

	private MappingStatus getStatus(FlowRef source, FlowRef target) {
		if (differentUnits(source, target))
			return MappingStatus.warn(M.DifferentUnits);

		if (!Objects.equals(source.flow.refId, target.flow.refId))
			return MappingStatus.warn(M.MatchedByFlowAttributes);

		if (target.provider != null)
			return MappingStatus.warn(M.ProviderMatchedByAttributes);

		return MappingStatus.ok(M.MatchedByFlowIds);
	}

	private boolean differentUnits(FlowRef source, FlowRef target) {
		if (source.unit == null && target.unit == null)
			return false;
		if (source.unit == null || target.unit == null)
			return true;
		var s = source.unit;
		var t = target.unit;
		boolean equal = Strings.isNotBlank(s.refId) && Strings.isNotBlank(t.refId)
			? Objects.equals(s.refId, t.refId)
			: Objects.equals(s.name, t.name);
		return !equal;
	}

	/**
	 * Get the unmapped flows of the source system that could be mapped to the
	 * flows of the target system.
	 */
	private List<FlowRef> getCandidateFlows() {
		var candidates = new ArrayList<FlowRef>();
		var index = mapping.index();
		for (var f : sourceSystem.getFlowRefs()) {
			if (f.flow == null || f.flow.refId == null)
				continue;
			var entry = index.get(f.flow.refId);
			if (entry == null || entry.targetFlow() == null) {
				candidates.add(f);
			}
		}
		return candidates;
	}
}
