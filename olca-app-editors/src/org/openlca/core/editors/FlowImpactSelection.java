package org.openlca.core.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.results.AnalysisResult;
import org.openlca.ui.viewer.AbstractViewer;
import org.openlca.ui.viewer.FlowViewer;
import org.openlca.ui.viewer.ISelectionChangedListener;
import org.openlca.ui.viewer.ImpactCategoryViewer;

/**
 * Two combo boxes showing flows and impact categories. The impact categories
 * are only shown if the are category items available. The user can activate the
 * flow or impact combo and the selection change fires the selection of the
 * current activated combo.
 */
public class FlowImpactSelection {

	private final int FLOW = 0;
	private final int IMPACT = 1;
	private int resultType = FLOW;

	private IDatabase database;
	private AnalysisResult result;
	private Object initialSelection;
	private EventHandler eventHandler;

	private FlowViewer flowViewer;
	private ImpactCategoryViewer impactViewer;

	private FlowImpactSelection(IDatabase database) {
		this.database = database;
	}

	public void selectWithEvent(Object o) {
		if (o == null)
			return;
		if (o instanceof Flow)
			selectFlow(o);
		else if (o instanceof ImpactCategoryDescriptor)
			selectImpact(o);
		fireSelection();
	}

	public Object getSelection() {
		if (resultType == FLOW)
			return flowViewer.getSelected();
		return impactViewer.getSelected();
	}

	private void selectImpact(Object o) {
		resultType = IMPACT;
		if (impactViewer != null) {
			impactViewer.select((ImpactCategoryDescriptor) o);
			impactViewer.setEnabled(true);
		}
		if (flowViewer != null)
			flowViewer.setEnabled(false);
	}

	private void selectFlow(Object o) {
		resultType = FLOW;
		if (flowViewer != null) {
			flowViewer.select((Flow) o);
			flowViewer.setEnabled(true);
		}
		if (impactViewer != null)
			impactViewer.setEnabled(false);
	}

	public static Dispatch onDatabase(IDatabase database) {
		Dispatch dispatch = new Dispatch(new FlowImpactSelection(database));
		return dispatch;
	}

	private void render(Composite parent, FormToolkit toolkit) {
		if (result != null && result.getFlowIndex().getFlows().length > 0)
			initFlowCheckViewer(toolkit, parent);
		if (result != null && result.getImpactCategories().length > 0)
			initImpactCheckViewer(toolkit, parent);
		if (initialSelection instanceof ImpactCategoryDescriptor)
			resultType = IMPACT;
		else
			resultType = FLOW;
	}

	private void initFlowCheckViewer(FormToolkit toolkit, Composite section) {
		boolean typeFlows = !(initialSelection instanceof ImpactCategoryDescriptor);
		Button flowsCheck = toolkit.createButton(section, "Flows", SWT.RADIO);
		flowsCheck.setSelection(typeFlows);
		flowViewer = new FlowViewer(section);
		flowViewer.setEnabled(typeFlows);
		flowViewer.setDatabase(database);
		flowViewer.setInput(result);
		flowViewer.selectFirst();
		flowViewer.addSelectionChangedListener(new SelectionChange<Flow>());
		if (initialSelection instanceof Flow)
			flowViewer.select((Flow) initialSelection);

		new ResultTypeCheck(flowViewer, flowsCheck, FLOW);
	}

	private void initImpactCheckViewer(FormToolkit toolkit, Composite section) {
		boolean typeImpact = initialSelection instanceof ImpactCategoryDescriptor;
		Button impactCheck = toolkit.createButton(section, "Impact categories",
				SWT.RADIO);
		impactCheck.setSelection(typeImpact);
		impactViewer = new ImpactCategoryViewer(section);
		impactViewer.setEnabled(typeImpact);
		impactViewer.setInput(result);
		impactViewer.selectFirst();
		impactViewer
				.addSelectionChangedListener(new SelectionChange<ImpactCategoryDescriptor>());
		if (initialSelection instanceof ImpactCategoryDescriptor)
			impactViewer.select((ImpactCategoryDescriptor) initialSelection);

		new ResultTypeCheck(impactViewer, impactCheck, IMPACT);
	}

	private void fireSelection() {
		if (eventHandler == null)
			return;
		if (resultType == FLOW)
			eventHandler.flowSelected(flowViewer.getSelected());
		else {
			eventHandler.impactCategorySelected(impactViewer.getSelected());
		}
	}

	private class SelectionChange<T> implements ISelectionChangedListener<T> {
		@Override
		public void selectionChanged(T value) {
			fireSelection();
		}
	}

	private class ResultTypeCheck implements SelectionListener {

		private AbstractViewer<?> viewer;
		private Button check;
		private int type;

		public ResultTypeCheck(AbstractViewer<?> viewer, Button check, int type) {
			this.viewer = viewer;
			this.check = check;
			this.type = type;
			check.addSelectionListener(this);
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			if (check.getSelection()) {
				viewer.setEnabled(true);
				resultType = this.type;
				fireSelection();
			} else
				viewer.setEnabled(false);
		}
	}

	/** The event handler for selection changes. */
	public interface EventHandler {

		void flowSelected(Flow flow);

		void impactCategorySelected(ImpactCategoryDescriptor impactCategory);

	}

	/** Dispatch class for the initialisation of the widgets. */
	public static class Dispatch {

		private FlowImpactSelection selection;

		private Dispatch(FlowImpactSelection selection) {
			this.selection = selection;
		}

		public Dispatch withSelection(Object item) {
			selection.initialSelection = item;
			return this;
		}

		public Dispatch withAnalysisResult(AnalysisResult result) {
			selection.result = result;
			return this;
		}

		public Dispatch withEventHandler(EventHandler handler) {
			selection.eventHandler = handler;
			return this;
		}

		public FlowImpactSelection create(Composite parent, FormToolkit toolkit) {
			selection.render(parent, toolkit);
			return selection;
		}
	}

}
