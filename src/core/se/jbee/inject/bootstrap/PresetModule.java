/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * A {@link PresetModule} is an extension to a usual {@link Module} that depends on *one* of the
 * values that have been preset.
 * 
 * @see Module
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T>
 *            The type of the preset value
 */
public interface PresetModule<T> {

	/**
	 * @param bindings
	 *            use to declare made bound within this {@link Module}.
	 * @param inspector
	 *            the chosen strategy to pick the {@link Constructor}s or {@link Method}s used to
	 *            create instances.
	 * @param preset
	 *            The preset value (chosen by the value's type from the set of all preset values).
	 *            This can very well be null in case no such type value has been preset.
	 */
	void declare( Bindings bindings, Inspector inspector, T preset );
}
