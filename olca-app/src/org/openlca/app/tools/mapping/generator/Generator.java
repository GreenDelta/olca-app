package org.openlca.app.tools.mapping.generator;

import java.util.List;
import java.util.stream.Collectors;

import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.io.maps.FlowMap;
import org.openlca.io.maps.FlowMapEntry;
import org.openlca.io.maps.FlowRef;
import org.openlca.io.maps.Status;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

/**
 * Try to find matching flows in a target system for the flows in a source
 * system that do not have a mapping assigned.
 */
public class Generator implements Runnable {

	private final Logger log = LoggerFactory.getLogger(getClass());
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

			log.info("create target flow matcher");
			Matcher matcher = new Matcher(targetSystem);
			for (FlowRef sflow : sourceFlows) {
				FlowMapEntry e = new FlowMapEntry();
				e.sourceFlow = sflow.clone();
				e.sourceFlow.status = Status.ok();
				FlowRef tflow = matcher.find(sflow);
				if (tflow != null) {
					tflow = tflow.clone();
					e.targetFlow = tflow;
					if (Objects.equal(sflow.flow.refId, tflow.flow.refId)) {
						tflow.status = Status.ok("matched by flow IDs");
					} else {
						tflow.status = Status.warn("matched by flow attributes");
					}
					if (!sameUnits(sflow, tflow)) {
						tflow.status = Status.warn("different units");
					} else if (tflow.status.isOk() && tflow.provider != null) {
						tflow.status = Status.warn("provider matched by attributes");
					}
				}
				e.factor = 1.0;
				mapping.entries.add(e);
			}

		} catch (Exception e) {
			log.error("Generation of flow mappings failed", e);
		}
	}

	private boolean sameUnits(FlowRef sflow, FlowRef tflow) {
		if (sflow == null || tflow == null)
			return false;
		if (sflow.unit == null || tflow.unit == null)
			return false;
		return Strings.nullOrEqual(sflow.unit.name, tflow.unit.name);
	}
}
