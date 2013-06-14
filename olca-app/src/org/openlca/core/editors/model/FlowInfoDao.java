package org.openlca.core.editors.model;

import org.apache.commons.lang.StringUtils;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowInfoDao {

	private IDatabase database;

	public FlowInfoDao(IDatabase database) {
		this.database = database;
	}

	public FlowInfo fromFlow(Flow flow) {
		if (flow == null || database == null)
			return null;
		FlowInfo info = new FlowInfo();
		info.setId(flow.getId());
		info.setName(flow.getName());
		if (flow.getLocation() != null)
			info.setLocation(flow.getLocation().getName());
		addCategory(info, flow);
		addUnit(info, flow);
		return info;
	}

	private void addCategory(FlowInfo info, Flow flow) {
		if (flow.getCategoryId() == null)
			return;
		try {
			Category cat = database.createDao(Category.class).getForId(
					flow.getCategoryId());
			if (isNullOrRoot(cat))
				return;
			if (isNullOrRoot(cat.getParentCategory())) {
				info.setCategory(cat.getName());
			} else {
				info.setCategory(cat.getParentCategory().getName());
				info.setSubCategory(cat.getName());
			}
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(FlowInfo.class);
			log.error("Failed to load flow category", e);
		}
	}

	private boolean isNullOrRoot(Category flowCategory) {
		if (flowCategory == null)
			return true;
		return StringUtils.equals(flowCategory.getId(),
				Flow.class.getCanonicalName());
	}

	private void addUnit(FlowInfo info, Flow flow) {
		FlowProperty refProp = flow.getReferenceFlowProperty();
		if (refProp == null)
			return;
		try {
			FlowDao dao = new FlowDao(database.getEntityFactory());
			info.setUnit(dao.getRefUnitName(flow));
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(FlowInfo.class);
			log.error("Failed to get the flow unit", e);
		}
	}

}
