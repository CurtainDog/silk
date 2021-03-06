package se.jbee.inject.bind;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.util.Scoped.INJECTION;
import static se.jbee.inject.util.Typecast.injectronsTypeOf;

import java.beans.ConstructorProperties;

import org.junit.Test;

import se.jbee.inject.DIRuntimeException.DependencyCycleException;
import se.jbee.inject.DeclarationType;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Injectron;
import se.jbee.inject.Name;
import se.jbee.inject.Resource;
import se.jbee.inject.Supplier;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bundle;
import se.jbee.inject.bootstrap.Inspect;

/**
 * The tests shows an example of cyclic depended {@link Bundle}s. It shows that a {@link Bundle}
 * doesn't have to know or consider other bundles since it is valid to make cyclic references or
 * install the {@link Bundle}s multiple times.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestBootstrapper {

	/**
	 * One of two bundles in a minimal example of mutual dependent bundles. While this installs
	 * {@link OtherMutualDependentBundle} that bundle itself installs this bundle. This should not
	 * be a problem and both bundles are just installed once.
	 */
	private static class OneMutualDependentBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( OtherMutualDependentBundle.class );
		}

	}

	private static class OtherMutualDependentBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( OneMutualDependentBundle.class );
		}

	}

	/**
	 * Because the same {@link Resource} is defined twice (the {@link Name#DEFAULT} {@link Integer}
	 * instance) this module should cause an exception. All {@link Resource} have to be unique.
	 */
	private static class ClashingBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( Integer.class ).to( 42 );
			bind( Integer.class ).to( 8 );
		}

	}

	private static class ReplacingBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			asDefault().bind( Number.class ).to( 7 );
			asDefault().bind( Integer.class ).to( 11 );
			autobind( Integer.class ).to( 2 );
			autobind( Float.class ).to( 4f );
			autobind( Double.class ).to( 42d );
			bind( Number.class ).to( 6 );
		}

	}

	@SuppressWarnings ( "unused" )
	private static class Foo {

		Foo( Bar bar ) {
			// something
		}
	}

	@SuppressWarnings ( "unused" )
	private static class Bar {

		public Bar( Foo foo ) {
			// ...
		}
	}

	@SuppressWarnings ( "unused" )
	private static class A {

		A( B b ) {
			// ...
		}
	}

	@SuppressWarnings ( "unused" )
	private static class B {

		B( C c ) {
			// ...
		}
	}

	@SuppressWarnings ( "unused" )
	private static class C {

		C( A a ) {
			// ...
		}
	}

	private static class CyclicBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( Foo.class ).toConstructor( raw( Bar.class ) );
			bind( Bar.class ).toConstructor( raw( Foo.class ) );
		}

	}

	private static class CircularBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( A.class ).toConstructor( raw( B.class ) );
			bind( B.class ).toConstructor( raw( C.class ) );
			bind( C.class ).toConstructor( raw( A.class ) );
		}

	}

	private static class EagerSingletonsBindsModule
			extends BinderModule
			implements Supplier<String> {

		static int eagers = 0;

		@Override
		protected void declare() {
			bind( named( "eager" ), String.class ).to( this );
			per( INJECTION ).bind( named( "lazy" ), String.class ).to( this );
		}

		@Override
		public String supply( Dependency<? super String> dependency, Injector injector ) {
			if ( !dependency.getName().equalTo( named( "lazy" ) ) ) {
				eagers++;
				return "eager";
			}
			fail( "since it is lazy it should not be called" );
			return "fail";
		}

	}

	private static class CustomInspectedBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( CustomInspectedModule.class,
					Inspect.all().constructors().annotatedWith( ConstructorProperties.class ) );
		}
	}

	private static class CustomInspectedModule
			extends BinderModule {

		@Override
		protected void declare() {
			construct( D.class );
			bind( String.class ).to( "will be passed to D" );
		}
	}

	@SuppressWarnings ( "unused" )
	private static class D {

		final String s;

		@ConstructorProperties ( {} )
		D( String s ) {
			this.s = s;

		}

		D() {
			this( "would be picked normally" );
		}
	}

	/**
	 * The assert itself doesn't play such huge role here. we just want to reach this code.
	 */
	@Test
	public void thatBundlesAreNotBootstrappedMultipleTimesEvenWhenTheyAreMutual() {
		Injector injector = Bootstrap.injector( OneMutualDependentBundle.class );
		assertThat( injector, notNullValue() );
	}

	@Test ( expected = IllegalStateException.class )
	public void thatNonUniqueResourcesThrowAnException() {
		Bootstrap.injector( ClashingBindsModule.class );
	}

	@Test ( expected = DependencyCycleException.class )
	public void thatDependencyCyclesAreDetected() {
		Injector injector = Bootstrap.injector( CyclicBindsModule.class );
		Foo foo = injector.resolve( dependency( Foo.class ) );
		fail( "foo should not be resolvable but was: " + foo );
	}

	@Test ( expected = DependencyCycleException.class )
	public void thatDependencyCyclesInCirclesAreDetected() {
		Injector injector = Bootstrap.injector( CircularBindsModule.class );
		A a = injector.resolve( dependency( A.class ) );
		fail( "A should not be resolvable but was: " + a );
	}

	/**
	 * In the example {@link Number} is {@link DeclarationType#AUTO} bound for {@link Integer} and
	 * {@link Float} but an {@link DeclarationType#EXPLICIT} bind done overrides these automatic
	 * binds. They are removed and no {@link Injectron} is created for them.
	 */
	@Test
	public void thatBindingsAreReplacedByMorePreciseOnes() {
		Injector injector = Bootstrap.injector( ReplacingBindsModule.class );
		assertEquals( 6, injector.resolve( dependency( Number.class ) ) );
		Injectron<?>[] injectrons = injector.resolve( dependency( Injectron[].class ) );
		assertEquals( 7, injectrons.length ); // 3x Comparable, Float, Double, Integer and Number (3x Serializable has been nullified)
		Injectron<Number>[] numberInjectrons = injector.resolve( dependency( injectronsTypeOf( Number.class ) ) );
		assertEquals( 1, numberInjectrons.length );
		@SuppressWarnings ( "rawtypes" )
		Injectron<Comparable>[] compareableInjectrons = injector.resolve( dependency( injectronsTypeOf( Comparable.class ) ) );
		assertEquals( 3, compareableInjectrons.length );
	}

	@Test
	public void thatEagerSingeltonsCanBeCreated() {
		Injector injector = Bootstrap.injector( EagerSingletonsBindsModule.class );
		int before = EagerSingletonsBindsModule.eagers;
		Bootstrap.eagerSingletons( injector );
		assertEquals( before + 1, EagerSingletonsBindsModule.eagers );
	}

	@Test
	public void thatCustomInspectorIsUsedToPickConstructor() {
		Injector injector = Bootstrap.injector( CustomInspectedBundle.class );
		assertEquals( "will be passed to D", injector.resolve( dependency( D.class ) ).s );
	}
}
