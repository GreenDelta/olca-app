package org.openlca.core.editors.flow;

import org.openlca.core.application.Messages;

/**
 * The column constants of the table viewer of the flow property factors.
 * 
 * @author Michael Srocka
 * 
 */
interface FlowPropertyColumn {

	int NAME = 0;

	int FACTOR = 1;

	int UNIT = 2;

	int IS_REFERNCE = 3;

	String[] LABELS = { Messages.Common_Name, Messages.Flows_ConversionFactor,
			Messages.Common_ReferenceUnit, Messages.Flows_IsReference };

}
