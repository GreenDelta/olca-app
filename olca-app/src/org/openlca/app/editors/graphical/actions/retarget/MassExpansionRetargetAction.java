package org.openlca.app.editors.graphical.actions.retarget;

import static org.openlca.app.editors.graphical.actions.MassExpansionAction.*;

import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphActionIds;
import org.openlca.app.rcp.images.Icon;

public class MassExpansionRetargetAction extends RetargetAction {

	public MassExpansionRetargetAction(int type) {
		super(
				(type == EXPAND)
						? GraphActionIds.EXPAND_ALL
						: GraphActionIds.COLLAPSE_ALL,
				(type == EXPAND)
						? M.ExpandAll
						: M.CollapseAll
		);

		if (type == EXPAND) {
			setToolTipText(M.ExpandAll);
			setImageDescriptor(Icon.EXPAND.descriptor());
		} else if (type == COLLAPSE) {
			setToolTipText(M.CollapseAll);
			setImageDescriptor(Icon.COLLAPSE.descriptor());
		}
	}

}
