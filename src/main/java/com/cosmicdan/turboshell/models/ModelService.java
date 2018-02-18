package com.cosmicdan.turboshell.models;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.HashSet;
import java.util.Set;

/**
 * A wrapper for "service"-type models. They have a runtime loop and observable data holders.
 * Eases implementation of environment cleanup when closing the thread and thread safe data transfer.
 * Be sure that any getters/setters that are shared by the observable implement some synchronization.
 * @author Daniel 'CosmicDan' Connolly
 */
@Log4j2
public abstract class ModelService {
	private final ModelServiceThread thread;
	private final Object observerLock = new Object();
	private Set<Observer> mObservers;

	ModelService() {
		thread = getThread();
		thread.start();
	}

	///////////////////
	// Thread related things
	///////////////////

	/**
	 * Provide a new instance of the thread to use for this ModelService.
	 */
	public abstract ModelServiceThread getThread();

	public abstract class ModelServiceThread extends Thread {
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
	// Observer related things
	///////////////////

	public interface Observer {
		void onObservablePayload(ObservablePayload payload);
	}

	/**
	 * Generic data holder to ease data transfer between observables and observers
	 */
	public class ObservablePayload {
		private final Object lock = new Object();
		/** Gets the payload ID that was assigned to it from the observable end, if any. */
		@Getter private final int mPayloadId;
		private final ModelService mSource;
		private Object mData;

		/**
		 * Creates a new object intended to hold data that is send to observers when updated.
		 * @param payloadId A unique ID for this payload. The observers can use this to identify the data, if desired.
		 * @param source The source observable (usually an instance of the object creator i.e. 'this').
		 */
		public ObservablePayload(int payloadId, ModelService source) {
			mPayloadId = payloadId;
			mSource = source;
		}

		/**
		 * Used by observers to check which observable the payload came from
		 * @return The ModelService subclass that sent this payload
		 */
		public Class<? extends ModelService> getSource() {
			return mSource.getClass();
		}

		/**
		 * Set the payload data and push to all registered observers.
		 * @param obj
		 */
		public void set(Object obj) {
			synchronized (lock) {
				mData = obj;
			}
			mSource.updateObservers(this);
		}

		/**
		 * Read the payload data
		 */
		public Object get() {
			synchronized (lock) {
				return mData;
			}
		}
	}

	public void registerObserver(Observer observer) {
		if (observer == null)
			return;

		synchronized(observerLock) {
			if (mObservers == null)
				mObservers = new HashSet<>(1);
			mObservers.add(observer);
		}
	}

	public void unregisterObserver(Observer observer) {
		if (observer == null)
			return;
		synchronized(observerLock) {
			if (mObservers != null)
				mObservers.remove(observer);
		}
	}

	/**
	 * Send the Observable to any registered Observers.
	 */
	private void updateObservers(ObservablePayload payload) {
		synchronized(observerLock) {
			if (mObservers != null) {
				for (Observer observer : mObservers) {
					observer.onObservablePayload(payload);
				}
			}
		}
	}
}
