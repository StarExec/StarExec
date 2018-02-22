package org.starexec.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * Exceptions that can be thrown from JAX-RS methods.
 * Right now, so as to keep pervious behavior, all exceptions produce the same
 * error message with an HTTP status of 200 (sucess). However, by making these
 * seperate messages, it gives us the flexibility to return more syntactically
 * correct error messages in the future.
 */
public class RESTException extends WebApplicationException {
	private RESTException() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Cannot instantiate class because it is static.");
	}

	private RESTException(Status status, String message) {
		super(buildResponse(status, message));
	}

	private static Response buildResponse(Status status, String message) {
		ResponseBuilder rb = Response.status(status);
		rb.type("text/plain");
		rb.entity(message);
		return rb.build();
	}

	public static final RESTException NOT_FOUND = new RESTException(Status.OK, "not available");
	public static final RESTException FORBIDDEN = new RESTException(Status.OK, "not available");
	public static final RESTException INTERNAL_SERVER_ERROR = new RESTException(Status.OK, "not available");
}
