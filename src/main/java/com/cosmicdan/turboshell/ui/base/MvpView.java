package com.cosmicdan.turboshell.ui.base;

public interface MvpView<T extends MvpPresenter> {
	// common View signatures
	void setPresenter(T presenter);
}
