package org.openlca.app.editors.systems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Processes;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ParameterClipboard {

	private ParameterClipboard() {
	}

	static List<ParameterRedef> read(String text) {
		if (text == null)
			return Collections.emptyList();
		String[] lines = text.toString().split("\n");
		List<ParameterRedef> list = new ArrayList<>();
		Mapper mapper = new Mapper();
		for (String line : lines) {
			String[] row = line.split("\t");
			for (int k = 0; k < row.length; k++) {
				row[k] = row[k].trim();
			}
			if (row.length > 2 && Strings.nullOrEqual(row[2], M.Amount))
				continue; // the header row
			ParameterRedef p = mapper.doIt(row);
			if (p != null) {
				list.add(p);
			}
		}
		return list;
	}

	private static class Mapper {

		final Logger log = LoggerFactory.getLogger(getClass());

		ParameterRedef doIt(String[] row) {
			ParameterRedef redef = init(row);
			if (redef == null)
				return null;
			if (row.length > 2) {
				try {
					redef.setValue(Double.parseDouble(row[2]));
				} catch (Exception e) {
					log.warn("Parameter redef. value is not numeric");
				}
			}
			if (row.length > 3) {
				redef.setUncertainty(Uncertainty.fromString(row[3]));
			}
			return redef;
		}

		private ParameterRedef init(String[] row) {
			if (row == null || row.length < 2)
				return null;
			String cname = row[0];
			if (Strings.nullOrEmpty(cname)) {
				log.warn("No parameter context given");
				return null;
			}
			ParameterRedef p = new ParameterRedef();
			p.setName(row[1]);
			if (Strings.nullOrEqual(cname, "global"))
				return p;
			ProcessDescriptor process = findProcess(cname);
			if (process == null)
				return null;
			p.setContextId(process.getId());
			p.setContextType(ModelType.PROCESS);
			return p;
		}

		private ProcessDescriptor findProcess(String fullName) {
			if (Strings.nullOrEmpty(fullName))
				return null;
			ProcessDescriptor d = Processes.findForLabel(Database.get(), fullName);
			if (d == null) {
				log.warn("Could not find process '{}' in database", fullName);
			}
			return d;
		}
	}
}
