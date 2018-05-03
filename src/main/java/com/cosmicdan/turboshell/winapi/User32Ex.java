package com.cosmicdan.turboshell.winapi;

import com.sun.jna.Callback;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD.LONG_PTR;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.win32.W32APIOptions;
import lombok.extern.log4j.Log4j2;

/**
 * Contains User32 methods as a direct mapping instead of JNA's default interface mapping for performance
 */
@SuppressWarnings({"UnusedReturnValue", "MethodWithTooManyParameters", "BooleanMethodNameMustStartWithQuestion", "NativeMethod",
		"Singleton"})
@Log4j2
public final class User32Ex {
	public static final User32Ex INSTANCE;

	static {
		INSTANCE = new User32Ex();
		final NativeLibrary user32 = NativeLibrary.getInstance("user32", W32APIOptions.DEFAULT_OPTIONS);
		Native.register(user32);
	}

	private User32Ex() {}

	// generic window stuff
	/** See {@link User32#FindWindow} */
	public native HWND FindWindow(String lpClassName, String lpWindowName);
	/** See {@link User32#SetWindowLongPtr} */
	public native Pointer SetWindowLongPtr(HWND hWnd, int nIndex, Pointer dwNewLongPtr);
	/** See {@link User32#GetWindowLongPtr} */
	public native LONG_PTR GetWindowLongPtr(HWND hWnd, int nIndex);
	/** SetWindowLongPtr variant specifically for setting window callbacks. See {@link User32#SetWindowLongPtr} */
	public native LONG_PTR SetWindowLongPtr(HWND hWnd, int nIndex, Callback wndProc);
	/** See {@link User32#SetWindowPos} */
	public native boolean SetWindowPos(HWND hWnd, HWND hWndInsertAfter, int x, int y, int cx, int cy, int uFlags);
	/** See {@link User32#IsWindowVisible} */
	public native boolean IsWindowVisible(HWND hWnd);
	/** See {@link User32#GetAncestor} */
	public native HWND GetAncestor(HWND hwnd, int gaFlags);
	/** See {@link User32#GetWindow} */
	public native HWND GetWindow(HWND hWnd, int uCmd);
	/** Sets the show state of a window without waiting for the operation to complete. */
	public native BOOL ShowWindowAsync(HWND hWnd, int nCmdShow);
	/** See {@link User32#GetWindowRect} */
	public native boolean GetWindowRect(HWND hWnd, RECT rect);
	/** See {@link User32#GetForegroundWindow} */
	public native HWND GetForegroundWindow();
	/** See {@link User32#GetClassName} */
	public native int GetClassName(HWND hWnd, char[] lpClassName, int nMaxCount);


	// Callback/Window message related stuff
	/** See {@link User32#GetMessage} */
	public native int GetMessage(MSG lpMsg, HWND hWnd, int wMsgFilterMin, int wMsgFilterMax);
	/** See {@link User32#TranslateMessage} */
	public native boolean TranslateMessage(MSG lpMsg);
	/** See {@link User32#DispatchMessage} */
	public native LRESULT DispatchMessage(MSG lpMsg);

	// For getting window titles
	/** See {@link User32#GetWindowTextLength} */
	public native int GetWindowTextLength(HWND hWnd);
	/** See {@link User32#GetWindowText} */
	public native int GetWindowText(HWND hWnd, char[] lpString, int nMaxCount);





	/**
	 * Passes message information to the specified window procedure.
	 * @param proc The previous window procedure. If this value is obtained by calling the GetWindowLong function with
	 *             the nIndex parameter set to GWL_WNDPROC or DWL_DLGPROC, it is actually either the address of a
	 *             window or dialog box procedure, or a special internal value meaningful only to CallWindowProc.
	 * @param hWnd A handle to the window procedure to receive the message.
	 * @param uMsg The message.
	 * @param wParam Additional message-specific information. The contents of this parameter depend on the value of the
	 *               uMsg parameter.
	 * @param lParam Additional message-specific information. The contents of this parameter depend on the value of
	 *               the Msg parameter.
	 * @return The return value specifies the result of the message processing and depends on the message sent.
	 * @see <a href="https://msdn.microsoft.com/en-us/library/windows/desktop/ms633571(v=vs.85).aspx">CallWindowProc on
	 * MSDN</a>
	 */
	public native LRESULT CallWindowProc(Pointer proc, HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam);

}
