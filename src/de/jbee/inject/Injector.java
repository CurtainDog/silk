package de.jbee.inject;

/**
 * Knows how to *resolve* an instance for a given {@link Dependency}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public interface Injector {

	<T> T resolve( Dependency<T> dependency );
}
