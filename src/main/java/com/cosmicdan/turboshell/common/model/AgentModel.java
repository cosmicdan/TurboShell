package com.cosmicdan.turboshell.common.model;

import com.cosmicdan.turboshell.common.model.payload.IPayload;
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
	private Set<CallbackReceiverEntry<IPayload>> mCallbacks = null;
	private Thread agentThread = null;

	///////////////////
	// Thread related things
	///////////////////

	/**
	 * Start this agent if required. If it is already running, nothing will happen.
	 */
	public final void start() {
		if (null == agentThread) {
			agentThread = new Thread(this);
			agentThread.start();
		}
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

	@FunctionalInterface
	public interface PayloadCallback<T extends IPayload> {
		void run(T payload);
	}

	private static final class CallbackReceiverEntry<T extends IPayload> {
		private final Class<? extends IPayload> mPayloadClass;
		private final PayloadCallback<T> mCallback;

		private CallbackReceiverEntry(final Class<T> payloadClass, final PayloadCallback<T> callback) {
			mPayloadClass = payloadClass;
			mCallback = callback;
		}

		void run(final T mPayload) {
			mCallback.run(mPayload);
		}
	}

	public final void registerCallback(final Class<? extends IPayload> payloadClass, final PayloadCallback<? extends IPayload> callback) {
		if (null == callback)
			return;

		synchronized(callbackLock) {
			if (null == mCallbacks)
				mCallbacks = new HashSet<>(1);
			//noinspection unchecked
			mCallbacks.add(new CallbackReceiverEntry(payloadClass, callback));
		}
	}

	protected final void runCallbacks(final IPayload payload) {
		final Collection<CallbackReceiverEntry<IPayload>> callbacksCopy;
		// first we copy the relevent payload to a new hashmap since we don't want to run each callback with a thread lock held
		synchronized(callbackLock) {
			callbacksCopy = new HashSet<>(mCallbacks.size());
			for (final CallbackReceiverEntry<IPayload> callback : mCallbacks) {
				if (payload.isTypeOf(callback.mPayloadClass)) {
					callbacksCopy.add(callback);
				}
			}
		}

		for (final CallbackReceiverEntry<IPayload> callback : callbacksCopy) {
			callback.run(payload);
		}
	}
}
