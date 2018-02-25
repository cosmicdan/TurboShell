package com.cosmicdan.turboshell.winapi;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;

/**
 * Extra constants for this Windows header that didn't yet exist in JNA's platform includes
 */
@SuppressWarnings("InterfaceNeverImplemented")
public interface WinUserEx extends WinUser {
	///////////////////
	// Extended Window Styles.
	// Full descriptions at https://msdn.microsoft.com/en-us/library/windows/desktop/ff700543(v=vs.85).aspx
	///////////////////

	/** Window does not become the foreground window when the user clicks it or minimizes/closes another window */
	long WS_EX_NOACTIVATE = 0x08000000;
	/** The window is intended to be used as a floating toolbar */
	long WS_EX_TOOLWINDOW = 0x00000080;
	/** The window should be placed above all non-topmost windows and should stay above them */
	long WS_EX_TOPMOST = 0x00000008;
	/** Forces a top-level window onto the taskbar when the window is visible. */
	long WS_EX_APPWINDOW = 0x00040000;



	///////////////////
	// hWndInsertAfter special values. Used in some functions like SetWindowPos.
	// Full descriptions at https://msdn.microsoft.com/en-us/library/windows/desktop/ms633545(v=vs.85).aspx
	///////////////////

	/** Places the window at the bottom of the Z order. Clears any existing 'always on top' status */
	HWND HWND_BOTTOM = new HWND(Pointer.createConstant(1));
	/** Places the window above all non-topmost windows (that is, behind all topmost windows) */
	HWND HWND_NOTOPMOST = new HWND(Pointer.createConstant(-2));
	/** Places the window at the top of the Z order. If topmost, will put above other topmost windows */
	HWND HWND_TOP = new HWND(Pointer.createConstant(0));
	/** Places the window above all non-topmost windows. Maintains topmost position even when deactivated */
	HWND HWND_TOPMOST = new HWND(Pointer.createConstant(-1));



	///////////////////
	// Object Identifiers. These identify categories of accessible objects within a window.
	// Full descriptions at https://msdn.microsoft.com/en-us/library/windows/desktop/dd373606(v=vs.85).aspx
	///////////////////
	/** The window itself rather than a child object. */
	long OBJID_WINDOW = 0x00000000;



	///////////////////
	// Window event constants.
	// Full descriptions at https://msdn.microsoft.com/en-us/library/windows/desktop/dd318066(v=vs.85).aspx
	///////////////////
	/**	The foreground window has changed */
	int EVENT_SYSTEM_FOREGROUND = 0x0003;
	/** An object has changed location, shape, or size */
	int EVENT_OBJECT_LOCATIONCHANGE = 0x800B;
	/** An object's Name property has changed */
	int EVENT_OBJECT_NAMECHANGE = 0x800C;
}
