package org.openlca.app.plugin.installer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.eclipse.ui.forms.widgets.ColumnLayoutData;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.openlca.app.plugin.installer.helpers.Base64ImageHelper;
import org.openlca.app.util.ErrorPopup;
import org.openlca.app.util.InformationPopup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class AvailablePluginsForm {

	private static final Logger log = LoggerFactory
			.getLogger(AvailablePluginsForm.class);

	/**
	 * symbolic name to message.
	 */
	private static final HashMap<String, String> changedPlugins = new HashMap<>();

	private HashMap<String, Plugin> plugins = new HashMap<>();

	private IManagedForm mform;

	private LinkedHashMap<Composite, Plugin> pluginComps = new LinkedHashMap<>();

	private PluginsService pluginsService;

	private PluginManagerDialog pluginManagerDialog;

	public AvailablePluginsForm(PluginManagerDialog pluginManagerDialog,
			IManagedForm mform, PluginsService pluginsService) {
		this.pluginManagerDialog = pluginManagerDialog;
		this.mform = mform;
		this.pluginsService = pluginsService;
		getForm().setText("Plugins");
		getToolkit().decorateFormHeading(mform.getForm().getForm());
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 1;
		getBody().setLayout(layout);
	}

	protected ScrolledForm getForm() {
		return mform.getForm();
	}

	protected Composite getBody() {
		return mform.getForm().getBody();
	}

	protected IMessageManager getMessageManager() {
		return mform.getMessageManager();
	}

	public PluginManagerDialog getPluginManagerDialog() {
		return pluginManagerDialog;
	}

	public void clearPlugins() {
		clear();
	}

	public void showPlugins(List<Plugin> plugins) {
		for (Plugin p : plugins)
			if (p.isInstallable())
				addPlugin(p);
		getForm().reflow(true);
	}

	public void addPlugin(Plugin p) {
		if (plugins.containsKey(p.getSymbolicName())) {
			log.warn("Plugin {} already listed, ignoring", p.getSymbolicName());
			return;
		}
		Composite pluginComp = getToolkit()
				.createComposite(getBody(), SWT.NONE);
		pluginComp.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		TableWrapLayout pluginOuterLayout = new TableWrapLayout();
		pluginOuterLayout.numColumns = 2;
		pluginComp.setLayout(pluginOuterLayout);

		Label imageLabel = new Label(pluginComp, SWT.NONE);

		provideImage(pluginComp, imageLabel, p);

		Composite rightComp = getToolkit()
				.createComposite(pluginComp, SWT.NONE);
		rightComp.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		rightComp.setLayout(new TableWrapLayout());

		Composite headerComp = getToolkit()
				.createComposite(rightComp, SWT.NONE);
		headerComp.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		ColumnLayout headerLayout = new ColumnLayout();
		headerLayout.maxNumColumns = 2;
		headerLayout.minNumColumns = 1;
		headerComp.setLayout(headerLayout);
		String nameText = p.getName();
		if (Strings.isNullOrEmpty(nameText)) {
			nameText = p.getSymbolicName();
		}
		getToolkit().createLabel(headerComp, nameText, SWT.WRAP)
				.setToolTipText(p.getSymbolicName());
		Composite buttonComposite = getToolkit().createComposite(headerComp,
				SWT.FILL);
		ColumnLayoutData buttonLayoutData = new ColumnLayoutData();
		buttonLayoutData.horizontalAlignment = ColumnLayoutData.RIGHT;
		buttonComposite.setLayoutData(buttonLayoutData);
		GridLayout buttonGrid = new GridLayout();
		buttonComposite.setLayout(buttonGrid);
		addButtons(buttonComposite, p);

		getToolkit().createLabel(
				rightComp,
				"Available version: "
						+ (Strings.isNullOrEmpty(p.getVersion()) ? "None" : p
								.getVersion()));

		String installedVersion = "Not installed";
		if (!Strings.isNullOrEmpty(p.getInstalledVersion())) {
			installedVersion = "Installed: " + p.getInstalledVersion();
		}
		getToolkit().createLabel(rightComp, installedVersion);

		getToolkit().createLabel(rightComp, p.getDescription(), SWT.WRAP);

		plugins.put(p.getSymbolicName(), p);
		pluginComps.put(pluginComp, p);
	}

	private static final String DEFAULT_IMAGE = "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";

	private void provideImage(Composite pluginComp, Label imageLabel, Plugin p) {

		Image image = PluginsImageType.NEWSEARCH_WIZ.get();
		try {
			// FIXME go on : 1. remove plugin installer from plugin list

			String imageAsB64 = p.getImage();
			if (Strings.isNullOrEmpty(imageAsB64))
				imageAsB64 = DEFAULT_IMAGE;
			byte[] decodedImage = new Base64ImageHelper()
					.decodeBase64EncodedImage(imageAsB64);
			final Image image2 = new Image(pluginComp.getDisplay(),
					new ByteArrayInputStream(decodedImage));

			imageLabel.addDisposeListener(new DisposeListener() {

				@Override
				public void widgetDisposed(DisposeEvent e) {
					image2.dispose();
				}
			});
			image = image2;
		} catch (IOException e) {
			log.debug("Plugin {} without own image, using default.");
		}

		imageLabel.setImage(image);

	}

	private void addButtons(Composite buttonComposite, final Plugin p) {
		boolean available = false;
		boolean installed = false;
		boolean newerAvailable = false;
		if (!Strings.isNullOrEmpty(p.getVersion())) {
			available = true;
		}
		if (!Strings.isNullOrEmpty(p.getInstalledVersion())) {
			installed = true;
		}
		if (available && installed) {
			newerAvailable = PluginsService.isNewer(p.getVersion(),
					p.getInstalledVersion());
		}

		boolean changed = changedPlugins.containsKey(p.getSymbolicName());
		if (changed) {
			getToolkit().createLabel(buttonComposite,
					changedPlugins.get(p.getSymbolicName()), SWT.WRAP);
		} else {
			if (available && !installed) {
				final Button b = getToolkit().createButton(buttonComposite,
						"Install", SWT.PUSH);
				b.addListener(SWT.Selection, new InstallOrUpdateButtonListener(
						"Installation of " + p.getSymbolicName(), p));
				b.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event event) {
						b.setEnabled(false);
						pluginChanged(p, "Installing...");
					}
				});
			} else if (newerAvailable) {
				final Button b = getToolkit().createButton(buttonComposite,
						"Update", SWT.PUSH);
				b.addListener(SWT.Selection, new InstallOrUpdateButtonListener(
						"Update of " + p.getSymbolicName(), p));
				b.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event event) {
						b.setEnabled(false);
						pluginChanged(p, "Updating...");
					}
				});
			}
			if (installed) {
				log.debug("Adding the remove button for {}", p);
				getToolkit().createButton(buttonComposite, "Remove", SWT.PUSH)
						.addListener(SWT.Selection, new Listener() {

							@Override
							public void handleEvent(Event event) {
								log.debug("Remove requested for {}", p);
								try {
									if (pluginsService.uninstall(
											p.getSymbolicName(),
											p.getInstalledVersion(), null)) {

										pluginChanged(p,
												"Removed, please restart");
										PluginManagerDialog.restartNecessary();
										PluginManagerDialog.reloadPlugins();
									}
								} catch (Exception e) {
									log.debug("Removal failed", e);
									getMessageManager()
											.addMessage(
													"inst",
													"Removal failed: "
															+ e.getMessage(),
													null,
													IMessageProvider.ERROR);
								}
							}
						});
			}
		}
	}

	private FormToolkit getToolkit() {
		return mform.getToolkit();
	}

	private void clear() {
		for (Composite c : pluginComps.keySet()) {
			Plugin plugin = pluginComps.get(c);
			c.dispose();
			plugins.remove(plugin.getSymbolicName());
		}
	}

	private void pluginChanged(Plugin p2, String message) {
		changedPlugins.put(p2.getSymbolicName(), message);
	}

	protected class InstallOrUpdateButtonListener implements Listener {

		private final Plugin p;
		private String jobName;

		private InstallOrUpdateButtonListener(String jobName, Plugin p) {
			this.jobName = jobName;
			this.p = p;
		}

		@Override
		public void handleEvent(Event event) {

			new InstallOrUpdateJob(jobName, p).schedule();
		}
	}

	private final class InstallOrUpdateJob extends Job {
		private Plugin p;

		private InstallOrUpdateJob(String name, Plugin p) {
			super(name);
			this.p = p;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			PluginManagerDialog.backgroundJobStarting();
			monitor.beginTask(getName(), 100);
			try {
				log.debug("About to install {}", p);
				try {
					try {
						monitor.worked(5);
						pluginsService.installOrUpdatePlugin(p);
						monitor.worked(95);
					} catch (UserMessageException e) {
						throw e;
					} catch (Exception e) {
						log.error("Installation of plugin failed", e);
						throw new UserMessageException("Installation failed: "
								+ e.getMessage());
					}

					pluginChanged(p, "Installed, please restart");
					showInstallationSuccessfulMessage();

				} catch (final UserMessageException ume) {
					showInstallationErrorMessage(ume);
				}

			} finally {
				monitor.done();
				PluginManagerDialog.backgroundJobFinishing();
			}
			return Status.OK_STATUS;
		}

		protected void showInstallationErrorMessage(
				final UserMessageException ume) {
			Display.getDefault().asyncExec(new Runnable() {

				@Override
				public void run() {
					ErrorPopup.show("Installation of " + p.getName()
							+ " failed: " + ume.getMessage());
				}
			});
		}

		protected void showInstallationSuccessfulMessage() {
			Display.getDefault().asyncExec(new Runnable() {

				@Override
				public void run() {
					InformationPopup.show("Installation of " + p.getName()
							+ " successful.");
					PluginManagerDialog.reloadPlugins();

					PluginManagerDialog.restartNecessary();
				}
			});
		}
	}

}
