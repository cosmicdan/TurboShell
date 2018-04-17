package com.cosmicdan.turboshell.turbobar;

import com.cosmicdan.turboshell.gui.AdaptiveButton;
import com.cosmicdan.turboshell.turbobar.TurboBarContract.ITurboBarPresenter;
import com.cosmicdan.turboshell.turbobar.TurboBarContract.ITurboBarView;
import com.cosmicdan.turboshell.turbobar.TurboBarPresenter.SysBtnAction;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.Collection;

/**
 * TurboBar view
 * @author Daniel 'CosmicDan' Connolly
 */
public class TurboBarView implements ITurboBarView {
	public enum SysBtnResizeState {MAXIMIZE, RESTORE, DISABLED};

	private final Stage mPrimaryStage;
	private final HBox pane;

	private ITurboBarPresenter mPresenter = null;

	private AdaptiveButton sysBtnMinimize = null;
	private AdaptiveButton sysBtnResize = null;

	public TurboBarView(final Stage primaryStage) {
		mPrimaryStage = primaryStage;
		pane = new HBox();
		pane.setId("turbobar");
	}

	@Override
	public final void setPresenter(final ITurboBarPresenter presenter) {
		mPresenter = presenter;
	}

	@Override
	public final void setup(final int xPos, final int width, final int barHeight, final String css, final String windowName) {
		// initial stage setup
		final Scene scene = new Scene(pane, width, barHeight);
		scene.getStylesheets().add(css);
		mPrimaryStage.initStyle(StageStyle.UTILITY);
		mPrimaryStage.setScene(scene);
		mPrimaryStage.setTitle(windowName);
		refreshSize(xPos, width, barHeight);
		mPrimaryStage.setAlwaysOnTop(true);
		setupCoreControls(barHeight);
	}

	@Override
	public final void refreshSize(final int xPos, final int width, final int barHeight) {
		mPrimaryStage.setWidth(width);
		mPrimaryStage.setHeight(barHeight);
		mPrimaryStage.setX(xPos);
		mPrimaryStage.setY(0);
		mPrimaryStage.show();
	}

	private void setupCoreControls(final int barHeight) {
		// Build the list of controls that we're going to add
		final Collection<Region> coreControls = new ArrayList<>(10);
		// Start a new controls factory
		final TurboBarControlFactory factory = new TurboBarControlFactory(getClass(), barHeight);

		// Add all the controls we want from factory...
		coreControls.add(TurboBarControlFactory.newCenterPaddingRegion());

		// SysButton - Minimize
		sysBtnMinimize = factory.newGenericButton(
				"TurboBar_sysbtn_minimize.png",
				MouseEvent.MOUSE_CLICKED,
				(MouseEvent event) -> SysBtnAction.MINIMIZE.invoke(mPresenter, event));
		coreControls.add(sysBtnMinimize);
		// SysButton - Restore/Maximize
		sysBtnResize = factory.newGenericButton(
				new String[] {"TurboBar_sysbtn_resize_maximize.png", "TurboBar_sysbtn_resize_restore.png"},
				MouseEvent.MOUSE_CLICKED,
				(MouseEvent event) -> SysBtnAction.RESIZE.invoke(mPresenter, event));
		coreControls.add(sysBtnResize);

		// All done, now actually add them to the stage
		pane.getChildren().addAll(coreControls);
	}

	@Override
	public void updateSysBtnResize(final SysBtnResizeState toState) {
		Platform.runLater(() -> {
			if (SysBtnResizeState.RESTORE == toState) {
				sysBtnResize.setImageViewIndex(1);
			} else {
				// set it to "maximuze" graphic by default
				sysBtnResize.setImageViewIndex(0);
			}
			// set disabled/enabled
			sysBtnResize.setDisable(SysBtnResizeState.DISABLED == toState);
		});
	}
}
