package org.openlca.app.results;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IEditorPart;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.results.analysis.sankey.SankeyEditor;
import org.openlca.app.results.analysis.sankey.actions.SankeyBarContributor;
import org.openlca.app.tools.graphics.EditorActionBarContributor;
import org.openlca.app.tools.graphics.MultiPageSubActionBars;

/**
 * A special implementation of a
 * <code>MultiPageEditorActionBarContributor</code> to switch between
 * action bar contributions for product system editor pages.
 */
public class ResultsBarContributor extends EditorActionBarContributor {

	private SankeyEditor editor;

	@Override
	public void setActivePage(IEditorPart activePage) {
		if (activePage instanceof SankeyEditor sankeyEditor)
			this.editor = sankeyEditor;
		super.setActivePage(activePage);
	}
	@Override
	public MultiPageSubActionBars getNewSubActionBars() {
		return new MultiPageSubActionBars(getPage(),
				getActionBars2(),
				new SankeyBarContributor(editor),
				"org.openlca.app.results.analysis.sankey.actions" +
						".SankeyBarContributor");
	}

	@Override
	public void contributeToToolBar(IToolBarManager toolbar) {
		toolbar.add(new ExcelExportAction());
	}

}
