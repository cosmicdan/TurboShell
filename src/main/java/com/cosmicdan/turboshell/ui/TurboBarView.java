package com.cosmicdan.turboshell.ui;

import com.cosmicdan.turboshell.ui.TurboBarContract.SysBtnAction;
import com.cosmicdan.turboshell.ui.controls.AdaptiveButton;
import com.cosmicdan.turboshell.ui.controls.TurboBarControlFactory;
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
public class TurboBarView implements TurboBarContract.View {
	private final Stage mPrimaryStage;
	private final HBox pane;

	private TurboBarContract.Presenter mPresenter;

	public TurboBarView(Stage primaryStage) {
		mPrimaryStage = primaryStage;
		pane = new HBox();
		pane.setId("turbobar");
	}

	@Override
	public void setPresenter(TurboBarContract.Presenter presenter) {
		mPresenter = presenter;
	}

	@Override
	public void setup(int xPos, int width, int barHeight, String css) {
		// initial stage setup
		Scene scene = new Scene(pane, width, barHeight);
		scene.getStylesheets().add(css);
		mPrimaryStage.initStyle(StageStyle.UNDECORATED);
		mPrimaryStage.setScene(scene);
		mPrimaryStage.setTitle(mPresenter.getWindowName());
		mPrimaryStage.setX(xPos);
		mPrimaryStage.setY(0);
		mPrimaryStage.setAlwaysOnTop(true);
		mPrimaryStage.show();
		addCoreControls(barHeight);
	}

	private void addCoreControls(int barHeight) {
		// Build the list of controls that we're going to add
		final Collection<Region> coreControls = new ArrayList<>();
		// Start a new controls factory
		final TurboBarControlFactory factory = new TurboBarControlFactory(getClass(), barHeight);

		// Add all the controls we want from factory...
		coreControls.add(factory.newCenterPaddingRegion());

		// sysbuttons
		AdaptiveButton minimizeButton = factory.newGenericButton("TurboBar_sysbtn_minimize.png");
		minimizeButton.setClickAction(() -> mPresenter.doSysbtnAction(SysBtnAction.MINIMIZE));
		coreControls.add(minimizeButton);

		// All done, now actually add them to the stage
		pane.getChildren().addAll(coreControls);
	}
}
