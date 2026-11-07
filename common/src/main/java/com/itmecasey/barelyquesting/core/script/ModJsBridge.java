package com.itmecasey.barelyquesting.core.script;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

/**
 * Gives ModJS scripts a global {@code BarelyQuesting} (the {@link QuestApi}). ModJS is optional, so everything here goes through
 * reflection: ModJS offers {@code ExtraBindings.register(BindingProvider)} and the provider's {@code install(type, cx, scope)} adds
 * globals to every script scope. Without ModJS nothing is loaded and nothing fails.
 *
 * <p>In scripts: {@code BarelyQuesting.registerTask("fly", ctx => ctx.y() > 200 ? ctx.target() : ctx.current())}.
 */
public final class ModJsBridge {
    private static final Logger LOG = Logger.getLogger("barelyquesting");

    private ModJsBridge() {
    }

    /** Installs the binding when ModJS is on the classpath; returns whether it did. */
    public static boolean install(QuestApi api) {
        try {
            Class<?> provider = Class.forName("com.itmecasey.modjs.script.BindingProvider");
            Class<?> extra = Class.forName("com.itmecasey.modjs.script.ExtraBindings");
            Class<?> context = Class.forName("org.mozilla.javascript.Context");
            Class<?> scriptable = Class.forName("org.mozilla.javascript.Scriptable");
            Class<?> scriptableObject = Class.forName("org.mozilla.javascript.ScriptableObject");
            Method javaToJs = context.getMethod("javaToJS", Object.class, scriptable);
            Method put = scriptableObject.getMethod("putProperty", scriptable, String.class, Object.class);
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("install") && args != null && args.length == 3) {
                    put.invoke(null, args[2], "BarelyQuesting", javaToJs.invoke(null, api, args[2]));
                    return null;
                }
                if (method.getName().equals("toString")) {
                    return "BarelyQuesting binding";
                }
                if (method.getName().equals("hashCode")) {
                    return System.identityHashCode(proxy);
                }
                if (method.getName().equals("equals")) {
                    return proxy == args[0];
                }
                return null;
            };
            Object binding = Proxy.newProxyInstance(provider.getClassLoader(), new Class<?>[] {provider}, handler);
            extra.getMethod("register", provider).invoke(null, binding);
            LOG.info("Barely Questing: ModJS scripts can use the BarelyQuesting global");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warning("Barely Questing: could not add the BarelyQuesting global to ModJS: " + e);
            return false;
        }
    }
}
