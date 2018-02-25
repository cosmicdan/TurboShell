package com.cosmicdan.turboshell.gui;

import com.cosmicdan.turboshell.gui.TurboBarContract.Presenter;
import com.cosmicdan.turboshell.gui.TurboBarContract.View;
import com.cosmicdan.turboshell.gui.TurboBarPresenter.SysBtnAction;
import com.cosmicdan.turboshell.gui.controls.TurboBarControlFactory;
import javafx.scene.Scene;
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
public class TurboBarView implements View {
	private final Stage mPrimaryStage;
	private final HBox pane;

	private Presenter mPresenter = null;

	public TurboBarView(final Stage primaryStage) {
		mPrimaryStage = primaryStage;
		pane = new HBox();
		pane.setId("turbobar");
	}

	@Override
	public final void setPresenter(final Presenter presenter) {
		mPresenter = presenter;
	}

	@Override
	public final void setup(final int xPos, final int width, final int barHeight, final String css, String windowName) {
		// initial stage setup
		final Scene scene = new Scene(pane, width, barHeight);
		scene.getStylesheets().add(css);
		mPrimaryStage.initStyle(StageStyle.UNDECORATED);
		mPrimaryStage.setScene(scene);
		mPrimaryStage.setTitle(windowName);
		mPrimaryStage.setX(xPos);
		mPrimaryStage.setY(0);
		mPrimaryStage.setAlwaysOnTop(true);
		mPrimaryStage.show();
		setupCoreControls(barHeight);
	}

	private void setupCoreControls(final int barHeight) {
		// Build the list of controls that we're going to add
		final Collection<Region> coreControls = new ArrayList<>(10);
		// Start a new controls factory
		final TurboBarControlFactory factory = new TurboBarControlFactory(getClass(), barHeight);

		// Add all the controls we want from factory...
		coreControls.add(TurboBarControlFactory.newCenterPaddingRegion());

		// SysButton - Minimize
		coreControls.add(factory.newGenericButton(
				"TurboBar_sysbtn_minimize.png",
				() -> mPresenter.doViewAction(SysBtnAction.MINIMIZE)
		));

		// All done, now actually add them to the stage
		pane.getChildren().addAll(coreControls);
	}
}
