/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.rcp.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wizard for exporting processes, flows, flow properties and unit group to the
 * OLCAPack format
 */
public class PluginManagerDialog extends FormDialog {

	private static final Logger log = LoggerFactory
			.getLogger(PluginManagerDialog.class);

	private static AtomicReference<PluginManagerDialog> lastOpenedPluginsManagerDialog = new AtomicReference<>(
			null);
	private static AtomicInteger runningBackgroundJobs = new AtomicInteger(0);

	private final PluginsService pluginsService;

	private volatile boolean closed = false;

	private AvailablePluginsForm availablePluginsForm;

	private IManagedForm mform;

	private static boolean requestRestart = false;

	public PluginManagerDialog(Shell parentShell) {
		super(parentShell);
		pluginsService = new PluginsService();
		lastOpenedPluginsManagerDialog.set(this);
	}

	public boolean isClosed() {
		return closed;
	}

	@Override
	public boolean close() {
		this.closed = true;
		return super.close();
	}

	public IMessageManager getMessageManager() {
		return mform.getMessageManager();
	}

	@Override
	protected void createFormContent(final IManagedForm mform) {
		this.mform = mform;
		try {
			log.debug("creating dialog...");

			availablePluginsForm = new AvailablePluginsForm(this, mform,
					pluginsService);
			availablePluginsForm.getForm().setLayoutData(
					new GridData(SWT.FILL, SWT.FILL, true, true));

			mform.getMessageManager().addMessage("conn",
					"Connecting to server...", null,
					IMessageProvider.INFORMATION);

			reloadPlugins();

		} catch (UserMessageException ume) {
			mform.getMessageManager().addMessage("conn", ume.getMessage(),
					null, IMessageProvider.ERROR);
		} catch (Exception e) {
			log.error("Exception", e);
		}
	}

	public static void reloadPlugins() {
		if (isOpenPluginManagerDialogPresent()) {
			PluginManagerDialog pluginManagerDialog = lastOpenedPluginsManagerDialog
					.get();
			if (pluginManagerDialog != null) {
				pluginManagerDialog.availablePluginsForm.clearPlugins();
				pluginManagerDialog.new PluginListLoaderJob(
						pluginManagerDialog.mform).schedule();
			}
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true).addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {
				if (isRequestRestart()) {
					questionAndRestartIfYes();
				}
			}

		});
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	protected class PluginListLoaderJob extends Job {

		private final List<Plugin> additionalPlugins = new ArrayList<>();

		protected PluginListLoaderJob(IManagedForm mform) {
			super("Load plugin list from server");
		}

		/**
		 * An override for specifying plugins to list that are not in the public
		 * repository.
		 * 
		 * @param p
		 */
		public void addAdditionalPlugin(Plugin p) {
			additionalPlugins.add(p);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				final List<Plugin> allPlugs = new ArrayList<>();
				allPlugs.addAll(additionalPlugins);

				PluginListWrapper loadedPlugins = pluginsService
						.loadPluginsFromServer();
				PluginListWrapper installedPlugins;
				try {
					installedPlugins = pluginsService.getInstalledPlugins();
				} catch (Exception e) {
					throw new UserMessageException("Plugins service failed: "
							+ e.getMessage());
				}

				if (installedPlugins != null
						&& installedPlugins.getPlugins() != null) {
					allPlugs.addAll(pluginsService.mergePluginInfo(
							installedPlugins, loadedPlugins,
							pluginsService.getOpenlcaVersion()).getPlugins());
				} else {
					allPlugs.addAll(loadedPlugins.getPlugins());
				}

				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (!isClosed()) {
							try {
								availablePluginsForm.showPlugins(allPlugs);
								mform.getMessageManager().removeMessage("conn");
							} catch (Exception e) {
								log.error("Showing plugins failed", e);
							}
						}
					}
				});
			} catch (final UserMessageException ume) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (!isClosed()) {
							mform.getMessageManager().addMessage("conn",
									ume.getMessage(), null,
									IMessageProvider.ERROR);
						}
					}
				});
			}
			return Status.OK_STATUS;
		}
	}

	public static void setRequestRestart(boolean b) {
		requestRestart = b;
	}

	public static boolean isRequestRestart() {
		return requestRestart;
	}

	public static void backgroundJobStarting() {
		runningBackgroundJobs.incrementAndGet();
	}

	public static boolean isOpenPluginManagerDialogPresent() {
		PluginManagerDialog currDialog = lastOpenedPluginsManagerDialog.get();
		if (currDialog != null && !currDialog.isClosed()) {
			return true;
		}
		return false;
	}

	public static void backgroundJobFinishing() {
		int nowRunningJobs = runningBackgroundJobs.decrementAndGet();
		if (!isOpenPluginManagerDialogPresent()) {
			if (nowRunningJobs == 0 && PluginManagerDialog.isRequestRestart()) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {

						questionAndRestartIfYes();
					}
				});
			}
		}

	}

	public static void questionAndRestartIfYes() {
		if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
				"Restart?", "openLCA must restart for changes to take effect. "
						+ "Would you like to restart now?")) {
			PlatformUI.getWorkbench().restart();
		}
	}

	public static void restartNecessary() {

		setRequestRestart(true);

	}

}
