package org.openlca.app.results.contributions;

import java.io.File;

import org.openlca.core.results.UpstreamTree;

class UpstreamTreeExport {

	/**
	 * The maximum number of levels that should be exported. A value < 0 means
	 * unlimited. In this case reasonable recursion limits are required if the
	 * underlying product system has cycles.
	 */
	public int maxDepth = 10;

	/**
	 * In addition to the maximum tree depth, this parameter describes the minimum
	 * upstream contribution of a node in the tree. A value < 0 also means there is
	 * no minimum contribution.
	 */
	public double minContribution = 1e-9;

	/**
	 * When the max. tree depth is unlimited and the underlying product system has
	 * cycles, this parameter indicates how often a process can occur in a tree
	 * path. It defines the maximum number of expansions of a loop in a tree path.
	 */
	public int maxRecursionDepth = 10;

	private final File file;
	private final UpstreamTree tree;

	UpstreamTreeExport(File file, UpstreamTree tree) {
		this.file = file;
		this.tree = tree;
	}

}
