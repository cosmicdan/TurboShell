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

	public class WindowProperties {
		private final long styleFlags;
		private final long styleExFlags;

		public WindowProperties() {
			styleFlags = User32Ex.INSTANCE.GetWindowLongPtr(mHWnd, WinUserEx.GWL_STYLE).longValue();
			styleExFlags = User32Ex.INSTANCE.GetWindowLongPtr(mHWnd, WinUserEx.GWL_EXSTYLE).longValue();
		}

		/**
		 * Determine if a window is "real". We consider it "real" if:
		 * 1) It has a titlebar, and;
		 * 2) Is top-level OR appears on the taskbar, OR;
		 * 3) Has no owner AND is not a tool window and is allowed to be active
		 *
		 * Sources
		 */
		public boolean isReal() {
			boolean isReal = false;
			if (hasStyle(WinUser.WS_CAPTION) || hasStyle(WinUser.WS_OVERLAPPED)) {
				// has titlebar
				if (isTopWindow() || hasExStyle(WinUserEx.WS_EX_APPWINDOW)) {
					// is top-level or probably appears on taskbar (probably)
					isReal = true;
				} else if (!hasStyle(WinUser.WS_CHILD) && !hasParentWindow())  {
					if (!hasExStyle(WinUserEx.WS_EX_NOACTIVATE) && !hasExStyle(WinUserEx.WS_EX_TOOLWINDOW)) {
						isReal = true;
					}
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
			return User32Ex.INSTANCE.GetAncestor(mHWnd, WinUser.GA_ROOT);
		}

		private boolean hasStyle(long windowStyle) {
			return windowStyle == (styleFlags & windowStyle);
		}

		private boolean hasExStyle(long windowExStyle) {
			return windowExStyle == (styleExFlags & windowExStyle);
		}
	}
}
