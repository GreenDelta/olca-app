package org.openlca.app.navigation.actions;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.MappingFileElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Question;
import org.openlca.core.database.MappingFileDao;

public class DeleteMappingAction extends Action implements INavigationAction {

	private List<MappingFileElement> selection;

	public DeleteMappingAction() {
		setText(M.Delete);
		setImageDescriptor(Icon.DELETE.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection == null)
			return false;
		this.selection = selection.stream()
				.filter(e -> e instanceof MappingFileElement)
				.map(e -> (MappingFileElement) e)
				.filter(e -> e.getContent() != null)
				.collect(Collectors.toList());
		return !this.selection.isEmpty();
	}

	@Override
	public void run() {
		if (selection == null || selection.isEmpty())
			return;
		var db = Database.get();
		if (db == null)
			return;
		var delete = Question.ask(
				"Delete mapping file(s)?",
				"Do you want to delete the selected mapping file(s)?");
		if (!delete)
			return;
		var dao = new MappingFileDao(db);
		try {
			for (var elem : selection) {
				var name = elem.getContent();
				var mappingFile = dao.getForName(name);
				if (mappingFile != null) {
					dao.delete(mappingFile);
				}
			}
			Navigator.refresh();
		} catch (Exception e) {
			ErrorReporter.on("Failed to delete mapping file(s)", e);
		}
	}
}
