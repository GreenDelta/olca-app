package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.ModelTextFilter;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.filters.EmptyCategoryFilter;
import org.openlca.app.navigation.filters.FlowTypeFilter;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Result;
import org.openlca.core.model.descriptors.FlowDescriptor;

public class ResultWizard extends AbstractWizard<Result> {

	@Override
	protected String getTitle() {
		return M.NewResult;
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.RESULT;
	}

	@Override
	protected AbstractWizardPage<Result> createPage() {
		return new Page();
	}

	private static class Page extends AbstractWizardPage<Result> {

		private Text flowText;
		private TreeViewer flowTree;

		private Page() {
			super("ResultWizardPage");
			setTitle(M.NewResult);
			setWithDescription(false);
			setPageComplete(false);
		}

		@Override
		protected void modelWidgets(Composite comp) {
			UI.label(comp, M.ReferenceFlow);
			this.flowText = UI.text(comp);
			UI.label(comp, "");
			var treeComp = UI.composite(comp);
			UI.gridData(treeComp, true, true).heightHint = 200;
			UI.gridLayout(treeComp, 1, 0, 0);
			this.flowTree = NavigationTree.forSingleSelection(
				treeComp, ModelType.FLOW);
			UI.gridData(flowTree.getTree(), true, true).heightHint = 200;
			flowTree.addFilter(new FlowTypeFilter(FlowType.ELEMENTARY_FLOW));
			flowTree.addFilter(new EmptyCategoryFilter());
			flowTree.addFilter(new ModelTextFilter(flowText, flowTree));
		}

		@Override
		public Result createModel() {
			var flow = getSelectedFlow();
			Result result;
			if (flow != null) {
				result = Result.of(getModelName(), flow);
			} else {
				result = new Result();
				result.refId = UUID.randomUUID().toString();
				result.name = getModelName();
			}
			return result;
		}

		private Flow getSelectedFlow() {
			INavigationElement<?> e = Viewers.getFirstSelected(flowTree);
			return e != null && e.getContent() instanceof FlowDescriptor d
				? Database.get().get(Flow.class, d.id)
				: null;
		}
	}
}
