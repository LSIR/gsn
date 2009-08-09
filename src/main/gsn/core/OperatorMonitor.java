package gsn.core;

import org.picocontainer.*;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class OperatorMonitor  implements ComponentMonitor, Serializable {

    public <T> Constructor<T> instantiating(PicoContainer container, ComponentAdapter<T> componentAdapter,
                                            Constructor<T> constructor) {
        return constructor;
    }

    public <T> void instantiationFailed(PicoContainer container,
                                        ComponentAdapter<T> componentAdapter,
                                        Constructor<T> constructor,
                                        Exception e) {
    }

    public <T> void instantiated(PicoContainer container, ComponentAdapter<T>  componentAdapter,
                                 Constructor<T>  constructor,
                                 Object instantiated,
                                 Object[] injected,
                                 long duration) {
    }

    public Object invoking(PicoContainer container,
                           ComponentAdapter<?> componentAdapter,
                           Member member,
                           Object instance, Object[] args) {
        return KEEP;
    }

    public void invoked(PicoContainer container,
                        ComponentAdapter<?> componentAdapter,
                        Member member,
                        Object instance,
                        long duration, Object[] args, Object retVal) {
    }

    public void invocationFailed(Member member, Object instance, Exception e) {
     }

    public void lifecycleInvocationFailed(MutablePicoContainer container,
                                          ComponentAdapter<?> componentAdapter, Method method,
                                          Object instance,
                                          RuntimeException cause) {
//        if (cause instanceof PicoLifecycleException) {
//            throw cause;
//        }
//        throw cause;
//        cause.printStackTrace();
        if (!method.getName().equals("stop") && !method.getName().equals("dispose") )
            throw cause;
            
    }

    public Object noComponentFound(MutablePicoContainer container, Object componentKey) {
        return null;
    }

    public Injector newInjector(Injector injector) {
        return injector;
    }

    /** {@inheritDoc} **/
    public Behavior newBehavior(Behavior behavior) {
        return behavior;
    }


}