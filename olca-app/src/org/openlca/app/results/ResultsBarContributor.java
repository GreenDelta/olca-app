package org.openlca.app.results;

import org.eclipse.jface.action.IToolBarManager;
import org.openlca.app.results.analysis.sankey.actions.SankeyBarContributor;
import org.openlca.app.tools.graphics.EditorActionBarContributor;
import org.openlca.app.tools.graphics.MultiPageSubActionBars;

/**
 * A special implementation of a
 * <code>MultiPageEditorActionBarContributor</code> to switch between
 * action bar contributions for product system editor pages.
 */
public class ResultsBarContributor extends EditorActionBarContributor {

	@Override
	public MultiPageSubActionBars getNewSubActionBars() {
		return new MultiPageSubActionBars(getPage(),
				getActionBars2(),
				new SankeyBarContributor(),
				"org.openlca.app.results.analysis.sankey.actions" +
						".SankeyBarContributor");
	}

	@Override
	public void contributeToToolBar(IToolBarManager toolbar) {
		toolbar.add(new ExcelExportAction());
	}

}
