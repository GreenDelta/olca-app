package org.openlca.app.tools.mapping.generator;

import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.io.maps.FlowMap;
import org.openlca.io.maps.FlowMapEntry;
import org.openlca.io.maps.FlowRef;
import org.openlca.io.maps.MappingStatus;
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
			var sourceFlows = sourceSystem.getFlowRefs()
				.stream().filter(f -> {
					if (f.flow == null || f.flow.refId == null)
						return false;
					var e = mapping.getEntry(f.flow.refId);
					return e == null || e.targetFlow() == null;
				}).toList();
			if (sourceFlows.isEmpty()) {
				log.info("found no unmapped source flows");
				return;
			}

			log.info("create target flow matcher");
			Matcher matcher = new Matcher(targetSystem);
			for (FlowRef sflow : sourceFlows) {

				var source = sflow.copy();
				source.status = MappingStatus.ok();

				FlowRef matched = matcher.find(sflow);
				FlowRef target = null;
				if (matched != null) {
					target = matched.copy();
					if (Objects.equal(sflow.flow.refId, target.flow.refId)) {
						target.status = MappingStatus.ok("matched by flow IDs");
					} else {
						target.status = MappingStatus.warn("matched by flow attributes");
					}
					if (!sameUnits(source, target)) {
						target.status = MappingStatus.warn("different units");
					} else if (matched.status.isOk() && matched.provider != null) {
						target.status = MappingStatus.warn("provider matched by attributes");
					}
				}

				mapping.entries.add(new FlowMapEntry(source, target, 1.0));
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
