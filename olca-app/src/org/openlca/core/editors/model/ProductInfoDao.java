package org.openlca.core.editors.model;

import org.apache.commons.lang.StringUtils;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductInfoDao {

	private IDatabase database;

	public ProductInfoDao(IDatabase database) {
		this.database = database;
	}

	public ProductInfo fromProduct(Process process, Exchange product) {
		if (process == null || product == null || database == null)
			return null;
		ProductInfo info = new ProductInfo();
		info.setId(product.getId());
		info.setProcessId(process.getId());
		info.setFromInfrastructureProcess(process.isInfrastructureProcess());
		info.setFromMultiOutputProcess(process.getOutputs(FlowType.ProductFlow).length > 1);
		info.setProcess(process.getName());
		if (process.getLocation() != null)
			info.setProcessLocation(process.getLocation().getName());
		addCategory(info, process);
		addProductInfo(info, product);
		return info;
	}

	private void addCategory(ProductInfo info, Process process) {
		if (process.getCategoryId() == null)
			return;
		try {
			Category cat = database.createDao(Category.class).getForId(
					process.getCategoryId());
			if (isNullOrRoot(cat))
				return;
			if (isNullOrRoot(cat.getParentCategory())) {
				info.setProcessCategory(cat.getName());
			} else {
				info.setProcessCategory(cat.getParentCategory().getName());
				info.setProcessSubCategory(cat.getName());
			}
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(FlowInfo.class);
			log.error("Failed to load process category", e);
		}
	}

	private boolean isNullOrRoot(Category category) {
		if (category == null)
			return true;
		return StringUtils.equals(category.getId(),
				Process.class.getCanonicalName());
	}

	private void addProductInfo(ProductInfo info, Exchange product) {
		Flow flow = product.getFlow();
		if (flow == null)
			return;
		FlowProperty refProp = flow.getReferenceFlowProperty();
		if (refProp == null)
			return;
		try {
			FlowDao dao = new FlowDao(database);
			info.setProduct(flow.getName());
			info.setProductId(flow.getId());
			info.setProductUnit(dao.getRefUnitName(flow));
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(FlowInfo.class);
			log.error("Failed to get the product unit", e);
		}
	}
}
