package com.cosmicdan.turboshell.turbobar;

import com.cosmicdan.turboshell.gui.AdaptiveButton;
import com.cosmicdan.turboshell.gui.KillCountdownProgress;
import com.cosmicdan.turboshell.gui.KillCountdownProgress.AnimationDirection;
import com.cosmicdan.turboshell.models.TurboShellConfig;
import com.cosmicdan.turboshell.turbobar.TurboBarContract.ITurboBarPresenter;
import com.cosmicdan.turboshell.turbobar.TurboBarContract.ITurboBarView;
import com.cosmicdan.turboshell.turbobar.TurboBarPresenter.SysBtnAction;
import com.cosmicdan.turboshell.turbobar.TurboBarPresenter.SystemAction;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Collection;

/**
 * TurboBar view
 * @author Daniel 'CosmicDan' Connolly
 */
@SuppressWarnings("ClassWithTooManyDependencies")
@Log4j2(topic = "TurboBarView")
public class TurboBarView implements ITurboBarView {
	public enum SysBtnMinimizeState {ENABLED, DISABLED}
	public enum SysBtnResizeState {MAXIMIZE, RESTORE, DISABLED}
	public enum SysBtnCloseAction {CANCEL, CLICK, PRIMARY_HELD, SECONDARY_HELD}

	private SysBtnCloseAction currentCloseAction = SysBtnCloseAction.CLICK;

	private final Stage mPrimaryStage;
	private final HBox pane;

	private ITurboBarPresenter mPresenter = null;

	private static AdaptiveButton sysBtnMinimize = null;
	private static AdaptiveButton sysBtnResize = null;
	private static AdaptiveButton sysBtnClose = null;

	private static Label dateTime = null;

	public TurboBarView(final Stage primaryStage) {
		mPrimaryStage = primaryStage;
		pane = new HBox();
		pane.setId("turbobar");
		pane.setAlignment(Pos.CENTER_LEFT);
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
		scene.setOnMouseClicked((MouseEvent event) -> {
			SystemAction.ACTIVATE_LAST_MAXIMIZED.invoke(mPresenter, event);
		});
	}

	@Override
	public final void refreshSize(final int xPos, final int width, final int barHeight) {
		mPrimaryStage.setWidth(width);
		mPrimaryStage.setHeight(barHeight);
		mPrimaryStage.setX(xPos);
		mPrimaryStage.setY(0);
		mPrimaryStage.show();
	}

	@SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "FeatureEnvy"})
	private void setupCoreControls(final int barHeight) {
		// Build the list of controls that we're going to add
		final Collection<Region> coreControls = new ArrayList<>(10);
		// Start a new controls factory
		final TurboBarControlFactory factory = new TurboBarControlFactory(getClass(), barHeight);

		//////////////////////////////////////////////////////////////
		// Center padding (for right alignment)
		//////////////////////////////////////////////////////////////
		coreControls.add(factory.newCenterPaddingRegion());

		// Date
		dateTime = factory.newLabel();
		coreControls.add(dateTime);

		// Separator
		coreControls.add(factory.newVerticalSeparator());

		//////////////////////////////////////////////////////////////
		// SysButtons
		//////////////////////////////////////////////////////////////

		// minimize
		sysBtnMinimize = factory.newGenericButton(
				"TurboBar_sysbtn_minimize.png",
				MouseEvent.MOUSE_CLICKED,
				(MouseEvent event) -> SysBtnAction.MINIMIZE.invoke(mPresenter, event));
		coreControls.add(sysBtnMinimize);
		// resize (maximize/restore)
		sysBtnResize = factory.newGenericButton(
				new String[] {"TurboBar_sysbtn_resize_maximize.png", "TurboBar_sysbtn_resize_restore.png"},
				MouseEvent.MOUSE_CLICKED,
				(MouseEvent event) -> SysBtnAction.RESIZE.invoke(mPresenter, event));
		coreControls.add(sysBtnResize);
		// close
		sysBtnClose = factory.newGenericButton(
				"TurboBar_sysbtn_close.png",
				null,
				null);
		sysBtnClose.addEventFilter(MouseEvent.MOUSE_RELEASED, (MouseEvent event) -> {
			closeButtonAction(event, currentCloseAction);
			// reset
			currentCloseAction = SysBtnCloseAction.CLICK;
		});
		// close button primary-click-and-hold
		setHoldButtonHandler(MouseButton.PRIMARY, sysBtnClose, Duration.seconds(1), (MouseEvent event)
				-> currentCloseAction = SysBtnCloseAction.PRIMARY_HELD);
		// close button secondary-click-and-hold
		setHoldButtonHandler(MouseButton.SECONDARY, sysBtnClose, Duration.seconds(1), (MouseEvent event)
				-> currentCloseAction = SysBtnCloseAction.SECONDARY_HELD);
		sysBtnClose.setId("close");
		coreControls.add(sysBtnClose);

		//////////////////////////////////////////////////////////////
		// TurboBar controls done, add them to the stage
		//////////////////////////////////////////////////////////////
		pane.getChildren().addAll(coreControls);
	}

	/**
	 * Thanks to James_D @ StackOverflow for the basis of this
	 * https://stackoverflow.com/a/25610190/1767892
	 */
	private void setHoldButtonHandler(final MouseButton mouseButton, final AdaptiveButton node, final Duration holdTime, final EventHandler<MouseEvent> handler) {
		final MouseEvent[] mouseEvent = {null};

		final boolean isPrimaryClick = MouseButton.PRIMARY == mouseButton;
		final String colorHex = isPrimaryClick ? TurboShellConfig.getTurboBarCloseBgPrimary() : TurboShellConfig.getTurboBarCloseBgSecondary();

		final KillCountdownProgress holdTimer = new KillCountdownProgress(holdTime, sysBtnClose, colorHex,
				isPrimaryClick ? AnimationDirection.REVERSE : AnimationDirection.NORMAL);
		holdTimer.setOnFinished((ActionEvent event) -> handler.handle(mouseEvent[0]));

		node.addEventHandler(MouseEvent.MOUSE_PRESSED, (MouseEvent event) -> {
			if (mouseButton == event.getButton()) {
				mouseEvent[0] = event ;
				holdTimer.playFromStart();
			}
		});
		node.addEventHandler(MouseEvent.MOUSE_RELEASED, (MouseEvent event) -> holdTimer.stop());
		node.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, (MouseEvent event) -> {
			// abort any existing action
			currentCloseAction = SysBtnCloseAction.CANCEL;
			holdTimer.stop();
		});
		node.addEventHandler(MouseEvent.MOUSE_ENTERED_TARGET, (MouseEvent event) -> {
			// re-set default action
			currentCloseAction = SysBtnCloseAction.CLICK;
		});
	}

	@Override
	public final void updateSysBtnMinimize(final SysBtnMinimizeState toState) {
		Platform.runLater(() -> sysBtnMinimize.setDisable(SysBtnMinimizeState.DISABLED == toState));
	}

	@Override
	public final void updateSysBtnResize(final SysBtnResizeState toState) {
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

	@Override
	public void updateDateTime(final String date) {
		log.info(date);
		dateTime.setText(date);
	}


	@SuppressWarnings("FeatureEnvy")
	private void closeButtonAction(final MouseEvent event, final SysBtnCloseAction action) {
		//noinspection SwitchStatement
		switch (action) {
			case CANCEL:
				break;
			case CLICK:
				if (MouseButton.PRIMARY == event.getButton()) {
					SysBtnAction.CLOSE.invoke(mPresenter, event);
				}
				break;
			case PRIMARY_HELD:
				SysBtnAction.FORCE_CLOSE.invoke(mPresenter, event);
				break;
			case SECONDARY_HELD:
				SysBtnAction.KILL.invoke(mPresenter, event);
				break;
		}
	}
}
