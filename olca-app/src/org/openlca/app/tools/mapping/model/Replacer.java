package org.openlca.app.tools.mapping.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Replacer implements Runnable {

	private final ReplacerConfig conf;
	private final Logger log = LoggerFactory.getLogger(getClass());

	public Replacer(ReplacerConfig conf) {
		this.conf = conf;
	}

	@Override
	public void run() {
		if (conf == null || (!conf.processes && !conf.methods)) {
			log.info("no configuration; nothing to replace");
			return;
		}
	}

}
