package de.nmichael.efa.core.update;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Immutable metadata descriptor for a concrete {@link UpdateProviderStrategy}
 * implementation.
 *
 * Unlike the {@link UpdateProviderStrategy} interface itself, a descriptor can
 * be queried WITHOUT having to construct an instance of the strategy first.
 * This is important because some strategies (e.g. {@code LocalFileUpdateProviderStrategy}
 * or {@code EfaNMichaelDeUpdateProviderStrategy}) require mandatory constructor
 * arguments (a file path or a URL) that may not be known yet when the UI just
 * wants to show a list of all available update sources.
 *
 * Display name and user hint are stored as {@link Supplier} instead of plain
 * {@link String}, because the underlying texts are usually obtained via
 * {@code International.getString(...)}. Evaluating them lazily (i.e. every
 * time {@link #getDisplayName()} / {@link #getUserHint()} is called) ensures
 * that the correct, currently active UI language is used, even if the user
 * switches the language at runtime after the descriptor has been registered.
 *
 * The descriptor also carries a {@code factory} function that can create an
 * actual {@link UpdateProviderStrategy} instance via {@link #createInstance(String)}.
 * The single {@code String} argument is an optional, strategy-specific piece
 * of configuration (e.g. a local eou.xml file path, or a download URL).
 * Strategies that do not require any configuration simply ignore this argument.
 * This allows callers (e.g. UI code) to instantiate any registered strategy
 * generically, without needing a per-class switch/if-chain.
 */
public final class UpdateProviderStrategyDescriptor {

    private final String name;
    private final Class<? extends UpdateProviderStrategy> strategyClass;
    private final Supplier<String> displayNameSupplier;
    private final Supplier<String> userHintSupplier;
    private final Function<String, UpdateProviderStrategy> factory;

    /**
     * Creates a new descriptor.
     *
     * @param name                the unique identifier of the strategy, must match
     *                            the value returned by {@link UpdateProviderStrategy#getName()}
     * @param strategyClass       the concrete implementation class this descriptor describes
     * @param displayNameSupplier supplier for the (translated) display name
     * @param userHintSupplier    supplier for the (translated) user hint text
     * @param factory             creates a new strategy instance from an optional,
     *                            strategy-specific configuration string (may be
     *                            ignored by strategies that do not need one)
     */
    public UpdateProviderStrategyDescriptor(String name,
            Class<? extends UpdateProviderStrategy> strategyClass,
            Supplier<String> displayNameSupplier,
            Supplier<String> userHintSupplier,
            Function<String, UpdateProviderStrategy> factory) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must not be null or empty");
        }
        if (strategyClass == null) {
            throw new IllegalArgumentException("strategyClass must not be null");
        }
        if (displayNameSupplier == null) {
            throw new IllegalArgumentException("displayNameSupplier must not be null");
        }
        if (userHintSupplier == null) {
            throw new IllegalArgumentException("userHintSupplier must not be null");
        }
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }
        this.name = name;
        this.strategyClass = strategyClass;
        this.displayNameSupplier = displayNameSupplier;
        this.userHintSupplier = userHintSupplier;
        this.factory = factory;
    }

    /**
     * @return the unique identifier of the strategy (same value as
     *         {@link UpdateProviderStrategy#getName()})
     */
    public String getName() {
        return name;
    }

    /**
     * @return the concrete implementation class this descriptor describes
     */
    public Class<? extends UpdateProviderStrategy> getStrategyClass() {
        return strategyClass;
    }

    /**
     * @return the (translated) display name, evaluated at call time
     */
    public String getDisplayName() {
        return displayNameSupplier.get();
    }

    /**
     * @return the (translated) user hint text, evaluated at call time
     */
    public String getUserHint() {
        return userHintSupplier.get();
    }

    /**
     * Creates a new instance of the described {@link UpdateProviderStrategy}.
     *
     * @param config optional, strategy-specific configuration (e.g. a local
     *               file path or a URL). May be {@code null} if the strategy
     *               does not require any configuration, or if a sensible
     *               default should be used by the strategy itself.
     * @return a newly created strategy instance, never {@code null}
     */
    public UpdateProviderStrategy createInstance(String config) {
        return factory.apply(config);
    }

    @Override
    public String toString() {
        return "UpdateProviderStrategyDescriptor[name=" + name
                + ", strategyClass=" + strategyClass.getName() + "]";
    }
}

