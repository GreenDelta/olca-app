package org.openlca.app.navigation;

import java.util.Objects;

import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.openlca.app.collaboration.navigation.RepositoryLabel;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.DatabaseDirElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.GroupElement;
import org.openlca.app.navigation.elements.LibraryDirElement;
import org.openlca.app.navigation.elements.LibraryElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.app.navigation.elements.ScriptElement;
import org.openlca.util.Strings;

public class NavigationComparator extends ViewerComparator {

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 == null || e2 == null)
			return 0;

		// for elements of different types we have a defined type order
		if (!Objects.equals(e1.getClass(), e2.getClass()))
			return typeOrderOf(e1) - typeOrderOf(e2);

		// group elements have a predefined order
		if (e1 instanceof GroupElement)
			return 0;

		if (e1 instanceof ModelTypeElement && e2 instanceof ModelTypeElement) {
			return ModelTypeOrder.compare(
					((ModelTypeElement) e1).getContent(),
					((ModelTypeElement) e2).getContent());
		}

		// for script elements folders come before files
		if (e1 instanceof ScriptElement) {
			var f1 = ((ScriptElement) e1).getContent();
			var f2 = ((ScriptElement) e2).getContent();
			if (f1.isDirectory() && !f2.isDirectory())
				return -1;
			if (!f1.isDirectory() && f2.isDirectory())
				return 1;
			return Strings.compare(f1.getName(), f2.getName());
		}

		String name1 = getLabel(viewer, e1);
		String name2 = getLabel(viewer, e2);

		// when a database is connected to a repository, the navigation label
		// contains the repository URL as suffix; we do not want to include
		// that URL in this comparison
		if (e1 instanceof DatabaseElement && name1.contains(" ")) {
			name1 = name1.substring(0, name1.indexOf(" "));
		}
		if (e2 instanceof DatabaseElement && name2.contains(" ")) {
			name2 = name2.substring(0, name2.indexOf(" "));
		}

		return Strings.compare(name1, name2);
	}

	private int typeOrderOf(Object o) {
		if (o instanceof DatabaseDirElement)
			return 0;
		if (o instanceof DatabaseElement)
			return 1;
		if (o instanceof ModelTypeElement)
			return 2;
		if (o instanceof GroupElement)
			return 3;
		if (o instanceof CategoryElement)
			return 4;
		if (o instanceof ModelElement)
			return 5;
		if (o instanceof LibraryDirElement)
			return 6;
		if (o instanceof LibraryElement)
			return 7;
		if (o instanceof ScriptElement)
			return 8;
		return 10;
	}

	private String getLabel(Viewer viewer, Object obj) {
		if (obj == null)
			return "";
		if (!(viewer instanceof ContentViewer cv))
			return obj.toString();
		var prov = cv.getLabelProvider();
		if (prov instanceof ILabelProvider provider) {
			var label = provider.getText(obj);
			if (label == null)
				return "";
			var changed = RepositoryLabel.CHANGED_STATE;
			return label.startsWith(changed)
				? label.substring(changed.length())
				: label;
		}
		return obj.toString();
	}

}
