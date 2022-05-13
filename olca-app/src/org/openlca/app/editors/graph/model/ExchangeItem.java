package org.openlca.app.editors.graph.model;

import org.openlca.core.model.Exchange;

public class ExchangeItem extends GraphComponent {

	private final Exchange exchange;

	public ExchangeItem(Exchange exchange) {
		this.exchange = exchange;
	}

	public Exchange getExchange() {
		return exchange;
	}

}
