package org.openlca.app.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.progress.UIJob;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.StartPage;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.model.CategorizedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Editors {

	private static Logger log = LoggerFactory.getLogger(Editors.class);

	private Editors() {
	}

	/**
	 * Adds a refresh function to the tool-bar of the given form (content of a
	 * editor page). When this function is executed the given editor is closed
	 * and opened again.
	 */
	public static void addRefresh(ScrolledForm form, ModelEditor<?> editor) {
		if (form == null || editor == null)
			return;
		CategorizedEntity model = editor.getModel();
		Action refresh = Actions.create(M.Reload, Icon.REFRESH.descriptor(), 
				() -> {
					App.closeEditor(model);
					App.openEditor(model);
				});
		IToolBarManager toolbar = form.getToolBarManager();
		toolbar.add(refresh);
		toolbar.update(true);
	}

	/**
	 * Closes all editors (except StartPage)
	 */
	public static void closeAll() {
		try {
			if (StartPage.isOpen()) {
				closeAllExceptStartPage();
			} else {
				getActivePage().closeAllEditors(false);
			}
		} catch (Exception e) {
			log.error("Failed to close editors", e);
		}
	}

	private static void closeAllExceptStartPage() {
		List<IEditorReference> rest = new ArrayList<>();
		for (IEditorReference editor : getReferences())
			if (!StartPage.is(editor))
				rest.add(editor);
		IEditorReference[] restArray = rest.toArray(new IEditorReference[rest.size()]);
		getActivePage().closeEditors(restArray, false);
	}

	@SuppressWarnings("unchecked")
	public static <T extends IEditorPart> T getActive() {
		try {
			return (T) getActivePage().getActiveEditor();
		} catch (ClassCastException e) {
			log.error("Error getting active editor", e);
			return null;
		}
	}

	public static void open(IEditorInput input, String editorId) {
		new OpenInUIJob(input, editorId).schedule();
	}

	public static void close(IEditorReference ref) {
		try {
			getActivePage().closeEditor(ref.getEditor(false), true);
		} catch (Exception e) {
			log.error("Failed to close an editor", e);
		}
	}

	public static IEditorReference[] getReferences() {
		try {
			return getActivePage().getEditorReferences();
		} catch (Exception e) {
			log.error("Failed to get editor references", e);
			return new IEditorReference[0];
		}
	}

	private static IWorkbenchPage getActivePage() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}

	private static class OpenInUIJob extends UIJob {

		private IEditorInput input;
		private String editorId;

		public OpenInUIJob(IEditorInput input, String editorId) {
			super(M.OpenEditor);
			this.input = input;
			this.editorId = editorId;
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			try {
				getActivePage().openEditor(input, editorId);
				return Status.OK_STATUS;
			} catch (Exception e) {
				log.error("Open editor " + editorId + " failed.", e);
				return Status.CANCEL_STATUS;
			}
		}
	}

}
