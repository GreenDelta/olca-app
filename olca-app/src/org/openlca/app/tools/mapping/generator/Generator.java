package org.openlca.app.tools.mapping.generator;

import java.util.List;
import java.util.stream.Collectors;

import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.io.maps.FlowMap;
import org.openlca.io.maps.FlowMapEntry;
import org.openlca.io.maps.FlowRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Try to find matching flows in a target system for the flows in a source
 * system that do not have a mapping assigned.
 */
public class Generator implements Runnable {

	private Logger log = LoggerFactory.getLogger(getClass());
	private final IProvider sourceSystem;
	private final IProvider targetSystem;
	private final FlowMap mapping;

	public Generator(IProvider sourceSystem, IProvider targetSystem,
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
			List<FlowRef> sourceFlows = sourceSystem.getFlowRefs()
					.stream().filter(f -> {
						if (f.flow == null || f.flow.refId == null)
							return false;
						FlowMapEntry e = mapping.getEntry(f.flow.refId);
						return e == null || e.targetFlow == null;
					}).collect(Collectors.toList());
			if (sourceFlows.isEmpty()) {
				log.info("found no unmapped source flows");
				return;
			}

			log.info("load target flows");
			List<FlowRef> targetFlows = targetSystem.getFlowRefs();
			Matcher matcher = new Matcher(targetFlows);

			for (FlowRef sflow : sourceFlows) {
				FlowMapEntry e = new FlowMapEntry();
				e.sourceFlow = sflow;
				e.targetFlow = matcher.find(sflow);
				e.factor = 1.0; // TODO: try to find a factor
				// TODO: find providers for product flows
				mapping.entries.add(e);
			}

		} catch (Exception e) {
			log.error("Generation of flow mappings failed", e);
		}
	}
}
