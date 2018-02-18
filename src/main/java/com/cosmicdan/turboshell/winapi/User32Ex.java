package com.cosmicdan.turboshell.winapi;

import com.sun.jna.Callback;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.W32APIOptions;
import lombok.extern.log4j.Log4j2;

/**
 * Contains User32 methods as a direct mapping instead of JNA's default interface mapping for performance
 */
@SuppressWarnings("UnusedReturnValue")
@Log4j2
public class User32Ex {
	public static final User32Ex INSTANCE;

	static {
		INSTANCE = new User32Ex();
		NativeLibrary user32 = NativeLibrary.getInstance("user32", W32APIOptions.DEFAULT_OPTIONS);
		Native.register(user32);
	}

	/** See {@link User32#FindWindow} */
	public native WinDef.HWND FindWindow(String lpClassName, String lpWindowName);
	/** See {@link User32#SetWindowLongPtr} */
	public native Pointer SetWindowLongPtr(WinDef.HWND hWnd, int nIndex, Pointer dwNewLongPtr);
	/** SetWindowLongPtr variant specifically for setting window callbacks. See {@link User32#SetWindowLongPtr} */
	public native BaseTSD.LONG_PTR SetWindowLongPtr(WinDef.HWND hWnd, int nIndex, Callback wndProc);
	/** See {@link User32#SetWindowPos} */
	public native boolean SetWindowPos(WinDef.HWND hWnd, WinDef.HWND hWndInsertAfter, int X, int Y, int cx, int cy, int uFlags);

	/**
	 * Passes message information to the specified window procedure.
	 * @param proc The previous window procedure. If this value is obtained by calling the GetWindowLong function with
	 *             the nIndex parameter set to GWL_WNDPROC or DWL_DLGPROC, it is actually either the address of a
	 *             window or dialog box procedure, or a special internal value meaningful only to CallWindowProc.
	 * @param hWnd A handle to the window procedure to receive the message.
	 * @param uMsg The message.
	 * @param wParam Additional message-specific information. The contents of this parameter depend on the value of the
	 *               uMsg parameter.
	 * @param lParam Additional message-specific information. The contents of this parameter depend on the value of the Msg parameter.
	 * @return The return value specifies the result of the message processing and depends on the message sent.
	 * @see <a href="https://msdn.microsoft.com/en-us/library/windows/desktop/ms633571(v=vs.85).aspx">CallWindowProc on MSDN</a>
	 */
	public native WinDef.LRESULT CallWindowProc(Pointer proc, WinDef.HWND hWnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam);

}
