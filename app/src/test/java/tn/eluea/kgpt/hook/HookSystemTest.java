package tn.eluea.kgpt.hook;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

public class HookSystemTest {

    // Dummy class to simulate target methods
    public static class SampleTarget {
        public String greet(String name) {
            return "Hello " + name;
        }

        public int calculate(int a, int b) {
            return a + b;
        }

        public void doWork() {
            // no-op
        }
    }

    @Test
    public void testFindMethodExact() {
        Method method = HookManager.findMethod(SampleTarget.class, "greet", new Class<?>[]{String.class});
        Assert.assertNotNull("findMethod should locate greet(String)", method);
        Assert.assertEquals("greet", method.getName());
    }

    @Test
    public void testFindMethodFuzzy() {
        Method method = HookManager.findMethod(SampleTarget.class, "calculate", null);
        Assert.assertNotNull("findMethod should fuzzy match calculate", method);
        Assert.assertEquals("calculate", method.getName());
    }

    @Test
    public void testMethodHookParamResultOverride() {
        Method method = HookManager.findMethod(SampleTarget.class, "greet", new Class<?>[]{String.class});
        SampleTarget target = new SampleTarget();

        MethodHook.MethodHookParam param = new MethodHook.MethodHookParam(method, target, new Object[]{"World"});
        Assert.assertEquals(1, param.getArgs().length);
        Assert.assertEquals("World", param.getArgs()[0]);
        Assert.assertFalse(param.isReturnEarly());

        // Override result
        param.setResult("Overridden");
        Assert.assertTrue(param.isReturnEarly());
        Assert.assertEquals("Overridden", param.getResult());
    }

    @Test
    public void testMethodHookParamThrowableOverride() {
        Method method = HookManager.findMethod(SampleTarget.class, "doWork", new Class<?>[]{});
        SampleTarget target = new SampleTarget();

        MethodHook.MethodHookParam param = new MethodHook.MethodHookParam(method, target, new Object[]{});
        Assert.assertFalse(param.hasThrowable());

        param.setThrowable(new IllegalStateException("Simulated Error"));
        Assert.assertTrue(param.hasThrowable());
        Assert.assertTrue(param.isReturnEarly());
        Assert.assertEquals("Simulated Error", param.getThrowable().getMessage());
    }

    @Test
    public void testMethodHookCallbacks() {
        AtomicBoolean beforeCalled = new AtomicBoolean(false);
        AtomicBoolean afterCalled = new AtomicBoolean(false);

        MethodHook hook = new MethodHook(
                p -> beforeCalled.set(true),
                p -> afterCalled.set(true)
        );

        Method method = HookManager.findMethod(SampleTarget.class, "doWork", new Class<?>[]{});
        MethodHook.MethodHookParam param = new MethodHook.MethodHookParam(method, new SampleTarget(), new Object[]{});

        hook.callBefore(param);
        Assert.assertTrue("before callback should be called", beforeCalled.get());

        hook.callAfter(param);
        Assert.assertTrue("after callback should be called", afterCalled.get());
    }
}
