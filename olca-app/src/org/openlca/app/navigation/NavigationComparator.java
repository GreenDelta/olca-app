package org.openlca.app.navigation;

import java.util.Objects;

import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.openlca.app.navigation.elements.CategoryElement;
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

		// TODO: document why this here makes sense: (?)
		// probably the status suffix when the database is
		// connected to a repository?
		if (e1 instanceof DatabaseElement && name1.contains(" "))
			name1 = name1.substring(0, name1.indexOf(" "));
		if (e2 instanceof DatabaseElement && name2.contains(" "))
			name2 = name2.substring(0, name2.indexOf(" "));

		return getComparator().compare(name1, name2);
	}

	private int typeOrderOf(Object o) {
		if (o instanceof DatabaseElement)
			return 0;
		if (o instanceof ModelTypeElement)
			return 1;
		if (o instanceof GroupElement)
			return 2;
		if (o instanceof CategoryElement)
			return 3;
		if (o instanceof ModelElement)
			return 4;
		if (o instanceof LibraryDirElement)
			return 5;
		if (o instanceof LibraryElement)
			return 6;
		if (o instanceof ScriptElement)
			return 7;
		return 10;
	}

	private String getLabel(Viewer viewer, Object e1) {
		if (!(viewer instanceof ContentViewer))
			return e1.toString();
		var prov = ((ContentViewer) viewer).getLabelProvider();
		if (prov instanceof ILabelProvider) {
			var lprov = (ILabelProvider) prov;
			var label = lprov.getText(e1);
			if (label == null)
				return "";
			String changed = RepositoryLabel.CHANGED_STATE;
			if (label.startsWith(changed))
				return label.substring(changed.length());
			return label;
		}
		return e1.toString();
	}

}
