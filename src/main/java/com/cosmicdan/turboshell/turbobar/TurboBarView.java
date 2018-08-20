package com.cosmicdan.turboshell.turbobar;

import com.cosmicdan.turboshell.TurboShellConfig;
import com.cosmicdan.turboshell.common.control.AdaptiveButton;
import com.cosmicdan.turboshell.turbobar.TurboBarContract.ITurboBarPresenter;
import com.cosmicdan.turboshell.turbobar.TurboBarContract.ITurboBarView;
import com.cosmicdan.turboshell.turbobar.TurboBarPresenter.SysBtnAction;
import com.cosmicdan.turboshell.turbobar.TurboBarPresenter.SystemAction;
import com.cosmicdan.turboshell.turbobar.animation.KillCountdownProgress;
import com.cosmicdan.turboshell.turbobar.animation.KillCountdownProgress.AnimationDirection;
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
public class TurboBarView extends TurboBarControlFactory implements ITurboBarView {
	private SysBtnCloseAction currentCloseAction = SysBtnCloseAction.CLICK;

	private final Stage mPrimaryStage;
	private final HBox pane;

	private ITurboBarPresenter mPresenter = null;

	// Controls
	// We re-use this collection during runtime for modifying positioning
	private final List<Region> coreControls = new ArrayList<>(10);
	private Region centerPaddingLeft = null;
	private Region centerPaddingLeftAdjuster = null; // Grows as calculated to keep title centered
	private Label windowTitleLbl = null;
	private Region centerPaddingRight = null;
	private Region centerPaddingRightAdjuster = null; // Grows as calculated to keep title centered
	private Label dateTimeLbl = null;
	private AdaptiveButton sysBtnMinimize = null;
	private AdaptiveButton sysBtnResize = null;
	private AdaptiveButton sysBtnClose = null;

	public TurboBarView(final Stage primaryStage) {
		mPrimaryStage = primaryStage;
		pane = new HBox();
		pane.setId("turbobar");
		pane.setAlignment(Pos.CENTER_LEFT);
	}

	@Override
	public final void setup(final ITurboBarPresenter presenter, final int xPos, final int width, final int barHeight, final String css, final String windowName) {
		// TODO: This will need to be called again if the environment changes (resolution, startbar position, etc)
		// initial stage setup
		mPresenter = presenter;
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
		setupCoreControls(newBarHeight);
		mPrimaryStage.setWidth(newBarWidth);
		mPrimaryStage.setHeight(newBarHeight);
		mPrimaryStage.setX(newXPos);
		mPrimaryStage.setY(0);
		mPrimaryStage.show();
	}

	private void setupCoreControls(final int barHeight) {
		// remove all existing controls, if any
		pane.getChildren().removeAll(coreControls);
		coreControls.clear();

		// Start a new controls factory
		//final TurboBarControlFactory factory = new TurboBarControlFactory(getClass(), barHeight);
		setupFactory(getClass(), barHeight);

		//////////////////////////////////////////////////////////////
		// Center-left padding (for center alignment of title)
		//////////////////////////////////////////////////////////////
		centerPaddingLeft = newHboxPaddingRegion();
		coreControls.add(centerPaddingLeft);
		// Grows as calculated to keep title centered
		centerPaddingLeftAdjuster = newHboxPaddingRegion();
		centerPaddingLeftAdjuster.setMinWidth(Region.USE_PREF_SIZE);
		centerPaddingLeftAdjuster.setMaxWidth(Region.USE_PREF_SIZE);
		coreControls.add(centerPaddingLeftAdjuster);

		// Current Window title
		windowTitleLbl = newLabel();
		coreControls.add(windowTitleLbl);

		//////////////////////////////////////////////////////////////
		// Center-right padding (for center alignment of title, and right-aligned controls)
		//////////////////////////////////////////////////////////////
		// Grows as calculated to keep title centered
		centerPaddingRightAdjuster = newHboxPaddingRegion();
		centerPaddingRightAdjuster.setMinWidth(Region.USE_PREF_SIZE);
		centerPaddingRightAdjuster.setMaxWidth(Region.USE_PREF_SIZE);
		coreControls.add(centerPaddingRightAdjuster);
		centerPaddingRight = newHboxPaddingRegion();
		coreControls.add(centerPaddingRight);

		// Date
		dateTimeLbl = newLabel();
		// always try to fit
		dateTimeLbl.setMinWidth(Region.USE_PREF_SIZE);
		coreControls.add(dateTimeLbl);

		// Separator
		coreControls.add(newVerticalSeparator());

		//////////////////////////////////////////////////////////////
		// SysButtons
		//////////////////////////////////////////////////////////////

		// minimize
		sysBtnMinimize = newGenericButton(
				"TurboBar_sysbtn_minimize.png",
				MouseEvent.MOUSE_CLICKED,
				(MouseEvent event) -> SysBtnAction.MINIMIZE.invoke(mPresenter, event));
		coreControls.add(sysBtnMinimize);
		// resize (maximize/restore)
		sysBtnResize = newGenericButton(
				new String[] {"TurboBar_sysbtn_resize_maximize.png", "TurboBar_sysbtn_resize_restore.png"},
				MouseEvent.MOUSE_CLICKED,
				(MouseEvent event) -> SysBtnAction.RESIZE.invoke(mPresenter, event));
		coreControls.add(sysBtnResize);
		// close
		sysBtnClose = newGenericButton(
				"TurboBar_sysbtn_close.png",
				null,
				null);
		sysBtnClose.addEventFilter(MouseEvent.MOUSE_RELEASED, (MouseEvent event) -> {
			performCloseAction(currentCloseAction, mPresenter, event);
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

		final KillCountdownProgress holdTimer = new KillCountdownProgress(
				holdTime, sysBtnClose, colorHex, isPrimaryClick ? AnimationDirection.REVERSE : AnimationDirection.NORMAL);
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
	public final void updateWindowTitle(final String windowTitle) {
		if (!windowTitleLbl.getText().equals(windowTitle)) {
			//log.info("Got window title update: {}", windowTitle);
			Platform.runLater(() -> windowTitleLbl.setText(windowTitle));
		}
	}

	private static void performCloseAction(final SysBtnCloseAction action, final ITurboBarPresenter mPresenter, final MouseEvent event) {
		if ((SysBtnCloseAction.CLICK == action) && (MouseButton.PRIMARY != event.getButton()))
			// we don't take any action for secondary-click
			return;
		SysBtnAction.performCloseAction(action, mPresenter, event);
	}
}
