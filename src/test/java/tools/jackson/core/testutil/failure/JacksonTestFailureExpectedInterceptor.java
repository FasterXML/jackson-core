package tools.jackson.core.testutil.failure;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * Custom {@link InvocationInterceptor} that intercepts test method invocation.
 * To pass the test ***only if*** test fails with an exception, and fail the test otherwise.
 *
 * @since 2.19
 */
public class JacksonTestFailureExpectedInterceptor
    implements InvocationInterceptor
{
    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
        throws Throwable
    {
        try {
            invocation.proceed();
        } catch (Throwable t) {
            // do-nothing, we do expect an exception
            return;
        }
        handleUnexpectePassingTest(invocationContext);
    }

    /**
     * Interceptor API method that is called to intercept test template
     * method invocation, like {@link org.junit.jupiter.params.ParameterizedTest}.
     *<p>
     * Before failing passing test case, it checks if the test should pass according to the
     * {@link ExpectedPassingTestCasePredicate} provided in the
     * {@link JacksonTestFailureExpected} annotation.
     */
    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
        throws Throwable
    {

        try {
            invocation.proceed();
        } catch (Throwable t) {
            // do-nothing, we do expect an exception
            return;
        }

        // Before we fail the test for passing,
        // check if the test should pass according to the predicate given
        ExpectedPassingTestCasePredicate predicate = findzExpectedPassingTestCasePredicate(invocationContext);
        if (predicate != null) {
            if (predicate.shouldPass(invocationContext.getArguments())) {
                // do-nothing, we do not expect an exception
                return;
            }
        }
        handleUnexpectePassingTest(invocationContext);
    }

    private void handleUnexpectePassingTest(ReflectiveInvocationContext<Method> invocationContext) {
        // Collect information we need
        Object targetClass = invocationContext.getTargetClass();
        Object testMethod = invocationContext.getExecutable().getName();
        //List<Object> arguments = invocationContext.getArguments();

        // Create message
        String message = String.format("Test method %s.%s() passed, but should have failed", targetClass, testMethod);

        // throw exception
        throw new JacksonTestShouldFailException(message);
    }

    /**
     * @return null if the default predicate is found,
     *       otherwise instance of the {@link ExpectedPassingTestCasePredicate}.
     */
    private ExpectedPassingTestCasePredicate findzExpectedPassingTestCasePredicate(
            ReflectiveInvocationContext<Method> invocationContext)
            throws InstantiationException, IllegalAccessException
    {
        JacksonTestFailureExpected annotation = invocationContext.getExecutable().getAnnotation(JacksonTestFailureExpected.class);
        Class<? extends ExpectedPassingTestCasePredicate> predicate = annotation.expectedPassingTestCasePredicate();
        if (ExpectedPassingTestCasePredicate.class.equals(predicate)) {
            return null;
        } else {
            return predicate.newInstance();
        }
    }

}
