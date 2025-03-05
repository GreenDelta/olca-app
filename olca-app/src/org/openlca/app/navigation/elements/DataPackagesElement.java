package org.openlca.app.navigation.elements;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openlca.app.rcp.Workspace;
import org.openlca.core.database.IDatabase.DataPackage;
import org.openlca.core.library.LibraryDir;

public class DataPackagesElement extends NavigationElement<LibraryDir> {

	private Set<DataPackage> dataPackages;

	public DataPackagesElement(INavigationElement<?> parent, Set<DataPackage> dataPackages) {
		super(parent, Workspace.getLibraryDir());
		this.dataPackages = dataPackages;
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		return dataPackages.stream()
				.map(p -> new DataPackageElement(this, p))
				.collect(Collectors.toList());
	}

}
