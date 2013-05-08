/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.navigation;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.database.IDatabase;

/**
 * Abstract implementation of {@link INavigationElement}
 * 
 * @author Sebastian Greve
 * @author Michael Srocka
 * 
 */
public abstract class AbstractNavigationElement implements INavigationElement {

	private final List<INavigationElement> elements = new ArrayList<>();

	protected final void synchronize(List<INavigationElement> newElements) {
		removeOld(newElements);
		addNew(newElements);
	}

	private void removeOld(List<INavigationElement> newElements) {
		List<INavigationElement> toBeRemoved = new ArrayList<>();
		for (INavigationElement element : this.elements) {
			if (!contains(newElements, element)) {
				toBeRemoved.add(element);
			}
		}
		this.elements.removeAll(toBeRemoved);
	}

	private void addNew(List<INavigationElement> newElements) {
		List<INavigationElement> toBeAdded = new ArrayList<>();
		for (INavigationElement newElement : newElements) {
			if (!contains(this.elements, newElement)) {
				toBeAdded.add(newElement);
			}
		}
		this.elements.addAll(toBeAdded);
	}

	private boolean contains(List<INavigationElement> elements,
			INavigationElement element) {
		Object val1 = element.getData();
		for (INavigationElement e : elements) {
			Object val2 = e.getData();
			if (areEqual(val1, val2))
				return true;
		}
		return false;
	}

	private boolean areEqual(Object val1, Object val2) {
		if (val1 == null && val2 == null)
			return true;
		if (val1 != null && val2 != null)
			return val1.equals(val2)
					&& StringFieldComparator.areEqual(val1, val2);
		return false;
	}

	protected abstract void refresh();

	@Override
	public final INavigationElement[] getChildren(boolean refresh) {
		if (refresh) {
			refresh();
		}
		return elements.toArray(new INavigationElement[elements.size()]);
	}

	@Override
	public IDatabase getDatabase() {
		IDatabase database = null;
		INavigationElement parent = getParent();
		if (getData() instanceof IDatabase) {
			database = (IDatabase) getData();
		} else if (parent != null) {
			database = parent.getDatabase();
		}
		return database;
	}

	@Override
	public boolean isEmpty() {
		return elements.size() == 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof INavigationElement) {
			INavigationElement other = (INavigationElement) obj;
			return bothNullOrEqual(this.getDatabase(), other.getDatabase())
					&& bothNullOrEqual(this.getData(), other.getData());
		}
		return false;
	}

	private boolean bothNullOrEqual(Object that, Object other) {
		if (that == null)
			return other == null;
		return other != null && that.equals(other);
	}

	@Override
	public int hashCode() {
		int hash = 0;
		IDatabase db = getDatabase();
		if (db != null)
			hash += db.hashCode();
		Object data = getData();
		if (data != null)
			hash += data.hashCode();
		return hash == 0 ? super.hashCode() : hash;
	}

}
