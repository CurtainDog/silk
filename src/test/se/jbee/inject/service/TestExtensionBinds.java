package se.jbee.inject.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static se.jbee.inject.bind.AssertInjects.assertEqualSets;
import static se.jbee.inject.service.ExtensionModule.extensionDependency;

import java.io.Serializable;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Module;
import se.jbee.inject.service.ServiceMethod.ServiceClassExtension;

public class TestExtensionBinds {

	private static class TestExtensionModule
			extends ExtensionModule {

		@Override
		protected void declare() {
			extend( asDefault(), ServiceClassExtension.class, TestExtensionService.class );
			extend( inPackageOf( Module.class ), ServiceClassExtension.class,
					TestExtensionPackageLocalService.class );
			extend( injectingInto( Serializable.class ), ServiceClassExtension.class,
					TestExtensionInstanceOfService.class );
		}
	}

	private static class TestExtensionService {
		// just to see that it is resolved as service class
	}

	private static class TestExtensionPackageLocalService {
		// just to see that it is resolved as service class
	}

	private static class TestExtensionInstanceOfService {
		// just to see that it is resolved as service class
	}

	private final Injector injector = Bootstrap.injector( TestExtensionModule.class );

	@SuppressWarnings ( { "rawtypes" } )
	private final Dependency<Class[]> dependency = extensionDependency( ServiceClassExtension.class );

	@Test
	public void thatJustUntargetedExtensionsAreResolvedGlobally() {
		Class<?>[] classes = injector.resolve( dependency );
		assertThat( classes.length, is( 1 ) );
		assertSame( TestExtensionService.class, classes[0] );
	}

	@Test
	public void thatPackageLocalExtensionsAreResolvedWithAppropiateInjection() {
		Class<?>[] classes = injector.resolve( dependency.injectingInto( Module.class ) );
		assertEqualSets( new Class<?>[] { TestExtensionService.class,
				TestExtensionPackageLocalService.class }, classes );
	}

	@Test
	public void thatInstanceOfExtensionsAreResolvedWithAppropiateInjection() {
		Class<?>[] classes = injector.resolve( dependency.injectingInto( String.class ) );
		assertEqualSets( new Class<?>[] { TestExtensionService.class,
				TestExtensionInstanceOfService.class }, classes );
	}
}
