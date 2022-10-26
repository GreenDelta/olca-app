package org.openlca.app.results;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IActionBars2;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.openlca.app.results.analysis.sankey.actions.SankeyBarContributor;
import org.openlca.app.results.analysis.sankey.SankeyEditor;
import org.openlca.app.tools.graphics.MultiPageSubActionBars;

/**
 * A special implementation of a
 * <code>MultiPageEditorActionBarContributor</code> to switch between
 * action bar contributions for product system editor pages.
 */
public class ResultsBarContributor extends
		MultiPageEditorActionBarContributor {

	private IActionBars2 actionBars2;
	private MultiPageSubActionBars graphicalSubActionBars;
	private MultiPageSubActionBars activeEditorActionBars;

	@Override
	public void init(IActionBars bars) {
		super.init(bars);
		assert bars instanceof IActionBars2;
		actionBars2 = (IActionBars2) bars;
	}

	@Override
	public void contributeToToolBar(IToolBarManager toolbar) {
		toolbar.add(new ExcelExportAction());
	}

	@Override
	public void setActivePage(IEditorPart activePage) {
		setActiveActionBars(null, activePage);
		if (activePage instanceof SankeyEditor)
				setActiveActionBars(getGraphicalSubActionBars(), activePage);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (graphicalSubActionBars != null) {
			graphicalSubActionBars.dispose();
			graphicalSubActionBars = null;
		}
	}

	/**
	 * Switches the active action bars.
	 */
	private void setActiveActionBars(MultiPageSubActionBars actionBars,
																	 IEditorPart activeEditor) {
		if (activeEditorActionBars != null
				&& activeEditorActionBars != actionBars) {
			activeEditorActionBars.deactivate();
		}
		activeEditorActionBars = actionBars;
		if (activeEditorActionBars != null) {
			activeEditorActionBars.setEditorPart(activeEditor);
			activeEditorActionBars.activate();
		}
	}

	/**
	 * @return Returns the bar manager for the graphical editor.
	 */
	public MultiPageSubActionBars getGraphicalSubActionBars() {
		if (graphicalSubActionBars == null)
			if (getPage() != null && actionBars2 != null)
				graphicalSubActionBars = new MultiPageSubActionBars(getPage(),
							actionBars2,
							new SankeyBarContributor(),
							"org.openlca.app.results.analysis.sankey.actions" +
									".SankeyBarContributor");
		return graphicalSubActionBars;
	}

}
