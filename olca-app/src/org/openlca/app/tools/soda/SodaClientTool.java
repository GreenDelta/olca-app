package org.openlca.app.tools.soda;

public class SodaClientTool {

	public static void open() {
		var con = LoginDialog.show().orElse(null);
		if (con == null || con.hasError())
			return;
		for (var stock : con.stocks()) {
			System.out.println(stock.shortName);
		}
	}

}
