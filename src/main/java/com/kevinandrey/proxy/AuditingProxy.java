package com.kevinandrey.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Dynamic proxy built with {@link java.lang.reflect.Proxy}: one handler audits any interface without
 * writing a proxy class per type.
 */
public final class AuditingProxy {

    private AuditingProxy() {
    }

    /**
     * Wraps a target so every interface call is reported to a sink.
     *
     * @param type interface to expose
     * @param target object that does the work
     * @param sink receives one line per call, such as {@code findById(INF-2018) -> Optional[...]}
     * @param <T> interface type
     * @return a proxy implementing {@code type}
     */
    public static <T> T wrap(Class<T> type, T target, Consumer<String> sink) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(sink, "sink");
        if (!type.isInterface()) {
            throw new IllegalArgumentException(type.getName() + " is not an interface");
        }
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, new Handler(target, sink));

        return type.cast(proxy);
    }

    private record Handler(Object target, Consumer<String> sink) implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return objectMethod(proxy, method, args);
            }
            String call = method.getName() + "(" + render(args) + ")";
            try {
                Object result = method.invoke(target, args);
                sink.accept(call + " -> " + result);

                return result;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                sink.accept(call + " threw " + cause.getClass().getSimpleName());
                throw cause;
            }
        }

        private Object objectMethod(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                default -> "AuditingProxy[" + target + "]";
            };
        }

        private static String render(Object[] args) {
            return args == null ? "" : Arrays.stream(args).map(String::valueOf).collect(Collectors.joining(", "));
        }
    }
}
