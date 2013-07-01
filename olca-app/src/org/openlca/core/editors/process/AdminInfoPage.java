package org.openlca.core.editors.process;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.DateFormatter;
import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.editors.ModelEditorPage;
import org.openlca.core.model.Actor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.core.model.Source;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.ui.UIFactory;
import org.openlca.ui.dnd.ISingleModelDrop;
import org.openlca.ui.dnd.TextDropComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process page with administrative information.
 */
public class AdminInfoPage extends ModelEditorPage implements
		PropertyChangeListener {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private ProcessDocumentation doc;
	private Process process;
	private Text restrictionsText;
	private Button copyrightButton;
	private Text creationDateText;
	private TextDropComponent documentorDrop;
	private TextDropComponent generatorDrop;
	private TextDropComponent ownerDrop;
	private Text intendedApplicationText;
	private Text lastChangeText;
	private Text projectText;
	private TextDropComponent publicationDrop;
	private Text versionText;

	public AdminInfoPage(ProcessEditor editor, Process process) {
		super(editor, "AdminInfoPage", Messages.Processes_AdminInfoPageLabel);
		this.doc = process.getDocumentation();
		this.process = process;
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

		ownerDrop = UIFactory.createDropComponent(composite,
				Messages.Processes_DataSetOwner, toolkit, false,
				ModelType.ACTOR);
		ownerDrop.setContent(Descriptors.toDescriptor(doc.getDataSetOwner()));

		generatorDrop = UIFactory.createDropComponent(composite,
				Messages.Processes_DataGenerator, toolkit, false,
				ModelType.ACTOR);
		generatorDrop.setContent(Descriptors.toDescriptor(doc
				.getDataGenerator()));

		documentorDrop = UIFactory.createDropComponent(composite,
				Messages.Processes_DataDocumentor, toolkit, false,
				ModelType.ACTOR);
		documentorDrop.setContent(Descriptors.toDescriptor(doc
				.getDataDocumentor()));

		publicationDrop = UIFactory.createDropComponent(composite,
				Messages.Processes_Publication, toolkit, false,
				ModelType.SOURCE);
		publicationDrop.setContent(Descriptors.toDescriptor(doc
				.getPublication()));

		restrictionsText = UIFactory.createTextWithLabel(composite, toolkit,
				Messages.Processes_AccessAndUseRestrictions, true);

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

		// TODO: use data binding here

		intendedApplicationText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				doc.setIntendedApplication(intendedApplicationText.getText());
			}
		});

		restrictionsText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				doc.setRestrictions(restrictionsText.getText());
			}
		});

		projectText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				doc.setProject(projectText.getText());
			}

		});

		versionText.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				doc.setVersion(versionText.getText());
			}

		});

		copyrightButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				// no action on default selection
			}

			@Override
			public void widgetSelected(final SelectionEvent e) {
				doc.setCopyright(copyrightButton.getSelection());
			}
		});

		ownerDrop.setHandler(new ISingleModelDrop() {
			@Override
			public void handle(BaseDescriptor descriptor) {
				if (descriptor == null)
					doc.setDataSetOwner(null);
				else
					try {
						Actor actor = Database.load(descriptor);
						doc.setDataSetOwner(actor);
					} catch (Exception e) {
						log.error("failed to load actor " + descriptor, e);
					}
			}
		});

		generatorDrop.setHandler(new ISingleModelDrop() {
			@Override
			public void handle(BaseDescriptor descriptor) {
				if (descriptor == null)
					doc.setDataGenerator(null);
				else
					try {
						Actor actor = Database.load(descriptor);
						doc.setDataGenerator(actor);
					} catch (Exception e) {
						log.error("failed to load actor " + descriptor, e);
					}
			}
		});

		publicationDrop.setHandler(new ISingleModelDrop() {
			@Override
			public void handle(BaseDescriptor descriptor) {
				if (descriptor == null)
					doc.setPublication(null);
				else
					try {
						Source source = Database.load(descriptor);
						doc.setPublication(source);
					} catch (Exception e) {
						log.error("failed to load actor " + descriptor, e);
					}
			}
		});

		documentorDrop.setHandler(new ISingleModelDrop() {
			@Override
			public void handle(BaseDescriptor descriptor) {
				if (descriptor == null)
					doc.setDataDocumentor(null);
				else
					try {
						Actor actor = Database.load(descriptor);
						doc.setDataDocumentor(actor);
					} catch (Exception e) {
						log.error("failed to load actor " + descriptor, e);
					}
			}
		});

	}

	@Override
	protected void setData() {
		if (process != null) {
			if (doc != null) {
				if (doc.getIntendedApplication() != null) {
					intendedApplicationText.setText(doc
							.getIntendedApplication());
				}
				if (doc.getRestrictions() != null) {
					restrictionsText.setText(doc.getRestrictions());
				}
				if (doc.getProject() != null) {
					projectText.setText(doc.getProject());
				}
				if (doc.getVersion() != null) {
					versionText.setText(doc.getVersion());
				}
				if (doc.getCreationDate() != null) {
					creationDateText.setText(DateFormatter.formatShort(doc
							.getCreationDate()));
					creationDateText.setToolTipText(DateFormatter
							.formatLong(doc.getCreationDate()));
				}
				if (doc.getLastChange() != null) {
					lastChangeText.setText(DateFormatter.formatShort(doc
							.getLastChange()));
					lastChangeText.setToolTipText(DateFormatter.formatLong(doc
							.getLastChange()));
				}
				copyrightButton.setSelection(doc.isCopyright());
			}
		}
	}

	@Override
	public void propertyChange(final PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("lastChange")) {
			if (lastChangeText != null) {
				lastChangeText.setText(DateFormatter.formatShort(doc
						.getLastChange()));
				lastChangeText.setToolTipText(DateFormatter.formatLong(doc
						.getLastChange()));
			}
		}
	}

}
