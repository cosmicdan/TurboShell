package com.cosmicdan.turboshell;

import com.cosmicdan.turboshell.gui.TurboBarContract;
import com.cosmicdan.turboshell.gui.TurboBarContract.View;
import com.cosmicdan.turboshell.models.WindowsEnvironment;
import com.cosmicdan.turboshell.gui.TurboBarContract.Presenter;
import com.cosmicdan.turboshell.gui.TurboBarPresenter;
import com.cosmicdan.turboshell.gui.TurboBarView;
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

	public static class App extends Application {
		public App() {
		}

		@Override
		public final void start(final Stage primaryStage) {
			log.info("Starting TurboShell...");
			final WindowsEnvironment winEnv = new WindowsEnvironment();

			final View turboBarView = new TurboBarView(primaryStage);
			final Presenter turboBarPresenter = new TurboBarPresenter().setup(turboBarView, winEnv);
		}
	}
}
