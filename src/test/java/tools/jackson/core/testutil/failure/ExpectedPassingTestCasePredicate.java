package tools.jackson.core.testutil.failure;

import java.util.List;

/**
 * Interface that defines a predicate to determine if a test case is expected to pass.
 * <p>
 * For internal use by Jackson projects only.
 *
 * @since 2.19
 */
@FunctionalInterface
public interface ExpectedPassingTestCasePredicate {
    boolean shouldPass(List<Object> arguments);
}
