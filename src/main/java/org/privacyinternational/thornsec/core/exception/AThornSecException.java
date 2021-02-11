package org.privacyinternational.thornsec.core.exception;

public abstract class AThornSecException extends Exception {
	private static final long serialVersionUID = -428651404537657202L;

	public AThornSecException(String message) {
		super(message);
	}
}
