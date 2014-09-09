package org.openlca.app.results.projects;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Editors;
import org.openlca.app.util.InformationPopup;
import org.openlca.core.model.Project;
import org.openlca.core.results.ProjectResultProvider;
import org.openlca.io.xls.results.ProjectResultExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectResultActions extends EditorActionBarContributor {

	private Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * A failure flag that is passed between the export thread and the UI thread
	 */
	private boolean failed = false;

	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(new Action(Messages.ExportToExcel,
				ImageType.FILE_EXCEL_SMALL.getDescriptor()) {
			@Override
			public void run() {
				runExport();
			}
		});
	}

	private void runExport() {
		log.trace("export project results");
		ProjectResultEditor editor = Editors.getActive();
		if (editor == null || editor.getResult() == null
				|| editor.getProject() == null) {
			log.error("Could not get project result from editor");
			return;
		}
		Project project = editor.getProject();
		ProjectResultProvider result = editor.getResult();
		File file = FileChooser.forExport(".xlsx", project.getName() + ".xlsx");
		if (file == null)
			return;
		ProjectResultExport export = new ProjectResultExport(project, file,
				Cache.getEntityCache());
		tryRun(export, result);
	}

	private void tryRun(final ProjectResultExport export,
			final ProjectResultProvider result) {
		App.run(Messages.ExportResults, new Runnable() {
			public void run() {
				try {
					export.run(result);
					failed = false;
				} catch (Exception e) {
					log.error("Project exort failed", e);
					failed = true;
				}
			}
		}, new Runnable() {
			public void run() {
				if (!failed)
					InformationPopup.show(Messages.ExportDone);
			}
		});
	}

}
