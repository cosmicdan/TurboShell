package com.cosmicdan.turboshell.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * TurboBar view
 * @author Daniel 'CosmicDan' Connolly
 */
public class TurboBarView implements TurboBarContract.View {
	private final Stage mPrimaryStage;
	private final Parent mView;

	private TurboBarContract.Presenter mPresenter;

	public TurboBarView(Stage primaryStage) {
		mPrimaryStage = primaryStage;
		mView = new HBox();
		mView.setId("turbobar");
	}

	@Override
	public void setPresenter(TurboBarContract.Presenter presenter) {
		mPresenter = presenter;
	}

	@Override
	public void setup(int xPos, int width, int barHeight, String css) {
		Scene scene = new Scene(mView, width, barHeight);
		scene.getStylesheets().add(css);
		mPrimaryStage.initStyle(StageStyle.UNDECORATED);
		mPrimaryStage.setScene(scene);
		mPrimaryStage.setTitle(mPresenter.getWindowTitle());
		mPrimaryStage.setX(xPos);
		mPrimaryStage.setY(0);
		mPrimaryStage.setAlwaysOnTop(true);
		mPrimaryStage.show();
	}
}
