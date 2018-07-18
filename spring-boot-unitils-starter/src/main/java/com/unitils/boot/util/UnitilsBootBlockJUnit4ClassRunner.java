package com.unitils.boot.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.ExpectException;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.test.annotation.TestAnnotationUtils;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.junit4.statements.*;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.unitils.core.TestListener;
import org.unitils.core.Unitils;
import org.unitils.core.junit.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @Author: yangjianzhou
 * @Description:
 * @Date:Created in 2018-07-08
 */
public class UnitilsBootBlockJUnit4ClassRunner extends BlockJUnit4ClassRunner {

    private final TestContextManager testContextManager;

    protected Object test;

    protected TestListener unitilsTestListener;

    private static final Method withRulesMethod;

    private static final Log logger = LogFactory.getLog(UnitilsBootBlockJUnit4ClassRunner.class);

    static {
        Assert.state(ClassUtils.isPresent("org.junit.internal.Throwables", UnitilsBootBlockJUnit4ClassRunner.class.getClassLoader()),
                "UnitilsBootJUnit4ClassRunner requires JUnit 4.12 or higher.");

        Method method = ReflectionUtils.findMethod(UnitilsBootBlockJUnit4ClassRunner.class, "withRules",FrameworkMethod.class, Object.class, Statement.class);
        Assert.state(method != null, "UnitilsBootJUnit4ClassRunner requires JUnit 4.12 or higher");
        ReflectionUtils.makeAccessible(method);
        withRulesMethod = method;
    }

    /**
     * Construct a new {@code UnitilsBootBlockJUnit4ClassRunner} and initialize a
     * {@link TestContextManager} to provide Spring testing functionality to
     * standard JUnit tests.
     *
     * @param clazz the test class to be run
     * @see #createTestContextManager(Class)
     */
    public UnitilsBootBlockJUnit4ClassRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
        if (logger.isDebugEnabled()) {
            logger.debug("UnitilsBootJUnit4ClassRunner constructor called with [" + clazz + "]");
        }
        ensureSpringRulesAreNotPresent(clazz);
        this.testContextManager = createTestContextManager(clazz);
        this.unitilsTestListener = getUnitilsTestListener();
    }

    @Override
    protected Statement classBlock(RunNotifier notifier) {
        Class<?> testClass = getTestClass().getJavaClass();

        Statement statement = super.classBlock(notifier);
        statement = new BeforeTestClassStatement(testClass, unitilsTestListener, statement);
        return statement;
    }

    private static void ensureSpringRulesAreNotPresent(Class<?> testClass) {
        for (Field field : testClass.getFields()) {
            Assert.state(!SpringClassRule.class.isAssignableFrom(field.getType()), () -> String.format(
                    "Detected SpringClassRule field in test class [%s], " +
                            "but SpringClassRule cannot be used with the UnitilsBootBlockJUnit4ClassRunner.", testClass.getName()));
            Assert.state(!SpringMethodRule.class.isAssignableFrom(field.getType()), () -> String.format(
                    "Detected SpringMethodRule field in test class [%s], " +
                            "but SpringMethodRule cannot be used with the UnitilsBootBlockJUnit4ClassRunner.", testClass.getName()));
        }
    }


    protected TestListener getUnitilsTestListener() {
        return Unitils.getInstance().getTestListener();
    }

    /**
     * Create a new {@link TestContextManager} for the supplied test class.
     * <p>Can be overridden by subclasses.
     *
     * @param clazz the test class to be managed
     */
    protected TestContextManager createTestContextManager(Class<?> clazz) {
        return new TestContextManager(clazz);
    }

    /**
     * Get the {@link TestContextManager} associated with this runner.
     */
    protected final TestContextManager getTestContextManager() {
        return this.testContextManager;
    }

    /**
     * Return a description suitable for an ignored test class if the test is
     * disabled via {@code @IfProfileValue} at the class-level, and
     * otherwise delegate to the parent implementation.
     *
     * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Class)
     */
    @Override
    public Description getDescription() {
        if (!ProfileValueUtils.isTestEnabledInThisEnvironment(getTestClass().getJavaClass())) {
            return Description.createSuiteDescription(getTestClass().getJavaClass());
        }
        return super.getDescription();
    }

    /**
     * Check whether the test is enabled in the current execution environment.
     * <p>This prevents classes with a non-matching {@code @IfProfileValue}
     * annotation from running altogether, even skipping the execution of
     * {@code prepareTestInstance()} methods in {@code TestExecutionListeners}.
     *
     * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Class)
     * @see org.springframework.test.annotation.IfProfileValue
     * @see org.springframework.test.context.TestExecutionListener
     */
    @Override
    public void run(RunNotifier notifier) {
        if (!ProfileValueUtils.isTestEnabledInThisEnvironment(getTestClass().getJavaClass())) {
            notifier.fireTestIgnored(getDescription());
            return;
        }
        super.run(notifier);
    }

    /**
     * Wrap the {@link Statement} returned by the parent implementation with a
     * {@code RunBeforeTestClassCallbacks} statement, thus preserving the
     * default JUnit functionality while adding support for the Spring TestContext
     * Framework.
     *
     * @see RunBeforeTestClassCallbacks
     */
    @Override
    protected Statement withBeforeClasses(Statement statement) {
        Statement junitBeforeClasses = super.withBeforeClasses(statement);
        return new RunBeforeTestClassCallbacks(junitBeforeClasses, getTestContextManager());
    }

    /**
     * Wrap the {@link Statement} returned by the parent implementation with a
     * {@code RunAfterTestClassCallbacks} statement, thus preserving the default
     * JUnit functionality while adding support for the Spring TestContext Framework.
     *
     * @see RunAfterTestClassCallbacks
     */
    @Override
    protected Statement withAfterClasses(Statement statement) {
        Statement junitAfterClasses = super.withAfterClasses(statement);
        return new RunAfterTestClassCallbacks(junitAfterClasses, getTestContextManager());
    }

    /**
     * Delegate to the parent implementation for creating the test instance and
     * then allow the {@link #getTestContextManager() TestContextManager} to
     * prepare the test instance before returning it.
     *
     * @see TestContextManager#prepareTestInstance
     */
    @Override
    protected Object createTest() throws Exception {
        Object testInstance = super.createTest();
        getTestContextManager().prepareTestInstance(testInstance);
        return testInstance;
    }

    /**
     * Augment the default JUnit behavior
     * {@linkplain #withPotentialRepeat with potential repeats} of the entire
     * execution chain.
     * <p>Furthermore, support for timeouts has been moved down the execution
     * chain in order to include execution of {@link org.junit.Before @Before}
     * and {@link org.junit.After @After} methods within the timed execution.
     * Note that this differs from the default JUnit behavior of executing
     * {@code @Before} and {@code @After} methods in the main thread while
     * executing the actual test method in a separate thread. Thus, the net
     * effect is that {@code @Before} and {@code @After} methods will be
     * executed in the same thread as the test method. As a consequence,
     * JUnit-specified timeouts will work fine in combination with Spring
     * transactions. However, JUnit-specific timeouts still differ from
     * Spring-specific timeouts in that the former execute in a separate
     * thread while the latter simply execute in the main thread (like regular
     * tests).
     *
     * @see #methodInvoker(FrameworkMethod, Object)
     * @see #withBeforeTestExecutionCallbacks(FrameworkMethod, Object, Statement)
     * @see #withAfterTestExecutionCallbacks(FrameworkMethod, Object, Statement)
     * @see #possiblyExpectingExceptions(FrameworkMethod, Object, Statement)
     * @see #withBefores(FrameworkMethod, Object, Statement)
     * @see #withAfters(FrameworkMethod, Object, Statement)
     * @see #withRulesReflectively(FrameworkMethod, Object, Statement)
     * @see #withPotentialRepeat(FrameworkMethod, Object, Statement)
     * @see #withPotentialTimeout(FrameworkMethod, Object, Statement)
     */
    @Override
    protected Statement methodBlock(FrameworkMethod frameworkMethod) {
        Object testInstance;
        try {
            testInstance = new ReflectiveCallable() {
                @Override
                protected Object runReflectiveCall() throws Throwable {
                    return createTest();
                }
            }.run();
        } catch (Throwable ex) {
            return new Fail(ex);
        }

        Statement statement = methodInvoker(frameworkMethod, testInstance);
        statement = withBeforeTestExecutionCallbacks(frameworkMethod, testInstance, statement);
        statement = withAfterTestExecutionCallbacks(frameworkMethod, testInstance, statement);
        statement = possiblyExpectingExceptions(frameworkMethod, testInstance, statement);
        statement = withBefores(frameworkMethod, testInstance, statement);
        statement = withAfters(frameworkMethod, testInstance, statement);
        statement = withRulesReflectively(frameworkMethod, testInstance, statement);
        statement = withPotentialRepeat(frameworkMethod, testInstance, statement);
        statement = withPotentialTimeout(frameworkMethod, testInstance, statement);

        Method testMethod = frameworkMethod.getMethod();

       // Statement statement = super.methodBlock(method);
        statement = new BeforeTestSetUpStatement(test, testMethod, unitilsTestListener, statement);
        statement = new AfterTestTearDownStatement(unitilsTestListener, statement, test, testMethod);
        return statement;
    }

    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        this.test = test;
        Statement statement = super.methodInvoker(method, test);
        statement = new BeforeTestMethodStatement(unitilsTestListener, statement, method.getMethod(), test);
        statement = new AfterTestMethodStatement(unitilsTestListener, statement, method.getMethod(), test);
        return statement;
    }

    /**
     * Invoke JUnit's private {@code withRules()} method using reflection.
     */
    private Statement withRulesReflectively(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
        Object result = ReflectionUtils.invokeMethod(withRulesMethod, this, frameworkMethod, testInstance, statement);
        Assert.state(result instanceof Statement, "withRules mismatch");
        return (Statement) result;
    }

    /**
     * Perform the same logic as
     * {@link BlockJUnit4ClassRunner#possiblyExpectingExceptions(FrameworkMethod, Object, Statement)}
     * except that the <em>expected exception</em> is retrieved using
     * {@link #getExpectedException(FrameworkMethod)}.
     */
    @Override
    protected Statement possiblyExpectingExceptions(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
        Class<? extends Throwable> expectedException = getExpectedException(frameworkMethod);
        return (expectedException != null ? new ExpectException(next, expectedException) : next);
    }

    /**
     * Get the {@code exception} that the supplied {@linkplain FrameworkMethod
     * test method} is expected to throw.
     * <p>Supports JUnit's {@link Test#expected() @Test(expected=...)} annotation.
     * <p>Can be overridden by subclasses.
     *
     * @return the expected exception, or {@code null} if none was specified
     */
    @Nullable
    protected Class<? extends Throwable> getExpectedException(FrameworkMethod frameworkMethod) {
        Test test = frameworkMethod.getAnnotation(Test.class);
        return (test != null && test.expected() != Test.None.class ? test.expected() : null);
    }

    /**
     * Perform the same logic as
     * {@link BlockJUnit4ClassRunner#withPotentialTimeout(FrameworkMethod, Object, Statement)}
     * but with additional support for Spring's {@code @Timed} annotation.
     * <p>Supports both Spring's {@link org.springframework.test.annotation.Timed @Timed}
     * and JUnit's {@link Test#timeout() @Test(timeout=...)} annotations, but not both
     * simultaneously.
     *
     * @return either a {@link SpringFailOnTimeout}, a {@link FailOnTimeout},
     * or the supplied {@link Statement} as appropriate
     * @see #getSpringTimeout(FrameworkMethod)
     * @see #getJUnitTimeout(FrameworkMethod)
     */
    @Override
    @SuppressWarnings("deprecation")
    protected Statement withPotentialTimeout(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
        Statement statement = null;
        long springTimeout = getSpringTimeout(frameworkMethod);
        long junitTimeout = getJUnitTimeout(frameworkMethod);
        if (springTimeout > 0 && junitTimeout > 0) {
            String msg = String.format("Test method [%s] has been configured with Spring's @Timed(millis=%s) and " +
                    "JUnit's @Test(timeout=%s) annotations, but only one declaration of a 'timeout' is " +
                    "permitted per test method.", frameworkMethod.getMethod(), springTimeout, junitTimeout);
            logger.error(msg);
            throw new IllegalStateException(msg);
        } else if (springTimeout > 0) {
            statement = new SpringFailOnTimeout(next, springTimeout);
        } else if (junitTimeout > 0) {
            statement = FailOnTimeout.builder().withTimeout(junitTimeout, TimeUnit.MILLISECONDS).build(next);
        } else {
            statement = next;
        }

        return statement;
    }

    /**
     * Retrieve the configured JUnit {@code timeout} from the {@link Test @Test}
     * annotation on the supplied {@linkplain FrameworkMethod test method}.
     *
     * @return the timeout, or {@code 0} if none was specified
     */
    protected long getJUnitTimeout(FrameworkMethod frameworkMethod) {
        Test test = frameworkMethod.getAnnotation(Test.class);
        return (test != null && test.timeout() > 0 ? test.timeout() : 0);
    }

    /**
     * Retrieve the configured Spring-specific {@code timeout} from the
     * {@link org.springframework.test.annotation.Timed @Timed} annotation
     * on the supplied {@linkplain FrameworkMethod test method}.
     *
     * @return the timeout, or {@code 0} if none was specified
     * @see TestAnnotationUtils#getTimeout(Method)
     */
    protected long getSpringTimeout(FrameworkMethod frameworkMethod) {
        return TestAnnotationUtils.getTimeout(frameworkMethod.getMethod());
    }

    /**
     * Wrap the supplied {@link Statement} with a {@code RunBeforeTestExecutionCallbacks}
     * statement, thus preserving the default functionality while adding support for the
     * Spring TestContext Framework.
     *
     * @see RunBeforeTestExecutionCallbacks
     */
    protected Statement withBeforeTestExecutionCallbacks(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
        return new RunBeforeTestExecutionCallbacks(statement, testInstance, frameworkMethod.getMethod(), getTestContextManager());
    }

    /**
     * Wrap the supplied {@link Statement} with a {@code RunAfterTestExecutionCallbacks}
     * statement, thus preserving the default functionality while adding support for the
     * Spring TestContext Framework.
     *
     * @see RunAfterTestExecutionCallbacks
     */
    protected Statement withAfterTestExecutionCallbacks(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
        return new RunAfterTestExecutionCallbacks(statement, testInstance, frameworkMethod.getMethod(), getTestContextManager());
    }

    /**
     * Wrap the {@link Statement} returned by the parent implementation with a
     * {@code RunBeforeTestMethodCallbacks} statement, thus preserving the
     * default functionality while adding support for the Spring TestContext
     * Framework.
     *
     * @see RunBeforeTestMethodCallbacks
     */
    @Override
    protected Statement withBefores(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
        Statement junitBefores = super.withBefores(frameworkMethod, testInstance, statement);
        return new RunBeforeTestMethodCallbacks(junitBefores, testInstance, frameworkMethod.getMethod(), getTestContextManager());
    }

    /**
     * Wrap the {@link Statement} returned by the parent implementation with a
     * {@code RunAfterTestMethodCallbacks} statement, thus preserving the
     * default functionality while adding support for the Spring TestContext
     * Framework.
     *
     * @see RunAfterTestMethodCallbacks
     */
    @Override
    protected Statement withAfters(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
        Statement junitAfters = super.withAfters(frameworkMethod, testInstance, statement);
        return new RunAfterTestMethodCallbacks(junitAfters, testInstance, frameworkMethod.getMethod(), getTestContextManager());
    }

    /**
     * Wrap the supplied {@link Statement} with a {@code SpringRepeat} statement.
     * <p>Supports Spring's {@link org.springframework.test.annotation.Repeat @Repeat}
     * annotation.
     *
     * @see TestAnnotationUtils#getRepeatCount(Method)
     * @see SpringRepeat
     */
    protected Statement withPotentialRepeat(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
        return new SpringRepeat(next, frameworkMethod.getMethod());
    }

}
