package org.openlca.core.editors.process;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.DateFormatter;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.model.Actor;
import org.openlca.core.model.Process;
import org.openlca.core.model.Source;
import org.openlca.ui.IContentChangedListener;
import org.openlca.ui.UIFactory;
import org.openlca.ui.dnd.TextDropComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process page with administrative information.
 */
public class AdminInfoPage extends ModelEditorPage implements
		PropertyChangeListener {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Text accessAndUseRestrictionsText;
	private Button copyrightButton;
	private Text creationDateText;
	private TextDropComponent dataDocumentorDropComponent;
	private TextDropComponent dataGeneratorDropComponent;
	private TextDropComponent dataSetOwnerDropComponent;
	private Text intendedApplicationText;
	private Text lastChangeText;
	private Text projectText;
	private TextDropComponent publicationDropComponent;
	private Text versionText;

	public AdminInfoPage(ProcessEditor editor, final AdminInfo adminInfo) {
		super(editor, "AdminInfoPage", Messages.Processes_AdminInfoPageLabel);
		this.process = (Process) editor.getModelComponent();
		this.adminInfo = adminInfo;
		this.adminInfo.addPropertyChangeListener(this);
	}

	@Override
	protected void createContents(final Composite body,
			final FormToolkit toolkit) {
		final Section section = UIFactory.createSection(body, toolkit,
				Messages.Processes_AdminInfoPageLabel, true, false);

		final Composite composite = UIFactory.createSectionComposite(section,
				toolkit, UIFactory.createGridLayout(2));

		intendedApplicationText = UIFactory.createTextWithLabel(composite,
				toolkit, Messages.Processes_IntendedApplication, true);

		dataSetOwnerDropComponent = createDropComponent(composite, toolkit,
				Messages.Processes_DataSetOwner, adminInfo.getDataSetOwner(),
				Actor.class, false);

		dataGeneratorDropComponent = createDropComponent(composite, toolkit,
				Messages.Processes_DataGenerator, adminInfo.getDataGenerator(),
				Actor.class, false);

		dataDocumentorDropComponent = createDropComponent(composite, toolkit,
				Messages.Processes_DataDocumentor,
				adminInfo.getDataDocumentor(), Actor.class, false);

		publicationDropComponent = createDropComponent(composite, toolkit,
				Messages.Processes_Publication, adminInfo.getPublication(),
				Source.class, false);

		accessAndUseRestrictionsText = UIFactory.createTextWithLabel(composite,
				toolkit, Messages.Processes_AccessAndUseRestrictions, true);

		projectText = UIFactory.createTextWithLabel(composite, toolkit,
				Messages.Processes_Project, false);

		versionText = UIFactory.createTextWithLabel(composite, toolkit,
				Messages.Processes_Version, false);

		creationDateText = UIFactory.createTextWithLabel(composite, toolkit,
				Messages.Processes_CreationDate, false);
		creationDateText.setEditable(false);

		lastChangeText = UIFactory.createTextWithLabel(composite, toolkit,
				Messages.Processes_LastChange, false);
		lastChangeText.setEditable(false);

		copyrightButton = UIFactory.createButton(composite, toolkit,
				Messages.Processes_Copyright);
	}

	@Override
	protected String getFormTitle() {
		final String title = Messages.Processes_FormText
				+ ": "
				+ (process != null ? process.getName() != null ? process
						.getName() : "" : "");
		return title;
	}

	@Override
	protected void initListeners() {

		intendedApplicationText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				adminInfo.setIntendedApplication(intendedApplicationText
						.getText());
			}

		});

		accessAndUseRestrictionsText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				adminInfo
						.setAccessAndUseRestrictions(accessAndUseRestrictionsText
								.getText());
			}

		});

		projectText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				adminInfo.setProject(projectText.getText());
			}

		});

		versionText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				adminInfo.setVersion(versionText.getText());
			}

		});

		copyrightButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				// no action on default selection
			}

			@Override
			public void widgetSelected(final SelectionEvent e) {
				adminInfo.setCopyright(copyrightButton.getSelection());
			}
		});

		dataSetOwnerDropComponent
				.addContentChangedListener(new IContentChangedListener() {

					@Override
					public void contentChanged(final Control source,
							final Object content) {
						if (content != null) {
							if (adminInfo != null) {
								Actor dataSetOwner;
								try {
									dataSetOwner = getDatabase()
											.select(Actor.class,
													((IModelComponent) content)
															.getId());
									adminInfo.setDataSetOwner(dataSetOwner);
								} catch (final Exception e) {
									log.error(
											"Reading actor from database failed",
											e);
								}
							}
						} else {
							adminInfo.setDataSetOwner(null);
						}
					}

				});

		dataGeneratorDropComponent
				.addContentChangedListener(new IContentChangedListener() {

					@Override
					public void contentChanged(final Control source,
							final Object content) {
						if (content != null) {
							if (adminInfo != null) {
								Actor dataGenerator;
								try {
									dataGenerator = getDatabase()
											.select(Actor.class,
													((IModelComponent) content)
															.getId());
									adminInfo.setDataGenerator(dataGenerator);
								} catch (final Exception e) {
									log.error(
											"Reading actor from database failed",
											e);
								}
							}
						} else {
							adminInfo.setDataGenerator(null);
						}
					}

				});

		dataDocumentorDropComponent
				.addContentChangedListener(new IContentChangedListener() {

					@Override
					public void contentChanged(final Control source,
							final Object content) {
						if (content != null) {
							if (adminInfo != null) {
								Actor dataDocumentor;
								try {
									dataDocumentor = getDatabase()
											.select(Actor.class,
													((IModelComponent) content)
															.getId());
									adminInfo.setDataDocumentor(dataDocumentor);
								} catch (final Exception e) {
									log.error(
											"Reading actor from database failed",
											e);
								}
							}
						} else {
							adminInfo.setDataDocumentor(null);
						}
					}

				});

		publicationDropComponent
				.addContentChangedListener(new IContentChangedListener() {

					@Override
					public void contentChanged(final Control source,
							final Object content) {
						if (content != null) {
							if (adminInfo != null) {
								Source publication;
								try {
									publication = getDatabase()
											.select(Source.class,
													((IModelComponent) content)
															.getId());
									adminInfo.setPublication(publication);
								} catch (final Exception e) {
									log.error(
											"Reading actor from database failed",
											e);
								}
							}
						} else {
							adminInfo.setPublication(null);
						}
					}

				});
	}

	@Override
	protected void setData() {
		if (process != null) {
			if (adminInfo != null) {
				if (adminInfo.getIntendedApplication() != null) {
					intendedApplicationText.setText(adminInfo
							.getIntendedApplication());
				}
				if (adminInfo.getAccessAndUseRestrictions() != null) {
					accessAndUseRestrictionsText.setText(adminInfo
							.getAccessAndUseRestrictions());
				}
				if (adminInfo.getProject() != null) {
					projectText.setText(adminInfo.getProject());
				}
				if (adminInfo.getVersion() != null) {
					versionText.setText(adminInfo.getVersion());
				}
				if (adminInfo.getCreationDate() != null) {
					creationDateText.setText(DateFormatter
							.formatShort(adminInfo.getCreationDate()));
					creationDateText.setToolTipText(DateFormatter
							.formatLong(adminInfo.getCreationDate()));
				}
				if (adminInfo.getLastChange() != null) {
					lastChangeText.setText(DateFormatter.formatShort(adminInfo
							.getLastChange()));
					lastChangeText.setToolTipText(DateFormatter
							.formatLong(adminInfo.getLastChange()));
				}
				copyrightButton.setSelection(adminInfo.getCopyright());
			}
		}
	}

	@Override
	public void propertyChange(final PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("lastChange")) {
			if (lastChangeText != null) {
				lastChangeText.setText(DateFormatter.formatShort(adminInfo
						.getLastChange()));
				lastChangeText.setToolTipText(DateFormatter
						.formatLong(adminInfo.getLastChange()));
			}
		}
	}

}
