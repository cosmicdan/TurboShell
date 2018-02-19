package com.cosmicdan.turboshell.models;

import lombok.extern.log4j.Log4j2;

import java.util.HashSet;
import java.util.Set;

/**
 * A template for "service"-type models. These have a runtime loop in their own thread and allow other classes (e.g.
 * a Presenter, if following an MVP pattern) to register callbacks to updates in the ModelService data. It's up to the
 * ModelService to send the callbacks manually.
 * @author Daniel 'CosmicDan' Connolly
 */
@Log4j2
public abstract class ModelService {
	private final Object callbackLock = new Object();
	private Set<CallbackInfo> mCallbacks;

	ModelService() {
		ModelServiceThread thread = getThread();
		thread.start();
	}

	///////////////////
	// Thread related things
	///////////////////

	/**
	 * Provide a new instance of the thread to use for this ModelService.
	 */
	protected abstract ModelServiceThread getThread();

	abstract class ModelServiceThread extends Thread {
		@Override
		public final void run() {
			Runtime.getRuntime().addShutdownHook(new Thread(this::serviceStop));
			serviceStart();
		}

		/**
		 * Called when a start is requested. Do your typical Thread#run() stuff here.
		 */
		public abstract void serviceStart();

		/**
		 * Called when a shutdown is request. Do environment cleanup here.
		 */
		public abstract void serviceStop();
	}

	///////////////////
	// Callback related things
	///////////////////

	@FunctionalInterface
	public interface PayloadCallback {
		void run(Object data);
	}

	private static class CallbackInfo {
		private final int mPayloadId;
		private final PayloadCallback mCallback;

		private CallbackInfo(int payloadId, PayloadCallback callback) {
			mPayloadId = payloadId;
			mCallback = callback;
		}

		void run(Object data) {
			mCallback.run(data);
		}
	}

	public void registerCallback(int payloadId, PayloadCallback callback) {
		if (null == callback)
			return;

		synchronized(callbackLock) {
			if (mCallbacks == null)
				mCallbacks = new HashSet<>(1);
			mCallbacks.add(new CallbackInfo(payloadId, callback));
		}
	}

	void runCallbacks(int payloadId, Object data) {
		Set<CallbackInfo> callbacksCopy;

		synchronized(callbackLock) {
			if (mCallbacks == null) return;
			callbacksCopy = new HashSet<>();
			for (CallbackInfo callback : mCallbacks) {
				if (callback.mPayloadId == payloadId)
					callbacksCopy.add(callback);
			}
		}

		for (CallbackInfo callback : callbacksCopy) {
			callback.run(data);
		}
	}
}
