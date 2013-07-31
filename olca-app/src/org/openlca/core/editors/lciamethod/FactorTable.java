package org.openlca.core.editors.lciamethod;

import org.openlca.app.Messages;

final class FactorTable {

	static final int FLOW_COLUMN = 0;
	static final int CATEGORY_COLUM = 1;
	static final int PROPERTY_COLUMN = 2;
	static final int UNIT_COLUMN = 3;
	static final int VALUE_COLUMN = 4;
	static final int UNCERTAINTY_COLUMN = 5;

	static final double[] COLUMN_WIDTHS = { 0.2, 0.2, 0.1, 0.1, 0.1, 0.3 };

	// column properties

	static final String CATEGORY = Messages.Common_Category;
	static final String FLOW = Messages.Common_Flow;
	static final String PROPERTY = Messages.Common_FlowProperty;
	static final String UNIT = Messages.Common_Unit;
	static final String VALUE = Messages.Methods_Value;
	static final String UNCERTAINTY = Messages.Common_Uncertainty;
	static final String[] COLUMN_PROPERTIES = new String[] { FLOW, CATEGORY,
			PROPERTY, UNIT, VALUE, UNCERTAINTY };

	private FactorTable() {
	}

}
