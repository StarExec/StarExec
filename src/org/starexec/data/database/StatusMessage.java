package org.starexec.data.database;

import org.starexec.logger.StarLogger;

import java.sql.SQLException;
import java.util.Optional;

public class StatusMessage {
	private static final StarLogger log = StarLogger.getLogger(StatusMessage.class);

	private StatusMessage() {} // Class is not instantiable

	public static void set(boolean enabled, String message, String url) throws SQLException {
		Common.update(
				"{CALL SetStatusMessage(?,?,?)}",
				procedure -> {
					procedure.setBoolean(1, enabled);
					procedure.setString(2, message);
					procedure.setString(3, url);
				}
		);
	}

	public static String getAsHtml() {
		final String html;
		try {
			html = Common.query(
					"{CALL GetStatusMessage()}",
					procedure -> {},
					results -> {
						results.next();
						if (results.getBoolean("enabled")) {
							final String message = results.getString("message");
							final String url = results.getString("url");
							return "<div class='status-message'><p>"
							     + message
							     + "<a href='" + url + "'>More Information</a>"
							     + "</p></div>"
							;
						} else {
							return "";
						}
					}
			);
		} catch (SQLException e) {
			log.error("getAsHtml", e);
			return "";
		}
		return html;
	}
}
