package com.cosmicdan.turboshell.models.data;

import com.cosmicdan.turboshell.winapi.User32Ex;
import com.cosmicdan.turboshell.winapi.WinUserEx;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import lombok.extern.log4j.Log4j2;

import java.util.EnumSet;

/**
 * A holder/wrapper for window styles, title name, and other interesting information
 * @author Daniel 'CosmicDan' Connolly
 */
@SuppressWarnings("WeakerAccess")
@Log4j2
public class WindowInfo {
	public static final String NO_TITLE = "[NO TITLE]";

	public enum Flag {
		IS_MAXIMIZED,
		IS_MAXIMIZABLE,
		IS_MINIMIZABLE
	}

	private final HWND mHWnd;
	private final long styleFlags;
	private final long styleExFlags;

	private String mTitle = null;

	public WindowInfo(final HWND hWnd) {
		mHWnd = User32Ex.INSTANCE.GetAncestor(hWnd, WinUser.GA_ROOTOWNER);
		styleFlags = User32Ex.INSTANCE.GetWindowLongPtr(mHWnd, WinUser.GWL_STYLE).longValue();
		styleExFlags = User32Ex.INSTANCE.GetWindowLongPtr(mHWnd, WinUser.GWL_EXSTYLE).longValue();
	}

	public final HWND getHWnd() {
		return mHWnd;
	}

	public String getTitle() {
		if (null == mTitle) {
			// get the title for the new window
			final int titleLength = User32Ex.INSTANCE.GetWindowTextLength(mHWnd) + 1;
			final char[] title = new char[titleLength];
			final int length = User32Ex.INSTANCE.GetWindowText(mHWnd, title, title.length);
			String windowTitle = NO_TITLE;
			if (0 < length)
				windowTitle = new String(title);
			if (null == mTitle)
				mTitle = windowTitle;
		}
		return mTitle;
	}

	/**
	 * Determine if a window is "real". We consider a window real if it probably exists on the taskbar.
	 * References:
	 * https://msdn.microsoft.com/en-us/library/windows/desktop/cc144179%28v=vs.85%29.aspx#Managing_Taskbar_But
	 * https://stackoverflow.com/questions/16973995/
	 * https://stackoverflow.com/questions/2262726/
	 */
	public final boolean isRealWindow() {
		boolean isReal = false;
		if (hasStyle(WinUser.WS_VISIBLE) && hasStyle(WinUserEx.WS_EX_APPWINDOW) && hasTitle()) {
			if (!(hasExStyle(WinUserEx.WS_EX_TOOLWINDOW) || hasExStyle(WinUserEx.WS_EX_NOACTIVATE))) {
				isReal = true;
			}
		}
		return isReal;
	}

	public final boolean canResize() {
		return hasStyle(WinUser.WS_SIZEBOX);
	}

	public final boolean hasResizeButton() {
		return hasStyle(WinUser.WS_MAXIMIZEBOX);
	}

	public final boolean isMaximized() {
		return hasStyle(WinUser.WS_MAXIMIZE);
	}

	public EnumSet<Flag> getFlags() {
		final EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
		if (isMaximized())
			flags.add(Flag.IS_MAXIMIZED);
		if (canMaximize())
			flags.add(Flag.IS_MAXIMIZABLE);
		if (hasMinimizeButton())
			flags.add(Flag.IS_MINIMIZABLE);
		return flags;
	}

	public boolean isVisible() {
		return User32Ex.INSTANCE.IsWindowVisible(mHWnd);
	}

	private boolean hasMinimizeButton() {
		return hasStyle(WinUser.WS_MINIMIZEBOX);
	}

	private boolean canMaximize() {
		return (canResize() && hasResizeButton());
	}
/*
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
*/
	private boolean hasStyle(final long windowStyle) {
		return windowStyle == (styleFlags & windowStyle);
	}

	private boolean hasExStyle(final long windowExStyle) {
		return windowExStyle == (styleExFlags & windowExStyle);
	}

	private boolean hasTitle() {
		return !NO_TITLE.equals(mTitle);
	}
}
