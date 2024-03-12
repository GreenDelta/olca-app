package org.openlca.app.collaboration.util;

import org.openlca.collaboration.api.WebRequests.WebRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebRequests {

	private static final Logger log = LoggerFactory.getLogger(WebRequests.class);

	public static void execute(RequestWithoutResponse request) {
		try {
			request.execute();
		} catch (WebRequestException e) {
			log.error("");
		}
	}

	public static <T> T execute(RequestWithResponse<T> request) {
		return execute(request, null);
	}

	public static <T> T execute(RequestWithResponse<T> request, T defaultValue) {
		try {
			return request.execute();
		} catch (WebRequestException e) {
			log.error("");
			return defaultValue;
		}
	}

	public interface RequestWithoutResponse {

		void execute() throws WebRequestException;

	}

	public interface RequestWithResponse<T> {

		T execute() throws WebRequestException;

	}

}
