package org.openlca.app.db;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.devtools.python.Python;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.rcp.html.HtmlFolder;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.Info;
import org.openlca.app.util.UI;
import org.openlca.updates.Update;
import org.openlca.updates.UpdateHelper;
import org.openlca.updates.UpdateMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

public class UpdateManager {

	private final static Logger log = LoggerFactory.getLogger(UpdateManager.class);

	/**
	 * Opens the update manager with all "unseen" and/or required updates.
	 * 
	 * @return true if the user installed (at least the required) updates, false if
	 *         canceled
	 */
	public static boolean openNewAndRequired() {
		UpdateHelper updater = new UpdateHelper(Database.get(), App.getCalculationContext(), Python.getDir());
		Set<UpdateMetaInfo> updates = updater.getNewAndRequired();
		if (updates.isEmpty())
			return true;
		return new Dialog(updater, updates).open() == IDialogConstants.OK_ID;
	}

	public static void openAll() {
		UpdateHelper updater = new UpdateHelper(Database.get(), App.getCalculationContext(), Python.getDir());
		Set<UpdateMetaInfo> updates = updater.getAllUpdates();
		if (updates.isEmpty())
			return;
		new Dialog(updater, updates).open();
	}

	private UpdateManager() {

	}

	private static class Dialog extends FormDialog implements WebPage {

		private final Set<UpdateMetaInfo> updates;
		private final Map<String, Update> localUpdates = new HashMap<>();
		private final UpdateHelper updater;
		private WebEngine webkit;
		private boolean hideExecuted = true;
		private boolean hasExecuted = false;
		private Gson gson = new Gson();

		public Dialog(UpdateHelper updater, Set<UpdateMetaInfo> updates) {
			super(UI.shell());
			this.updater = updater;
			this.updates = updates;
			for (UpdateMetaInfo update : updates) {
				if (update.executed) {
					hasExecuted = true;
				}
			}
		}

		private List<UpdateMetaInfo> topLevelUpdates() {
			Stack<String> toCheck = new Stack<>();
			Map<String, UpdateMetaInfo> topLevelUpdates = new HashMap<>();
			for (UpdateMetaInfo update : updates) {
				for (String mRefId : update.dependencies) {
					toCheck.add(mRefId);
				}
				topLevelUpdates.put(update.refId, update);
			}
			while (!toCheck.isEmpty()) {
				String mRefId = toCheck.pop();
				UpdateMetaInfo update = topLevelUpdates.remove(mRefId);
				if (update != null) {
					toCheck.addAll(update.dependencies);
				}
			}
			return new ArrayList<UpdateMetaInfo>(topLevelUpdates.values());
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			FormToolkit toolkit = mform.getToolkit();
			ScrolledForm form = UI.formHeader(mform, "openLCA Update Manager");
			Composite body = form.getBody();
			UI.gridLayout(body.getParent(), 1, 0, 0);
			UI.gridLayout(body.getParent().getParent(), 1, 0, 0);
			body.setLayout(new FillLayout());
			toolkit.paintBordersFor(body);
			UI.createWebView(body, this);
			form.reflow(true);
		}

		@Override
		protected void okPressed() {
			List<String> selection = new ArrayList<>(Arrays.asList(webkit.executeScript("getSelection()").toString()
					.split(",")));
			App.runWithProgress(M.ApplyingDatabaseUpdates, () -> {
				Database.getIndexUpdater().disable();
				List<Update> toExecute = new ArrayList<>();
				for (UpdateMetaInfo metaInfo : updates) {
					Update update = updater.getForRefId(metaInfo.refId);
					if (update == null)
						continue;
					if (selection.contains(metaInfo.refId)) {
						toExecute.add(update);
						selection.remove(metaInfo.refId);
					} else {
						updater.skip(update);
					}
				}
				for (String refId : selection) {
					Update update = localUpdates.get(refId);
					if (update == null)
						continue;
					toExecute.add(update);
				}
				Collections.sort(toExecute);
				for (Update update : toExecute) {
					execute(update);
				}
				Database.getIndexUpdater().enable();
			});
			Navigator.refresh();
			super.okPressed();
		}

		private void execute(Update update) {
			if (update == null)
				return;
			for (String depRefId : update.metaInfo.dependencies) {
				Update dependency = updater.getForRefId(depRefId);
				execute(dependency);
			}
			updater.execute(update);
		}

		@Override
		public String getUrl() {
			return HtmlFolder.getUrl(RcpActivator.getDefault().getBundle(), "update_manager.html");
		}

		@Override
		public void onLoaded(WebEngine webkit) {
			this.webkit = webkit;
			JSObject window = (JSObject) webkit.executeScript("window");
			window.setMember("java", new JsHandler());
			refresh();
		}

		private void refresh() {
			List<UpdateMetaInfo> metaInfos = topLevelUpdates();
			metaInfos.sort((m1, m2) -> -Long.compare(m1.releaseDate.getTime(), m2.releaseDate.getTime()));
			String data = new Gson().toJson(metaInfos);
			webkit.executeScript("setData(" + data + ")");
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			if (hasExecuted) {
				Button hideShowExecuted = createButton(parent, 9999, M.ShowExecutedUpdates, true);
				Controls.onSelect(hideShowExecuted, (e) -> {
					if (hideExecuted == true) {
						webkit.executeScript("showExecuted()");
						hideShowExecuted.setText(M.HideExecutedUpdates);
						hideExecuted = false;
					} else {
						webkit.executeScript("hideExecuted()");
						hideShowExecuted.setText(M.ShowExecutedUpdates);
						hideExecuted = true;
					}
				});
			}

			Button browse = createButton(parent, 9998, M.BrowseLocalFiles, true);
			Controls.onSelect(browse, (e) -> {
				File updateFile = FileChooser.forImport("*.polca|openLCA Patch file");
				if (updateFile == null)
					return;
				try {
					Update update = Update.open(new FileInputStream(updateFile));
					if (exists(update))
						return;
					localUpdates.put(update.metaInfo.refId, update);
					String data = gson.toJson(update.metaInfo);
					webkit.executeScript("var data = " + data
							+ ";data.parentRefId=null;$('#container').append(renderUpdate(" + data + "));");
					webkit.executeScript("$('#no-unexecuted-message').hide()");
				} catch (Exception e1) {
					log.error("Error opening patch file", e1);
				}
			});
			((GridLayout) parent.getLayout()).numColumns++;
			UI.filler(parent);
			super.createButtonsForButtonBar(parent);
		}

		private boolean exists(Update update) {
			boolean exists = false;
			if (localUpdates.containsKey(update.metaInfo.refId)) {
				exists = true;
			}
			for (UpdateMetaInfo metaInfo : updates) {
				if (metaInfo.refId.equals(update.metaInfo.refId)) {
					exists = true;
				}
			}
			if (exists) {
				Info.popup(M.UpdateWasAlreadyAddedOrExecuted);
			}
			return exists;
		}

		@SuppressWarnings("unused")
		public class JsHandler {

			public String getUpdate(String refId) {
				Update update = localUpdates.get(refId);
				if (update != null)
					return gson.toJson(update.metaInfo);
				UpdateMetaInfo metaInfo = updater.getForRefId(refId).metaInfo;
				return gson.toJson(metaInfo);
			}

			public boolean hasAttachment(String refId) {
				Update update = localUpdates.get(refId);
				if (update == null) {
					update = updater.getForRefId(refId);
				}
				return update != null && update.attachment != null && update.attachment.length > 0;
			}

			public void openAttachment(String refId) {
				if (!hasAttachment(refId))
					return;
				Update update = localUpdates.get(refId);
				if (update == null) {
					update = updater.getForRefId(refId);
				}
				if (update == null)
					return;
				try {
					Path tmp = Files.createTempFile("olca", ".pdf");
					Files.copy(new ByteArrayInputStream(update.attachment), tmp, StandardCopyOption.REPLACE_EXISTING);
					Desktop.browse(tmp.toUri().toString());
					tmp.toFile().deleteOnExit();
				} catch (IOException e) {
					log.error("Error opening attachment", e);
				}
			}

		}

	}

}
