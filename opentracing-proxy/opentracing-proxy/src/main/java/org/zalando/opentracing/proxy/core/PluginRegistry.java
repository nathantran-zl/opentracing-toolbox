package org.zalando.opentracing.proxy.core;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import lombok.AllArgsConstructor;
import org.zalando.opentracing.proxy.intercept.baggage.BaggageInterceptor;
import org.zalando.opentracing.proxy.intercept.injection.Injection;
import org.zalando.opentracing.proxy.intercept.log.LogInterceptor;
import org.zalando.opentracing.proxy.intercept.name.Naming;
import org.zalando.opentracing.proxy.intercept.span.SpanBuilderInterceptor;
import org.zalando.opentracing.proxy.intercept.tag.TagInterceptor;
import org.zalando.opentracing.proxy.listen.baggage.BaggageListener;
import org.zalando.opentracing.proxy.listen.log.LogListener;
import org.zalando.opentracing.proxy.listen.scope.ScopeListener;
import org.zalando.opentracing.proxy.listen.span.SpanListener;
import org.zalando.opentracing.proxy.listen.tag.TagListener;
import org.zalando.opentracing.proxy.spi.Plugin;
import org.zalando.opentracing.proxy.spi.Registry;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.BinaryOperator;

import static com.google.common.collect.ImmutableClassToInstanceMap.copyOf;
import static com.google.common.collect.ImmutableMap.builder;
import static com.google.common.collect.Maps.transformValues;
import static java.util.Collections.emptyList;

@AllArgsConstructor
final class PluginRegistry implements Registry, Plugins {

    private static final Map<Class<? extends Plugin>, Reducer<? extends Plugin>> reducers =
            ImmutableMap.<Class<? extends Plugin>, Reducer<? extends Plugin>>builder()
                    .put(Naming.class, reduce(Naming.DEFAULT, (left, right) -> right))
                    .put(SpanBuilderInterceptor.class, reduce(SpanBuilderInterceptor.DEFAULT, SpanBuilderInterceptor::composite))
                    .put(TagInterceptor.class, reduce(TagInterceptor.DEFAULT, TagInterceptor::composite))
                    .put(LogInterceptor.class, reduce(LogInterceptor.DEFAULT, LogInterceptor::composite))
                    .put(BaggageInterceptor.class, reduce(BaggageInterceptor.DEFAULT, BaggageInterceptor::composite))
                    .put(Injection.class, reduce(Injection.DEFAULT, Injection::composite))
                    .put(SpanListener.class, reduce(SpanListener.DEFAULT, SpanListener::composite))
                    .put(TagListener.class, reduce(TagListener.DEFAULT, TagListener::composite))
                    .put(LogListener.class, reduce(LogListener.DEFAULT, LogListener::composite))
                    .put(BaggageListener.class, reduce(BaggageListener.DEFAULT, BaggageListener::composite))
                    .put(ScopeListener.class, reduce(ScopeListener.DEFAULT, ScopeListener::composite))
            .build();

    private final ClassToInstanceMap<Plugin> plugins;
    private final MyInterceptors interceptors = new MyInterceptors();
    private final MyListeners listeners = new MyListeners();

    PluginRegistry() {
        this(copyOf(transformValues(reducers, reducer ->
                reducer.reduce(emptyList()))));
    }

    @FunctionalInterface
    private interface Reducer<P extends Plugin> {

        default P reduce(final P left, final P right) {
            return reduce(Arrays.asList(left, right));
        }

        P reduce(Collection<P> plugins);

    }

    private static <P extends Plugin> Reducer<P> reduce(
            final P seed, final BinaryOperator<P> merge) {
        return plugins -> plugins.stream().reduce(seed, merge);
    }

    @Override
    public PluginRegistry register(final Plugin plugin) {
        final Builder<Class<? extends Plugin>, Plugin> builder = builder();

        plugins.forEach((type, registered) -> {
            if (type.isInstance(plugin)) {
                @SuppressWarnings("unchecked")
                final Reducer<Plugin> reducer = (Reducer<Plugin>) reducers.get(type);
                builder.put(type, reducer
                        .reduce(registered, plugin));
            } else {
                builder.put(type, registered);
            }
        });

        return new PluginRegistry(copyOf(builder.build()));
    }

    @Override
    public Interceptors interceptors() {
        return interceptors;
    }

    @Override
    public Listeners listeners() {
        return listeners;
    }

    private class MyInterceptors implements Interceptors {

        @Override
        public Naming names() {
            return plugins.getInstance(Naming.class);
        }

        @Override
        public SpanBuilderInterceptor spans() {
            return plugins.getInstance(SpanBuilderInterceptor.class);
        }

        @Override
        public TagInterceptor tags() {
            return plugins.getInstance(TagInterceptor.class);
        }

        @Override
        public LogInterceptor logs() {
            return plugins.getInstance(LogInterceptor.class);
        }

        @Override
        public BaggageInterceptor baggage() {
            return plugins.getInstance(BaggageInterceptor.class);
        }

        @Override
        public Injection injections() {
            return plugins.getInstance(Injection.class);
        }

    }

    private final class MyListeners implements Listeners {

        @Override
        public SpanListener spans() {
            return plugins.getInstance(SpanListener.class);
        }

        @Override
        public TagListener tags() {
            return plugins.getInstance(TagListener.class);
        }

        @Override
        public LogListener logs() {
            return plugins.getInstance(LogListener.class);
        }

        @Override
        public BaggageListener baggage() {
            return plugins.getInstance(BaggageListener.class);
        }

        @Override
        public ScopeListener scopes() {
            return plugins.getInstance(ScopeListener.class);
        }

    }

}
