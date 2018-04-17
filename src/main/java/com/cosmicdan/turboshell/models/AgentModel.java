package com.cosmicdan.turboshell.models;

import lombok.extern.log4j.Log4j2;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A template for "agent"-type models. These have a runtime loop in their own thread and allow other classes (e.g.
 * a Presenter, if following an MVP pattern) to register callbacks for updating data or sending requests to the AgentModel.
 * It's up to the AgentModel to react or respond to these callbacks, if necessary.
 * @author Daniel 'CosmicDan' Connolly
 */
@Log4j2
public abstract class AgentModel implements Runnable {
	private final Object callbackLock = new Object();
	private Set<CallbackInfo> mCallbacks = null;

	AgentModel() {}

	///////////////////
	// Thread related things
	///////////////////

	public final void start() {
		new Thread(this).start();
	}

	@Override
	public final void run() {
		Runtime.getRuntime().addShutdownHook(new Thread(this::serviceStop));
		serviceStart();
	}

	/**
	 * Called when a start is requested. Do your typical Thread#run() stuff here.
	 */
	protected abstract void serviceStart();

	/**
	 * Called when a shutdown is request. Do environment cleanup here.
	 */
	protected abstract void serviceStop();


	///////////////////
	// Callback related things
	///////////////////

	@SuppressWarnings("PublicInnerClass")
	@FunctionalInterface
	public interface PayloadCallback {
		void run(Object[] data);
	}

	private static final class CallbackInfo {
		private final int mPayloadId;
		private final PayloadCallback mCallback;

		private CallbackInfo(final int payloadId, final PayloadCallback callback) {
			mPayloadId = payloadId;
			mCallback = callback;
		}

		void run(final Object[] data) {
			mCallback.run(data);
		}
	}

	public final void registerCallback(final int payloadId, final PayloadCallback callback) {
		if (null == callback)
			return;

		synchronized(callbackLock) {
			if (null == mCallbacks)
				mCallbacks = new HashSet<>(1);
			mCallbacks.add(new CallbackInfo(payloadId, callback));
		}
	}

	@SuppressWarnings("MethodWithMultipleLoops")
	final void runCallbacks(final int payloadId, final Object... data) {
		final Collection<CallbackInfo> callbacksCopy;
		synchronized(callbackLock) {
			callbacksCopy = new HashSet<>(mCallbacks.size());
			for (final CallbackInfo callback : mCallbacks) {
				if (callback.mPayloadId == payloadId)
					callbacksCopy.add(callback);
			}
		}

		for (final CallbackInfo callback : callbacksCopy) {
			callback.run(data);
		}
	}
}
