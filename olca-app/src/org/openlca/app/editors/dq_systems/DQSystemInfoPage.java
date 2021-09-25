package org.openlca.app.editors.dq_systems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.util.Controls;
import org.openlca.app.util.DQUI;
import org.openlca.app.util.UI;
import org.openlca.core.model.DQIndicator;
import org.openlca.core.model.DQScore;
import org.openlca.core.model.DQSystem;

class DQSystemInfoPage extends ModelPage<DQSystem> {

	private FormToolkit toolkit;
	private ScrolledForm form;
	private Composite body;
	private Section indicatorSection;
	private Section uncertaintySection;
	private Map<Integer, Text> indicatorTexts = new HashMap<>();
	private Map<Integer, Text> scoreTexts = new HashMap<>();

	DQSystemInfoPage(DQSystemEditor editor) {
		super(editor, "DQSystemInfoPage", M.GeneralInformation);
	}

	void redraw() {
		indicatorTexts.clear();
		indicatorSection.dispose();
		if (uncertaintySection != null)
			uncertaintySection.dispose();
		createAdditionalInfo(body);
		form.reflow(true);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(this);
		toolkit = managedForm.getToolkit();
		body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		dropComponent(infoSection.composite(), M.Source, "source");
		createAdditionalInfo(body);
		body.setFocus();
		form.reflow(true);
	}

	private void createAdditionalInfo(Composite body) {
		Collections.sort(getModel().indicators);
		for (DQIndicator indicator : getModel().indicators) {
			Collections.sort(indicator.scores);
		}
		indicatorSection = UI.section(body, toolkit, M.IndicatorsScores);
		CommentAction.bindTo(indicatorSection, "indicators", getEditor().getComments());
		Composite indicatorClient = UI.sectionClient(indicatorSection, toolkit, 1);
		createIndicatorMatrix(indicatorClient);
		if (!getModel().hasUncertainties)
			return;
		uncertaintySection = UI.section(body, toolkit, M.Uncertainties);
		Composite uncertaintyClient = UI.sectionClient(uncertaintySection, toolkit, 1);
		createUncertaintyMatrix(uncertaintyClient);
	}

	private void createIndicatorMatrix(Composite composite) {
		UI.gridLayout(composite, 2 * getModel().getScoreCount() + 3);
		createHeader(composite, true);
		createAddScoreButton(composite);
		for (DQIndicator indicator : getModel().indicators) {
			Text nameText = createTextCell(composite, 1, 15);
			((GridData) nameText.getLayoutData()).verticalAlignment = SWT.TOP;
			getBinding().onString(() -> indicator, "name", nameText);
			indicatorTexts.put(indicator.position, nameText);
			commentControl(composite, "indicators[" + indicator.position + "]");
			for (DQScore score : indicator.scores) {
				Text descriptionText = createTextCell(composite, 8, 8);
				getBinding().onString(() -> score, "description", descriptionText);
				descriptionText.setBackground(DQUI.getColor(score.position, getModel().getScoreCount()));
				commentControl(composite, "indicators[" + indicator.position + "].scores[" + score.position
						+ "].description");
			}
			createRemoveIndicatorButton(composite, indicator.position);
		}
		createAddIndicatorButton(composite);
		for (int i = 1; i <= getModel().getScoreCount(); i++) {
			UI.filler(composite);
			createRemoveScoreButton(composite, i);
		}
	}

	private void createUncertaintyMatrix(Composite composite) {
		UI.gridLayout(composite, 2 * getModel().getScoreCount() + 1);
		createHeader(composite, false);
		for (DQIndicator indicator : getModel().indicators) {
			String name = indicator.name != null ? indicator.name : "";
			Label label = toolkit.createLabel(composite, name);
			label.setToolTipText(name);
			setGridData(label, 1, 15);
			Text indicatorText = indicatorTexts.get(indicator.position);
			indicatorText.addModifyListener((e) -> {
				label.setText(indicatorText.getText());
				label.setToolTipText(indicatorText.getText());
			});
			for (DQScore score : indicator.scores) {
				Text uncertaintyText = createTextCell(composite, 1, 8);
				getBinding().onDouble(() -> score, "uncertainty", uncertaintyText);
				commentControl(composite, "indicators[" + indicator.position + "].scores[" + score.position
						+ "].uncertainty");
			}
		}
	}

	private void createHeader(Composite composite, boolean editable) {
		UI.filler(composite);
		if (editable) {
			UI.filler(composite);
		}
		for (int i = 1; i <= getModel().getScoreCount(); i++) {
			String scoreLabel = getModel().getScoreLabel(i);
			if (scoreLabel == null) {
				scoreLabel = Integer.toString(i);
			}
			if (editable) {
				Text labelText = createTextCell(composite, 1, 8);
				labelText.setText(scoreLabel);
				int pos = i;
				labelText.addModifyListener((e) -> {
					getModel().setScoreLabel(pos, labelText.getText());
					getEditor().setDirty(true);
				});
				scoreTexts.put(i, labelText);
				commentControl(composite, "scores[" + i + "]");
			} else {
				Label label = UI.formLabel(composite, scoreLabel);
				label.setToolTipText(scoreLabel);
				setGridData(label, 1, 8);
				((GridData) label.getLayoutData()).horizontalAlignment = SWT.CENTER;
				Text scoreText = scoreTexts.get(i);
				scoreText.addModifyListener((e) -> {
					label.setText(scoreText.getText());
					label.setToolTipText(scoreText.getText());
				});
				UI.filler(composite);
			}
		}
	}

	private Text createTextCell(Composite composite, int heightFactor, int widthFactor) {
		Text text = toolkit.createText(composite, null, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		text.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
					e.doit = true;
				}
			}
		});
		setGridData(text, heightFactor, widthFactor);
		return text;
	}

	private void setGridData(Control control, int heightFactor, int widthFactor) {
		GC gc = new GC(control);
		try {
			gc.setFont(control.getFont());
			FontMetrics fm = gc.getFontMetrics();
			GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			gd.heightHint = heightFactor * fm.getHeight();
			gd.widthHint = widthFactor * fm.getHeight();
			control.setLayoutData(gd);
		} finally {
			gc.dispose();
		}
	}

	private void createAddScoreButton(Composite parent) {
		Button button = toolkit.createButton(parent, M.AddScore, SWT.NONE);
		if (getModel().indicators.size() == 0) {
			button.setEnabled(false);
			return;
		}
		Controls.onSelect(button, (e) -> {
			int newScore = getModel().getScoreCount() + 1;
			for (DQIndicator indicator : getModel().indicators) {
				DQScore score = new DQScore();
				score.position = newScore;
				score.label = "Score " + newScore;
				score.description = indicator.name + " - score " + newScore;
				indicator.scores.add(score);
			}
			getEditor().setDirty(true);
			redraw();
		});
	}

	private void createRemoveScoreButton(Composite parent, int position) {
		Button button = toolkit.createButton(parent, M.RemoveScore, SWT.NONE);
		Controls.onSelect(button, (e) -> {
			for (DQIndicator indicator : getModel().indicators) {
				for (DQScore score : new ArrayList<>(indicator.scores)) {
					if (score.position < position)
						continue;
					if (score.position == position) {
						indicator.scores.remove(score);
						continue;
					}
					score.position--;
				}
			}
			getEditor().setDirty(true);
			redraw();
		});
	}

	private void createAddIndicatorButton(Composite parent) {
		Button button = toolkit.createButton(parent, M.AddIndicator, SWT.NONE);
		Controls.onSelect(button, (e) -> {
			DQIndicator indicator = new DQIndicator();
			indicator.name = "Indicator " + (getModel().indicators.size() + 1);
			indicator.position = getModel().indicators.size() + 1;
			for (int i = 1; i <= getModel().getScoreCount(); i++) {
				DQScore score = new DQScore();
				score.position = i;
				score.label = getModel().getScoreLabel(i);
				score.description = indicator.name + " - score " + i;
				indicator.scores.add(score);
			}
			getModel().indicators.add(indicator);
			getEditor().setDirty(true);
			redraw();
		});
	}

	private void createRemoveIndicatorButton(Composite parent, int position) {
		Button button = toolkit.createButton(parent, M.RemoveIndicator, SWT.NONE);
		Controls.onSelect(button, (e) -> {
			for (DQIndicator indicator : new ArrayList<>(getModel().indicators)) {
				if (indicator.position < position)
					continue;
				if (indicator.position == position) {
					getModel().indicators.remove(indicator);
					continue;
				}
				indicator.position--;
			}
			getEditor().setDirty(true);
			redraw();
		});
	}

	private void commentControl(Composite parent, String path) {
		if (getEditor().hasComment(path)) {
			new CommentControl(parent, toolkit, path, getEditor().getComments());
		} else {
			UI.filler(parent);
		}
	}

}
