package org.openlca.app.editors.graphical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

/**
 * A helper class for finding process links in a product system. We do not
 * always use the ProcessLinkSearchMap to find a process link in the graphical
 * editor, because building this map could be more expensive than just searching
 * for the links of a single process.
 */
public class ProcessLinks {

	/**
	 * Get the links where the given process is either the recipient or the
	 * provider.
	 */
	public static List<ProcessLink> getAll(ProductSystem system, long processId) {
		if (system == null)
			return Collections.emptyList();
		List<ProcessLink> links = new ArrayList<>();
		for (ProcessLink link : system.getProcessLinks()) {
			if (link.getProviderId() == processId
					|| link.getRecipientId() == processId)
				links.add(link);
		}
		return links;
	}

	/** Get the links where the given process is the recipient. */
	public static List<ProcessLink> getIncoming(ProductSystem system,
			long processId) {
		return getLinks(system, processId, true);
	}

	/** Get the links where the given process is the provider. */
	public static List<ProcessLink> getOutgoing(ProductSystem system,
			long processId) {
		return getLinks(system, processId, false);
	}

	private static List<ProcessLink> getLinks(ProductSystem system,
			long processId, boolean incoming) {
		if (system == null)
			return Collections.emptyList();
		List<ProcessLink> links = new ArrayList<>();
		for (ProcessLink link : system.getProcessLinks()) {
			if (link.getRecipientId() == processId && incoming)
				links.add(link);
			else if (link.getProviderId() == processId && !incoming)
				links.add(link);
		}
		return links;
	}

}
