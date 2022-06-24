package org.openlca.app.editors.graphical.edit;

import java.beans.PropertyChangeEvent;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.openlca.app.editors.graphical.figures.GridPos;
import org.openlca.app.editors.graphical.figures.IOPaneFigure;
import org.openlca.app.editors.graphical.model.ExchangeItem;
import org.openlca.app.editors.graphical.model.IOPane;
import org.openlca.app.editors.graphical.model.GraphComponent;

import static org.openlca.app.editors.graphical.figures.ExchangeFigure.getPreferredAmountLabelSize;
import static org.openlca.app.editors.graphical.figures.ExchangeFigure.getPreferredUnitLabelSize;
import static org.openlca.app.editors.graphical.model.GraphComponent.CHILDREN_PROP;
import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_ADD_INPUT_EXCHANGE;
import static org.openlca.app.editors.graphical.requests.GraphRequestConstants.REQ_ADD_OUTPUT_EXCHANGE;

public class IOPaneEditPart extends AbstractComponentEditPart<IOPane> {

	@Override
	protected IFigure createFigure() {
		var figure = new IOPaneFigure(getModel());
		addButtonActionListener(figure);
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.CONTAINER_ROLE,
			new IOPaneContainerEditPolicy());
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (GraphComponent.SIZE_PROP.equals(prop)
			|| GraphComponent.LOCATION_PROP.equals(prop)) {
			refreshVisuals();
		} else if (CHILDREN_PROP.equals(prop)) {
			setLabelSizes(evt);
			refreshChildren();
		} else super.propertyChange(evt);
	}

	private void setLabelSizes(PropertyChangeEvent evt) {
		// Case of the removal of a child.
		if (evt.getNewValue() == null) {
			if (evt.getOldValue() instanceof ExchangeItem item) {
				getFigure().setAmountLabelSize(getPreferredAmountLabelSize(item), true);
				getFigure().setUnitLabelSize(getPreferredUnitLabelSize(item), true);
			}
		}
		else if (evt.getNewValue() instanceof ExchangeItem item) {
			getFigure().setAmountLabelSize(getPreferredAmountLabelSize(item), false);
			getFigure().setUnitLabelSize(getPreferredUnitLabelSize(item), false);
		}
	}

	@Override
	public IFigure getContentPane() {
		return getFigure().getContentPane();
	}

	@Override
	protected void addChildVisual(EditPart childEditPart, int index) {
		IFigure child = ((GraphicalEditPart) childEditPart).getFigure();
		getContentPane().add(child, GridPos.fillTop(), index);
	}

	@Override
	public IOPaneFigure getFigure() {
		return (IOPaneFigure) super.getFigure();
	}

	@Override
	public NodeEditPart getParent() {
		return (NodeEditPart) super.getParent();
	}

	protected void addButtonActionListener(IOPaneFigure figure) {
		figure.addExchangeButton.addActionListener($ -> {
			var request = getModel().isForInputs()
				? new Request(REQ_ADD_INPUT_EXCHANGE)
				: new Request(REQ_ADD_OUTPUT_EXCHANGE);
			var command = getCommand(request);
			getViewer().getEditDomain().getCommandStack().execute(command);
		});
	}

}
