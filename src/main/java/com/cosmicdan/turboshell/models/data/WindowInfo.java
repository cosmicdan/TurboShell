package com.cosmicdan.turboshell.models.data;

import com.cosmicdan.turboshell.winapi.User32Ex;
import com.cosmicdan.turboshell.winapi.WinUserEx;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import lombok.extern.log4j.Log4j2;

/**
 * A holder/wrapper for window styles, title name, and other interesting information
 * @author Daniel 'CosmicDan' Connolly
 */
@Log4j2
public class WindowInfo {
	public enum Cache {USE, SKIP}

	private final HWND mHWnd;
	private final long styleFlags;
	private final long styleExFlags;

	private String mTitle = null;

	public WindowInfo(final HWND hWnd) {
		mHWnd = hWnd;
		styleFlags = User32Ex.INSTANCE.GetWindowLongPtr(mHWnd, WinUser.GWL_STYLE).longValue();
		styleExFlags = User32Ex.INSTANCE.GetWindowLongPtr(mHWnd, WinUser.GWL_EXSTYLE).longValue();
	}

	public final HWND getHWnd() {
		return mHWnd;
	}

	public final String getTitle() {
		return getTitle(Cache.USE);
	}

	public final String getTitle(final Cache cache) {
		String windowTitle = "[No title]";
		if ((null == mTitle) || (Cache.SKIP.equals(cache))) {
			// get the title for the new window
			final int titleLength = User32Ex.INSTANCE.GetWindowTextLength(mHWnd) + 1;
			final char[] title = new char[titleLength];
			final int length = User32Ex.INSTANCE.GetWindowText(mHWnd, title, title.length);
			if (0 < length)
				windowTitle = new String(title);
			// TODO: else set process name to title?
			//log.info("Title refresh to '" + windowTitle + "'");
			if (Cache.USE.equals(cache))
				mTitle = windowTitle;
		}
		return windowTitle;
	}

	public final void setTitle(final String newTitle) {
		mTitle = newTitle;
	}

	/**
	 * Determine if a window is "real". We consider a window real if it exists on the taskbar.
	 * References:
	 * https://msdn.microsoft.com/en-us/library/windows/desktop/cc144179%28v=vs.85%29.aspx#Managing_Taskbar_But
	 * https://stackoverflow.com/questions/16973995/
	 * https://stackoverflow.com/questions/2262726/
	 */
	public final boolean isRealWindow() {
		boolean isReal = false;
		if (!hasExStyle(WinUserEx.WS_EX_TOOLWINDOW)) {
			if (isTopWindow()) {
				if (hasStyle(WinUserEx.WS_EX_APPWINDOW) ||
						(!hasParentWindow() && !hasExStyle(WinUserEx.WS_EX_NOACTIVATE))) {
					isReal = true;
				}
			}
		}
		return isReal;
	}

	// If I ever need to detect if a window appears on the taskbar, see:
	// https://msdn.microsoft.com/en-us/library/windows/desktop/cc144179(v=vs.85).aspx#Managing_Taskbar_But
	// https://stackoverflow.com/questions/2262726/determining-if-a-window-has-a-taskbar-button

	public final boolean canResize() {
		return WinUser.WS_SIZEBOX == (styleFlags & WinUser.WS_SIZEBOX);
	}

	public final boolean hasResizeButton() {
		return WinUser.WS_MAXIMIZEBOX == (styleFlags & WinUser.WS_MAXIMIZEBOX);
	}

	public final boolean hasMinimizeButton() {
		return WinUser.WS_MINIMIZEBOX == (styleFlags & WinUser.WS_MINIMIZEBOX);
	}

	private boolean isTopWindow() {
		return mHWnd.equals(getTopWindow());
	}

	private HWND getTopWindow() {
		return User32Ex.INSTANCE.GetAncestor(mHWnd, WinUser.GA_ROOT);
	}

	private boolean hasParentWindow() {
		return mHWnd.equals(getParentWindow());
	}

	private HWND getParentWindow() {
		return User32Ex.INSTANCE.GetWindow(mHWnd, WinUser.GW_OWNER);
	}

	private boolean hasStyle(final long windowStyle) {
		return windowStyle == (styleFlags & windowStyle);
	}

	private boolean hasExStyle(final long windowExStyle) {
		return windowExStyle == (styleExFlags & windowExStyle);
	}
}
