package de.nmichael.efa.core.update;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry for all known {@link UpdateProviderStrategy} implementations.
 *
 * Every concrete strategy class is expected to register itself (self-registration
 * pattern) by calling {@link #register(UpdateProviderStrategyDescriptor)} from a
 * static initializer block, e.g.:
 *
 * <pre>
 * static {
 *     UpdateProviderStrategyRegistry.register(new UpdateProviderStrategyDescriptor(
 *             MY_NAME, MyStrategy.class,
 *             () -&gt; International.getString("My Display Name"),
 *             () -&gt; International.getString("My user hint"),
 *             config -&gt; new MyStrategy(config)));
 * }
 * </pre>
 *
 * Note on the Java class loading model: a static initializer block is only
 * executed once the class is actively used for the first time (e.g. via
 * {@code new MyStrategy()}, a call to a static method, or access to a
 * non-constant static field). Merely referencing a class literal
 * ({@code MyStrategy.class}) does NOT trigger initialization - it only
 * resolves the class metadata. There is no automatic classpath scanning in
 * plain Java (unlike e.g. Spring's component scanning). Therefore, this
 * registry keeps a single, explicit list of fully qualified class names in
 * {@link #ensureInitialized()} and loads them via {@link Class#forName(String)}
 * (which does initialize the class), whose only purpose is to trigger the
 * self-registration of every known implementation. This is the only place in
 * the whole codebase that needs to know all concrete strategy classes by
 * name; every other piece of code (e.g. UI components) can simply query this
 * registry for metadata instead of hard-coding the list of available update
 * sources.
 *
 * This class is thread-safe.
 */
public final class UpdateProviderStrategyRegistry {

    private static final Map<String, UpdateProviderStrategyDescriptor> descriptors =
            new LinkedHashMap<String, UpdateProviderStrategyDescriptor>();

    private static volatile boolean initialized = false;

    private UpdateProviderStrategyRegistry() {
        // utility class, no instances
    }

    /**
     * Registers (or replaces) the descriptor for an {@link UpdateProviderStrategy}
     * implementation. Intended to be called from a static initializer block of
     * the concrete strategy class itself.
     *
     * @param descriptor the descriptor to register, must not be {@code null}
     */
    public static synchronized void register(UpdateProviderStrategyDescriptor descriptor) {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        descriptors.put(descriptor.getName(), descriptor);
    }

    /**
     * Looks up the descriptor for the given strategy name.
     *
     * @param name the unique identifier of the strategy (see
     *             {@link UpdateProviderStrategy#getName()})
     * @return the matching descriptor, or {@code null} if no strategy with that
     *         name is registered
     */
    public static synchronized UpdateProviderStrategyDescriptor get(String name) {
        ensureInitialized();
        return descriptors.get(name);
    }

    /**
     * Returns all currently registered descriptors, in registration order.
     *
     * @return an unmodifiable snapshot list of all registered descriptors
     */
    public static synchronized List<UpdateProviderStrategyDescriptor> getAll() {
        ensureInitialized();
        return new ArrayList<UpdateProviderStrategyDescriptor>(descriptors.values());
    }

    /**
     * Makes sure that all known {@link UpdateProviderStrategy} implementations
     * have been loaded AND initialized at least once, so that their static
     * self-registration blocks have run. This method is idempotent and cheap
     * to call repeatedly.
     */
    private static void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;

        // IMPORTANT: a plain class literal (e.g. "SomeClass.class") does NOT
        // trigger class initialization in Java - it only resolves the class
        // metadata. Static initializer blocks only run on "active use" (e.g.
        // creating an instance, calling a static method, or accessing a
        // non-constant static field). Therefore we must use
        // Class.forName(name) here, which DOES initialize the class by
        // default, to reliably trigger each strategy's self-registration.
        // Add new strategy classes here so that they are picked up automatically.
        String[] knownStrategyClassNames = new String[] {
                "de.nmichael.efa.core.update.GithubProductionReleaseUpdateProviderStrategy",
                "de.nmichael.efa.core.update.GithubBetaReleaseUpdateProviderStrategy",
                "de.nmichael.efa.core.update.LocalFileUpdateProviderStrategy",
                "de.nmichael.efa.core.update.EfaNMichaelDeUpdateProviderStrategy",
        };
        for (String className : knownStrategyClassNames) {
            try {
                Class.forName(className); // default initialize=true runs the static block
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "Known UpdateProviderStrategy implementation not found on classpath: " + className, e);
            }
        }
    }
}
