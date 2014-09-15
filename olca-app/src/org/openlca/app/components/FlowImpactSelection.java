package org.openlca.app.components;

import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.Messages;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.app.viewers.combo.FlowViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.database.EntityCache;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.IResultProvider;

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

	private EntityCache cache;
	private Collection<FlowDescriptor> flows;
	private Collection<ImpactCategoryDescriptor> impacts;
	private Object initialSelection;
	private EventHandler eventHandler;

	private FlowViewer flowViewer;
	private ImpactCategoryViewer impactViewer;

	public static Dispatch on(IResultProvider result, EntityCache cache) {
		FlowImpactSelection selection = new FlowImpactSelection(cache);
		selection.flows = result.getFlowDescriptors();
		if (result.hasImpactResults())
			selection.impacts = result.getImpactDescriptors();
		return new Dispatch(selection);
	}

	private FlowImpactSelection(EntityCache cache) {
		this.cache = cache;
	}

	public void selectWithEvent(Object o) {
		if (o == null)
			return;
		if (o instanceof FlowDescriptor)
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
			flowViewer.select((FlowDescriptor) o);
			flowViewer.setEnabled(true);
		}
		if (impactViewer != null)
			impactViewer.setEnabled(false);
	}

	private void render(Composite parent, FormToolkit toolkit) {
		if (initialSelection instanceof ImpactCategoryDescriptor)
			resultType = IMPACT;
		else
			resultType = FLOW;
		if (flows != null && !flows.isEmpty())
			initFlowCheckViewer(toolkit, parent);
		if (impacts != null && !impacts.isEmpty())
			initImpactCheckViewer(toolkit, parent);
	}

	private void initFlowCheckViewer(FormToolkit toolkit, Composite section) {
		boolean typeFlows = !(initialSelection instanceof ImpactCategoryDescriptor);
		Button flowsCheck = toolkit.createButton(section, Messages.Flows,
				SWT.RADIO);
		flowsCheck.setSelection(typeFlows);
		flowViewer = new FlowViewer(section, cache);
		flowViewer.setEnabled(typeFlows);
		FlowDescriptor[] input = flows
				.toArray(new FlowDescriptor[flows.size()]);
		flowViewer.setInput(input);
		flowViewer.selectFirst();
		flowViewer.addSelectionChangedListener((val) -> fireSelection());
		if (initialSelection instanceof FlowDescriptor)
			flowViewer.select((FlowDescriptor) initialSelection);

		new ResultTypeCheck(flowViewer, flowsCheck, FLOW);
	}

	private void initImpactCheckViewer(FormToolkit toolkit, Composite section) {
		boolean typeImpact = initialSelection instanceof ImpactCategoryDescriptor;
		Button impactCheck = toolkit.createButton(section,
				Messages.ImpactCategories, SWT.RADIO);
		impactCheck.setSelection(typeImpact);
		impactViewer = new ImpactCategoryViewer(section);
		impactViewer.setEnabled(typeImpact);
		impactViewer.setInput(impacts);
		impactViewer.selectFirst();
		impactViewer.addSelectionChangedListener((val) -> fireSelection());
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

	private class ResultTypeCheck implements SelectionListener {

		private AbstractComboViewer<?> viewer;
		private Button check;
		private int type;

		public ResultTypeCheck(AbstractComboViewer<?> viewer, Button check,
				int type) {
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

	/**
	 * The event handler for selection changes.
	 */
	public interface EventHandler {

		void flowSelected(FlowDescriptor flow);

		void impactCategorySelected(ImpactCategoryDescriptor impactCategory);

	}

	/**
	 * Dispatch class for the initialization of the widgets.
	 */
	public static class Dispatch {

		private FlowImpactSelection selection;

		private Dispatch(FlowImpactSelection selection) {
			this.selection = selection;
		}

		public Dispatch withSelection(Object item) {
			selection.initialSelection = item;
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
