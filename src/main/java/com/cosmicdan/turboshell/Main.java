package com.cosmicdan.turboshell;

import com.cosmicdan.turboshell.turbobar.TurboBarContract.ITurboBarPresenter;
import com.cosmicdan.turboshell.turbobar.TurboBarContract.ITurboBarView;
import com.cosmicdan.turboshell.turbobar.TurboBarPresenter;
import com.cosmicdan.turboshell.turbobar.TurboBarView;
import javafx.application.Application;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

/**
 * TurboShell main entrypoint
 * @author Daniel 'CosmicDan' Connolly
 */
@SuppressWarnings("UtilityClass")
@Log4j2
public final class Main {
	private Main() {}

	public static void main(final String[] args) {
		final Runnable appRunnable = () -> Application.launch(App.class);
		final Thread appThread = new Thread(appRunnable);
		appThread.start();
	}

	@SuppressWarnings("PublicInnerClass")
	public static class App extends Application {
		public App() {}

		@Override
		public final void start(final Stage primaryStage) {
			log.info("Starting TurboShell...");

			final ITurboBarView turboBarView = new TurboBarView(primaryStage);
			final ITurboBarPresenter turboBarPresenter = new TurboBarPresenter().setup(turboBarView);
		}
	}
}
