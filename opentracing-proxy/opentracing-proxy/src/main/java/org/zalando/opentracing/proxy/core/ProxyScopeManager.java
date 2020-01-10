package org.zalando.opentracing.proxy.core;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import lombok.AllArgsConstructor;
import org.zalando.opentracing.proxy.base.ForwardingScopeManager;
import org.zalando.opentracing.proxy.base.ForwardingSpan;

import javax.annotation.Nullable;

@AllArgsConstructor
final class ProxyScopeManager implements ForwardingScopeManager {

    private final ScopeManager delegate;
    private final Plugins plugins;

    @Override
    public ScopeManager delegate() {
        return delegate;
    }

    @Override
    public Scope activate(@Nullable final Span span) {
        if (span == null) {
            return delegate.activate(null);
        }

        // we rely on the delegate to support any Span here
        final Scope original = delegate.activate(span);
        final ProxyScope scope = new ProxyScope(original, span, plugins);
        plugins.listeners().scopes().onActivated(scope, span);
        return scope;
    }

    @Nullable
    @Override
    public Span activeSpan() {
        @Nullable final Span span = delegate.activeSpan();

        if (span == null) {
            return null;
        }

        assert span instanceof ForwardingSpan;
        return span;
    }

}
