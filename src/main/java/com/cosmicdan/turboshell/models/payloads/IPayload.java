package com.cosmicdan.turboshell.models.payloads;

/**
 * @author Daniel 'CosmicDan' Connolly
 */
public interface IPayload {
	default boolean isTypeOf(final Class <? extends IPayload> comparePayload) {
		return getClass().getSimpleName().equals(comparePayload.getSimpleName());
	}

}
