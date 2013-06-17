/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.navigation;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.database.IDatabaseServer;
import org.openlca.core.jobs.Jobs;
import org.openlca.ui.JobListenerWithProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Navigation element for data providers.
 */
public class DataProviderNavigationElement extends AbstractNavigationElement {

	private IDatabaseServer dataProvider;
	private Logger log = LoggerFactory.getLogger(this.getClass());

	public DataProviderNavigationElement(IDatabaseServer dataProvider) {
		super();
		this.dataProvider = dataProvider;
	}

	@Override
	protected void refresh() {
		List<INavigationElement> elements = new ArrayList<>();
		if (dataProvider.isRunning()) {
			new DatabaseLoader(elements).run();
			log.trace("{} databases found", elements.size());
		}
		synchronize(elements);
	}

	@Override
	public Object getData() {
		return dataProvider;
	}

	@Override
	public INavigationElement getParent() {
		return null;
	}

	private class DatabaseLoader extends JobListenerWithProgress {

		private List<INavigationElement> elements;

		private DatabaseLoader(List<INavigationElement> elements) {
			super();
			this.elements = elements;
		}

		@Override
		public void run() {
			Jobs.getHandler(Jobs.MAIN_JOB_HANDLER).addJobListener(this);
			addDatabases();
			Jobs.getHandler(Jobs.MAIN_JOB_HANDLER).removeJobListener(this);
		}

		private void addDatabases() {
			// TODO: add database to navigation
			// try {
			// for (IDatabase database : dataProvider.getConnectedDatabases()) {
			// INavigationElement element = new DatabaseNavigationElement(
			// database, DataProviderNavigationElement.this);
			// elements.add(element);
			// }
			// } catch (Exception e) {
			// log.error("Cannot connect to databases", e);
			// }
		}
	}

}
