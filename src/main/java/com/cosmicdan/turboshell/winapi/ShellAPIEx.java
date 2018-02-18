package com.cosmicdan.turboshell.winapi;

/**
 * Extra constants for this Windows header that didn't yet exist in JNA's platform includes
 */
public interface ShellAPIEx {
	///////////////////
	// AppBar related
	///////////////////

	/** Notifies an appbar when a full-screen application is opening or closing. 1 for entering
	 * @see <a href="https://msdn.microsoft.com/en-us/library/windows/desktop/bb787965(v=vs.85).aspx">ABN_FULLSCREENAPP on MSDN</a> */
	int ABN_FULLSCREENAPP = 2;
}
