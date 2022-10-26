package org.openlca.app.editors.graphical.actions.retarget;

import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.actions.GraphActionIds;
import org.openlca.app.rcp.images.Icon;

import static org.openlca.app.editors.graphical.actions.MassExpansionAction.COLLAPSE;
import static org.openlca.app.editors.graphical.actions.MassExpansionAction.EXPAND;
import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.MAXIMIZE;
import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.MINIMIZE;

public class MinMaxAllRetargetAction extends RetargetAction {

	public MinMaxAllRetargetAction(int type) {
		super(
				(type == MINIMIZE)
						? GraphActionIds.MINIMIZE_ALL
						: GraphActionIds.MAXIMIZE_ALL,
				(type == MINIMIZE)
						? M.MinimizeAll
						: M.MaximizeAll
		);

		if (type == MINIMIZE) {
			setToolTipText(M.MinimizeAll);
			setImageDescriptor(Icon.MINIMIZE.descriptor());
		} else if (type == MAXIMIZE) {
			setToolTipText(M.MaximizeAll);
			setImageDescriptor(Icon.MAXIMIZE.descriptor());
		}
	}

}
