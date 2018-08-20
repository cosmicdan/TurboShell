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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import java.util.List;

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

	// cached values passed in from presenter (environment)
	private int barWidth = 0;

	// Controls
	// We re-use this collection during runtime for modifying positioning
	private final List<Region> coreControls = new ArrayList<>(10);
	private static Region centerPaddingLeft = null;
	private static Region centerPaddingLeftAdjuster = null; // Grows as calculated to keep title centered
	private static Label windowTitleLbl = null;
	private static final double windowTitleMaxWidthDivisor = 2.0; // title bar can take up max of a half of bar/screen width total
	private static Region centerPaddingRight = null;
	private static Region centerPaddingRightAdjuster = null; // Grows as calculated to keep title centered
	private static Label dateTimeLbl = null;
	private static AdaptiveButton sysBtnMinimize = null;
	private static AdaptiveButton sysBtnResize = null;
	private static AdaptiveButton sysBtnClose = null;

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
		// TODO: This will need to be called again if the environment changes (resolution, startbar position, etc)
		// initial stage setup
		final Scene scene = new Scene(pane, width, barHeight);
		scene.getStylesheets().add(css);
		mPrimaryStage.initStyle(StageStyle.UTILITY);
		mPrimaryStage.setScene(scene);
		mPrimaryStage.setTitle(windowName);
		mPrimaryStage.setAlwaysOnTop(true);
		// add action for click on TurboBar itself
		scene.setOnMouseClicked((MouseEvent event) -> {
			SystemAction.ACTIVATE_LAST_MAXIMIZED.invoke(mPresenter, event);
		});
		redraw(xPos, width, barHeight);
	}

	@Override
	public final void redraw(final int newXPos, final int newBarWidth, final int newBarHeight) {
		barWidth = newBarWidth;
		setupCoreControls(newBarHeight);
		mPrimaryStage.setWidth(newBarWidth);
		mPrimaryStage.setHeight(newBarHeight);
		mPrimaryStage.setX(newXPos);
		mPrimaryStage.setY(0);
		mPrimaryStage.show();
	}

	@SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "FeatureEnvy"})
	private void setupCoreControls(final int barHeight) {
		// remove all existing controls, if any
		pane.getChildren().removeAll(coreControls);
		coreControls.clear();

		// Start a new controls factory
		final TurboBarControlFactory factory = new TurboBarControlFactory(getClass(), barHeight);

		//////////////////////////////////////////////////////////////
		// Center-left padding (for center alignment of title)
		//////////////////////////////////////////////////////////////
		centerPaddingLeft = factory.newCenterPaddingRegion();
		coreControls.add(centerPaddingLeft);
		// Grows as calculated to keep title centered
		centerPaddingLeftAdjuster = factory.newCenterPaddingRegion();
		centerPaddingLeftAdjuster.setMinWidth(Region.USE_PREF_SIZE);
		centerPaddingLeftAdjuster.setMaxWidth(Region.USE_PREF_SIZE);
		coreControls.add(centerPaddingLeftAdjuster);

		// Current Window title
		windowTitleLbl = factory.newLabel(Pos.CENTER);
		coreControls.add(windowTitleLbl);

		//////////////////////////////////////////////////////////////
		// Center-right padding (for center alignment of title, and right-aligned controls)
		//////////////////////////////////////////////////////////////
		// Grows as calculated to keep title centered
		centerPaddingRightAdjuster = factory.newCenterPaddingRegion();
		centerPaddingRightAdjuster.setMinWidth(Region.USE_PREF_SIZE);
		centerPaddingRightAdjuster.setMaxWidth(Region.USE_PREF_SIZE);
		coreControls.add(centerPaddingRightAdjuster);
		centerPaddingRight = factory.newCenterPaddingRegion();
		coreControls.add(centerPaddingRight);

		// Date
		dateTimeLbl = factory.newLabel(Pos.CENTER);
		// always try to fit
		dateTimeLbl.setMinWidth(Region.USE_PREF_SIZE);
		coreControls.add(dateTimeLbl);

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
		// TurboBar controls done, add barWidth change listener then add them all to the stage
		//////////////////////////////////////////////////////////////
		pane.getChildren().addAll(coreControls);
		final ChangeListener<Number> changeListener =
				(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> updateTitlePaddingAdjusters();
		for (final Region childControl : coreControls) {
			childControl.widthProperty().addListener(changeListener);
		}
	}

	private void updateTitlePaddingAdjusters() {
		// abort if all the controls are not ready
		if (coreControls.isEmpty())
			return;

		// calculate which adjuster needs to be enlarged so the title stays centered
		final int leftPaddingIndex = coreControls.indexOf(centerPaddingLeft);
		final int rightPaddingIndex = coreControls.indexOf(centerPaddingRight);
		double controlsWidthLeft = 0.0;
		double controlsWidthRight = 0.0;
		for (int i = 0; i < coreControls.size(); i++) {
			if (i < leftPaddingIndex) {
				controlsWidthLeft += coreControls.get(i).getWidth();
			} else if (i > rightPaddingIndex) {
				controlsWidthRight += coreControls.get(i).getWidth();
			}
		}

		centerPaddingLeftAdjuster.setPrefWidth(0);
		centerPaddingRightAdjuster.setPrefWidth(0);

		if (controlsWidthRight > controlsWidthLeft) {
			centerPaddingLeftAdjuster.setPrefWidth(controlsWidthRight);
		} else {
			centerPaddingRightAdjuster.setPrefWidth(controlsWidthRight);
		}
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
				// set it to "maximize" graphic by default
				sysBtnResize.setImageViewIndex(0);
			}
			// set disabled/enabled
			sysBtnResize.setDisable(SysBtnResizeState.DISABLED == toState);
		});
	}

	@Override
	public final void updateDateTime(final String dateTime) {
		log.info(dateTime);
		Platform.runLater(() -> dateTimeLbl.setText(dateTime));
	}

	@Override
	public final void updateWindowTitle(String windowTitle) {
		if (!windowTitleLbl.getText().equals(windowTitle)) {
			//log.info("Got window title update: {}", windowTitle);
			Platform.runLater(() -> windowTitleLbl.setText(windowTitle));
		}
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
