package org.starexec.data.database;

import org.starexec.logger.StarLogger;

import com.google.gson.*;
import java.sql.SQLException;
import java.util.Optional;

public class StatusMessage {
	private static final StarLogger log = StarLogger.getLogger(StatusMessage.class);
	private static final Gson gson = new GsonBuilder().create();

	private StatusMessage() {} // Class is not instantiable

	public static void set(boolean enabled, String message, String url) throws SQLException {
		Common.update(
				"{CALL SetStatusMessage(?,?,?)}",
				procedure -> {
					procedure.setBoolean(1, enabled);
					procedure.setString(2, message.trim());
					procedure.setString(3, url.trim());
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
							     + (
							        url.isEmpty() ? "" :
							        "<a href='" + url + "'>More Information</a>"
							       )
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

	public static String getAsJson() {
		try {
			return Common.query(
					"{CALL GetStatusMessage()}",
					procedure -> {},
					results -> {
						results.next();
						JsonObject json = new JsonObject();
						json.addProperty("enabled", results.getBoolean("enabled"));
						if (results.getBoolean("enabled")) {
							json.addProperty("message", results.getString("message"));
							json.addProperty("url",     results.getString("url"));
						}
						return gson.toJson(json);
					}
			);
		} catch (SQLException e) {
			log.error("getAsHtml", e);
			return "{}";
		}
	}
}
