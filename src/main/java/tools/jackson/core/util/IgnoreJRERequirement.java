package tools.jackson.core.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method that the animal-sniffer check (see {@code pom.xml}) skips.
 * Only for calls that the Android API baseline has but that the check
 * cannot match against its signatures, such as the signature-polymorphic
 * {@code MethodHandle.invokeExact}.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@interface IgnoreJRERequirement { }
