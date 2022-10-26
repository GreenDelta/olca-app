package org.openlca.app.editors.graphical.actions.retarget;

import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.actions.GraphActionIds;
import org.openlca.app.rcp.images.Icon;

public class SetReferenceRetargetAction extends RetargetAction {

	public SetReferenceRetargetAction() {
		super(GraphActionIds.SET_REFERENCE,
				NLS.bind(M.SetAsQuantitativeReference, M.Reference));
		setToolTipText(NLS.bind(M.SetAsQuantitativeReference, M.Reference));
		setImageDescriptor(Icon.FORMULA.descriptor());
	}

}
