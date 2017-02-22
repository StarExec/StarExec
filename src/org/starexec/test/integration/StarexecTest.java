package org.starexec.test.integration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tag applied to all test functions in all TestSequence subclasses. All functions tagged
 * with @StarexecTest will be executed by the integration test runner.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface StarexecTest {
	
}
