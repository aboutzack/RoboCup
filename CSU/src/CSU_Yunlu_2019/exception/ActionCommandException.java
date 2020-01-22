package CSU_Yunlu_2019.exception;

import rescuecore2.standard.messages.StandardMessageURN;

/**
 * this exception used to make it possible to return to suitable method quickly
 * 
 * @author Frank
 *
 */
@SuppressWarnings("serial")
public class ActionCommandException extends Exception {
	private StandardMessageURN message;

	public StandardMessageURN message() {
		return message;
	}

	public ActionCommandException(StandardMessageURN message) {
		super();
		this.message = message;
	}
}
