package de.jbee.inject.bind;

import de.jbee.inject.Suppliable;

public interface Installer {

	Suppliable<?>[] install( Class<? extends Bundle> root );
}