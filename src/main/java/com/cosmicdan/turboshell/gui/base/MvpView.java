package com.cosmicdan.turboshell.gui.base;

@FunctionalInterface
public interface MvpView<T extends MvpPresenter> {
	void setPresenter(T presenter);
}
