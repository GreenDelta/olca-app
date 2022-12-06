package org.openlca.app.tools.graphics.actions.retarget;

import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.actions.RetargetAction;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.actions.GraphActionIds;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.graphics.actions.ActionIds;

public class LayoutAsTreeRetargetAction extends RetargetAction {

	public LayoutAsTreeRetargetAction() {
		super(ActionIds.LAYOUT_TREE, NLS.bind(M.LayoutAs, M.Tree));
		setToolTipText(NLS.bind(M.LayoutAs, M.Tree));
		setImageDescriptor(Icon.LAYOUT.descriptor());
	}

}
