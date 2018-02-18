package com.cosmicdan.turboshell.winapi;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

/**
 * Extra constants for this Windows header that didn't yet exist in JNA's platform includes
 */
public interface WinUserEx extends WinUser {
	///////////////////
	// Extended Window Styles.
	// Full descriptions at https://msdn.microsoft.com/en-us/library/windows/desktop/ff700543(v=vs.85).aspx
	///////////////////

	/** Window does not become the foreground window when the user clicks it or minimizes/closes another window */
	int WS_EX_NOACTIVATE = 0x08000000;
	/** The window is intended to be used as a floating toolbar */
	int WS_EX_TOOLWINDOW = 0x00000080;
	/** The window should be placed above all non-topmost windows and should stay above them */
	int WS_EX_TOPMOST = 0x00000008;



	///////////////////
	// hWndInsertAfter special values. Used in some functions like SetWindowPos.
	// Full descriptions at https://msdn.microsoft.com/en-us/library/windows/desktop/ms633545(v=vs.85).aspx
	///////////////////

	/** Places the window at the bottom of the Z order. Clears any existing 'always on top' status */
	WinDef.HWND HWND_BOTTOM = new WinDef.HWND(Pointer.createConstant(1));
	/** Places the window above all non-topmost windows (that is, behind all topmost windows) */
	WinDef.HWND HWND_NOTOPMOST = new WinDef.HWND(Pointer.createConstant(-2));
	/** Places the window at the top of the Z order. If topmost, will put above other topmost windows */
	WinDef.HWND HWND_TOP = new WinDef.HWND(Pointer.createConstant(0));
	/** Places the window above all non-topmost windows. Maintains topmost position even when deactivated */
	WinDef.HWND HWND_TOPMOST = new WinDef.HWND(Pointer.createConstant(-1));
}
