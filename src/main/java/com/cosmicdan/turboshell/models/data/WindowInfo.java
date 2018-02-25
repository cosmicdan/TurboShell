package com.cosmicdan.turboshell.models.data;

import com.cosmicdan.turboshell.winapi.User32Ex;
import com.cosmicdan.turboshell.winapi.WinUserEx;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import lombok.extern.log4j.Log4j2;

/**
 * A holder/wrapper for window styles, title name, and other interesting information
 * @author Daniel 'CosmicDan' Connolly
 */
@Log4j2
public class WindowInfo {
	private final WinDef.HWND mHWnd;
	private final WindowProperties mWindowProps;
	private String mTitle;

	public WindowInfo(WinDef.HWND hWnd) {
		mHWnd = hWnd;
		mWindowProps = new WindowProperties();
	}

	public WinDef.HWND getHWnd() {
		return mHWnd;
	}

	public WindowProperties getStyleInfo() {
		return mWindowProps;
	}

	public String getTitle() {
		return getTitle(false);
	}

	public String getTitle(final boolean skipCache) {
		if ((null == mTitle) || skipCache) {
			// get the title for the new window
			final int titleLength = User32Ex.INSTANCE.GetWindowTextLength(mHWnd) + 1;
			final char[] title = new char[titleLength];
			final int length = User32Ex.INSTANCE.GetWindowText(mHWnd, title, title.length);
			String windowTitle = "[No title/process]";
			if (0 < length)
				windowTitle = new String(title);
			// TODO: else set process name to title?
			//log.info("Title refresh to '" + windowTitle + "'");
			if (!skipCache)
				mTitle = windowTitle;
			else
				return windowTitle;
		}
		return mTitle;
	}

	public void setTitle(String newTitle) {
		mTitle = newTitle;
	}

	public class WindowProperties {
		private final long styleFlags;
		private final long styleExFlags;

		public WindowProperties() {
			styleFlags = User32Ex.INSTANCE.GetWindowLongPtr(mHWnd, WinUserEx.GWL_STYLE).longValue();
			styleExFlags = User32Ex.INSTANCE.GetWindowLongPtr(mHWnd, WinUserEx.GWL_EXSTYLE).longValue();
		}

		/**
		 * Determine if a window is "real". We consider a window real if it exists on the taskbar.
		 * References:
		 * https://msdn.microsoft.com/en-us/library/windows/desktop/cc144179%28v=vs.85%29.aspx#Managing_Taskbar_But
		 * https://stackoverflow.com/questions/16973995/
		 * https://stackoverflow.com/questions/2262726/
		 */
		public boolean isReal() {
			boolean isReal = false;
			if (!hasExStyle(WinUserEx.WS_EX_TOOLWINDOW)) {
				if (isTopWindow()) {
					if (hasStyle(WinUserEx.WS_EX_APPWINDOW))
						isReal = true;
					else if (!hasParentWindow() && !hasExStyle(WinUserEx.WS_EX_NOACTIVATE))
						isReal = true;
				}
			}
			return isReal;
		}

		// If I ever need to detect if a window appears on the taskbar, see:
		// https://msdn.microsoft.com/en-us/library/windows/desktop/cc144179(v=vs.85).aspx#Managing_Taskbar_But
		// https://stackoverflow.com/questions/2262726/determining-if-a-window-has-a-taskbar-button

		public boolean canResize() {
			return WinUserEx.WS_SIZEBOX == (styleFlags & WinUserEx.WS_SIZEBOX);
		}

		public boolean hasResizeButton() {
			return WinUserEx.WS_MAXIMIZEBOX == (styleFlags & WinUserEx.WS_MAXIMIZEBOX);
		}

		public boolean hasMinimizeButton() {
			return WinUserEx.WS_MINIMIZEBOX == (styleFlags & WinUserEx.WS_MINIMIZEBOX);
		}

		private boolean isTopWindow() {
			return mHWnd.equals(getTopWindow());
		}

		private WinDef.HWND getTopWindow() {
			return User32Ex.INSTANCE.GetAncestor(mHWnd, WinUser.GA_ROOT);
		}

		private boolean hasParentWindow() {
			return mHWnd.equals(getParentWindow());
		}

		private WinDef.HWND getParentWindow() {
			return User32Ex.INSTANCE.GetWindow(mHWnd, WinUser.GW_OWNER);
		}

		private boolean hasStyle(long windowStyle) {
			return windowStyle == (styleFlags & windowStyle);
		}

		private boolean hasExStyle(long windowExStyle) {
			return windowExStyle == (styleExFlags & windowExStyle);
		}
	}
}
