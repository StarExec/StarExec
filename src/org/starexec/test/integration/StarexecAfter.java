package org.starexec.test.integration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tag applied to all functions that will be executed after every individual test.
 * Useful as a place to put logging, teardown, or other functionality.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface StarexecAfter {
	
}
