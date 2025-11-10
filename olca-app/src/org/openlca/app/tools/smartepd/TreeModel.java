package org.openlca.app.tools.smartepd;

import java.util.ArrayList;
import java.util.List;

import org.openlca.commons.Res;
import org.openlca.commons.Strings;
import org.openlca.io.smartepd.SmartEpd;
import org.openlca.io.smartepd.SmartEpdClient;
import org.openlca.io.smartepd.SmartProject;

record TreeModel(List<ProjectNode> projectNodes) {

	static Res<TreeModel> fetch(SmartEpdClient client) {
		if (client == null)
			return Res.error("no client provided");
		var res = client.getProjects();
		if (res.isError())
			return res.wrapError("failed to fetch projects");

		var projects = res.value();
		var nodes = new ArrayList<ProjectNode>(projects.size());

		for (var project : projects) {
			var node = new ProjectNode(project, new ArrayList<>());
			nodes.add(node);
			var epdRes = client.getEpds(project.id());
			if (epdRes.isError())
				continue;
			var epds = epdRes.value();
			for (var epd : epds) {
				node.epdNodes().add(new EpdNode(node, epd));
			}
			node.epdNodes.sort((a, b) -> Strings.compareIgnoreCase(a.name(), b.name()));
		}
		nodes.sort((a, b) -> Strings.compareIgnoreCase(a.name(), b.name()));
		return Res.ok(new TreeModel(nodes));
	}

	interface Node {
		String id();

		String name();
	}

	record ProjectNode(
			SmartProject project, List<EpdNode> epdNodes
	) implements Node {

		@Override
		public String id() {
			return project.id();
		}

		@Override
		public String name() {
			return project.name();
		}

		@Override
		public int hashCode() {
			return project != null
					? project.hashCode()
					: System.identityHashCode(this);
		}
	}

	record EpdNode(ProjectNode parent, SmartEpd epd) implements Node {

		@Override
		public String id() {
			return epd.id();
		}

		@Override
		public String name() {
			return epd.productName();
		}

		@Override
		public int hashCode() {
			return epd != null
					? epd.hashCode()
					: System.identityHashCode(this);
		}
	}
}
