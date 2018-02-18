package com.cosmicdan.turboshell;

import com.cosmicdan.turboshell.ui.TurboBarPresenter;
import com.cosmicdan.turboshell.ui.TurboBarView;
import javafx.application.Application;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {
	public static void main(String[] args) {
		final Runnable appRunnable = () -> Application.launch(App.class);
		final Thread appThread = new Thread(appRunnable);
		appThread.start();
	}

	@SuppressWarnings("WeakerAccess")
	public static class App extends Application {
		@Override
		public void start(Stage primaryStage) {
			log.info("Starting TurboShell...");

			TurboBarView turboBarView = new TurboBarView(primaryStage);
			TurboBarPresenter turboBarPresenter = new TurboBarPresenter(turboBarView);

		}
	}
}
