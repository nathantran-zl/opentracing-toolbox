package org.zalando.opentracing.proxy;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.zalando.opentracing.proxy.core.ProxyTracer;
import org.zalando.opentracing.proxy.listen.scope.ScopeListener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ScopeListenerTest {

    private final ScopeListener listener = mock(ScopeListener.class,
            InvocationOnMock::callRealMethod);

    private final Tracer unit = new ProxyTracer(new MockTracer())
            .with(listener);

    @Test
    void shouldDelegateActivated() {
        final Span span = unit.buildSpan("test").start();

        try (final Scope scope = unit.activateSpan(span)) {
            verify(listener).onActivated(scope, span);
        }
    }

    @Test
    void shouldDelegateClosed() {
        final Span span = unit.buildSpan("test").start();
        final Scope scope = unit.activateSpan(span);

        scope.close();

        verify(listener).onClosing(scope, span);
        verify(listener).onClosed(scope, span);
    }

}
