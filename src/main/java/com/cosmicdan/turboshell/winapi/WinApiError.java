package com.cosmicdan.turboshell.winapi;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import lombok.extern.log4j.Log4j2;

/**
 * An exception we manually throw when a native call isn't an expected result
 */
@SuppressWarnings("UncheckedExceptionClass")
@Log4j2
public class WinApiError extends RuntimeException {
	public WinApiError(String msg) {
		super(msg, new LastErrorException(Native.getLastError()));
	}
}
