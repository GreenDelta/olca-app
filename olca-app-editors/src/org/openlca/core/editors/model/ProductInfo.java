package org.openlca.core.editors.model;

import org.openlca.util.Strings;

/**
 * A class for showing the essential information of a flow to the user.
 */
public class ProductInfo implements Comparable<ProductInfo> {

	private String id;
	private boolean fromInfrastructureProcess;
	private boolean fromMultiOutputProcess;
	private String process;
	private String processCategory;
	private String processId;
	private String processLocation;
	private String processSubCategory;
	private String product;
	private String productId;
	private String productUnit;

	public String getId() {
		return id;
	}

	public String getProcess() {
		return process;
	}

	public String getProcessCategory() {
		return processCategory;
	}

	public String getProcessId() {
		return processId;
	}

	public String getProcessLocation() {
		return processLocation;
	}

	public String getProcessSubCategory() {
		return processSubCategory;
	}

	public String getProduct() {
		return product;
	}

	public String getProductId() {
		return productId;
	}

	public String getProductUnit() {
		return productUnit;
	}

	public boolean isFromInfrastructureProcess() {
		return fromInfrastructureProcess;
	}

	public boolean isFromMultiOutputProcess() {
		return fromMultiOutputProcess;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setFromInfrastructureProcess(boolean fromInfrastructureProcess) {
		this.fromInfrastructureProcess = fromInfrastructureProcess;
	}

	public void setFromMultiOutputProcess(boolean fromMultiOutputProcess) {
		this.fromMultiOutputProcess = fromMultiOutputProcess;
	}

	public void setProcess(String process) {
		this.process = process;
	}

	public void setProcessCategory(String processCategory) {
		this.processCategory = processCategory;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	public void setProcessLocation(String processLocation) {
		this.processLocation = processLocation;
	}

	public void setProcessSubCategory(String processSubCategory) {
		this.processSubCategory = processSubCategory;
	}

	public void setProduct(String product) {
		this.product = product;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public void setProductUnit(String productUnit) {
		this.productUnit = productUnit;
	}

	@Override
	public int compareTo(ProductInfo other) {
		if (other == null)
			return 1;
		int c = Strings.compare(this.process, other.process);
		if (c != 0)
			return c;
		c = Strings.compare(this.processCategory, other.processCategory);
		if (c != 0)
			return c;
		c = Strings.compare(this.processSubCategory, other.processSubCategory);
		if (c != 0)
			return c;
		c = Strings.compare(this.product, other.product);
		return c;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProductInfo other = (ProductInfo) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

}
