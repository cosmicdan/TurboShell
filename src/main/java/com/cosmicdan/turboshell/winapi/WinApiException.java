package com.cosmicdan.turboshell.winapi;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import lombok.extern.log4j.Log4j2;

/**
 * An exception we manually throw when a native call isn't an expected result
 */
@Log4j2
public class WinApiException extends RuntimeException {
	public WinApiException(final String msg) {
		super(msg, new LastErrorException(Native.getLastError()));
	}
}
