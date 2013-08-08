package org.openlca.core.editors.process;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.Messages;
import org.openlca.core.model.Time;
import org.openlca.ui.UIFactory;

class ProcessTimeSection {

	private Time time;
	private DateTime startDate;
	private DateTime endDate;
	private Text commentText;
	private FormToolkit toolkit;
	private Composite composite;

	public ProcessTimeSection(Time time) {
		this.time = time;
	}

	public Section createSection(FormToolkit toolkit, Composite parent) {
		this.toolkit = toolkit;
		Section section = UIFactory.createSection(parent, toolkit,
				Messages.Processes_TimeInfoSectionLabel, true, false);
		composite = UIFactory.createSectionComposite(section, toolkit,
				UIFactory.createGridLayout(2));
		startDate = createDateTime(Messages.Processes_StartDate);
		endDate = createDateTime(Messages.Processes_EndDate);
		commentText = UIFactory.createTextWithLabel(composite, toolkit,
				Messages.Processes_Comment, true);
		initListeners();
		setData();
		return section;
	}

	private DateTime createDateTime(String label) {
		toolkit.createLabel(composite, label, SWT.NONE);
		DateTime dateTime = new DateTime(composite, SWT.DATE | SWT.DROP_DOWN);
		GridData data = new GridData();
		data.widthHint = 150;
		dateTime.setLayoutData(data);
		return dateTime;
	}

	private void initListeners() {
		startDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				time.setStartDate(fetchDate(startDate));
			}
		});
		endDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				time.setEndDate(fetchDate(endDate));
			}
		});
		commentText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				time.setComment(commentText.getText());
			}
		});
	}

	private Date fetchDate(DateTime dateTime) {
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(Calendar.DAY_OF_MONTH, dateTime.getDay());
		calendar.set(Calendar.YEAR, dateTime.getYear());
		calendar.set(Calendar.MONTH, dateTime.getMonth());
		return calendar.getTime();
	}

	private void setData() {
		if (time == null)
			return;
		setDate(startDate, time.getStartDate());
		setDate(endDate, time.getEndDate());
		if (time.getComment() != null)
			commentText.setText(time.getComment());
	}

	private void setDate(DateTime dateTime, Date date) {
		if (date == null || dateTime == null)
			return;
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		dateTime.setDay(calendar.get(Calendar.DAY_OF_MONTH));
		dateTime.setMonth(calendar.get(Calendar.MONTH));
		dateTime.setYear(calendar.get(Calendar.YEAR));
	}

}
