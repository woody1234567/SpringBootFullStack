package com.example.expensetracker.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Repository method with the literal Oracle object it calls
 * (stored procedure or function) so {@link LoggingAspect} can log the
 * real database object name instead of the Java method name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LoggedOperation {

    String value();
}
