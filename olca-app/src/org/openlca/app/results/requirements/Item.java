package org.openlca.app.results.requirements;

interface Item {

	String name();

	default boolean isProvider() {
		return this instanceof ProviderItem;
	}

	default ProviderItem asProvider() {
		return (ProviderItem) this;
	}

	default boolean isChild() {
		return this instanceof ChildItem;
	}

	default ChildItem asChild() {
		return (ChildItem) this;
	}

	default boolean isCategory() {
		return this instanceof CategoryItem;
	}

	default CategoryItem asCategory() {
		return (CategoryItem) this;
	}

}
