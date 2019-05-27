package org.openlca.app.viewers.combo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.FlowPropertyDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowPropertyViewer extends
		AbstractComboViewer<FlowPropertyDescriptor> {

	public FlowPropertyViewer(Composite parent) {
		super(parent);
		setInput(new FlowPropertyDescriptor[0]);
	}

	/**
	 * Sets the flow properties from the given database as input of this combo
	 * viewer. The flow properties are sorted by their usage in the flows of the
	 * database. If there is a flow property `mass` contained in the database it
	 * is always sorted to the top of the list.
	 */
	public void setInput(IDatabase db) {
		try {
			List<FlowPropertyDescriptor> props = new FlowPropertyDao(
					db).getDescriptors();
			Map<Long, Integer> count = propCount(db);

			Collections.sort(props, (p1, p2) -> {
				if ("mass".equalsIgnoreCase(p1.name))
					return -1;
				if ("mass".equalsIgnoreCase(p2.name))
					return 1;
				int c1 = count.getOrDefault(p1.id, 0);
				int c2 = count.getOrDefault(p2.id, 0);
				if (c1 == c2)
					return Strings.compare(p1.name, p2.name);
				return c2 - c1;
			});
			setInput(props.toArray(
					new FlowPropertyDescriptor[props.size()]));
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Loading flow properties failed", e);
		}
	}

	@Override
	protected ViewerComparator getComparator() {
		return null;
	}

	@Override
	public Class<FlowPropertyDescriptor> getType() {
		return FlowPropertyDescriptor.class;
	}

	private static Map<Long, Integer> propCount(IDatabase db) {
		String sql = "select prop.id, count(fac.f_flow_property) from "
				+ "tbl_flow_properties prop inner join tbl_flow_property_factors "
				+ "fac on prop.id = fac.f_flow_property group by prop.id";
		Map<Long, Integer> count = new HashMap<>();
		try {
			NativeSql.on(db).query(sql, r -> {
				count.put(r.getLong(1), r.getInt(2));
				return true;
			});
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(
					FlowPropertyViewer.class);
			log.error("Collecting usage count failed", e);
		}
		return count;
	}

}
