package org.openlca.core.editors.model;

import org.openlca.util.Strings;

/**
 * A class for showing the essential information of a flow to the user.
 */
public class FlowInfo implements Comparable<FlowInfo> {

	private String id;
	private String name;
	private String unit;
	private String category;
	private String subCategory;
	private String location;

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getSubCategory() {
		return subCategory;
	}

	public void setSubCategory(String subCategory) {
		this.subCategory = subCategory;
	}

	@Override
	public int compareTo(FlowInfo other) {
		if (other == null)
			return 1;
		int c = Strings.compare(this.name, other.name);
		if (c != 0)
			return c;
		c = Strings.compare(this.category, other.category);
		if (c != 0)
			return c;
		c = Strings.compare(this.subCategory, other.subCategory);
		return c;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FlowInfo other = (FlowInfo) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
