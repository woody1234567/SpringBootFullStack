package com.example.expensetracker.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Repository method with the literal SQL Server object it calls
 * (stored procedure, function, or view) so {@link LoggingAspect} can log the
 * real SQL object name instead of the Java method name, per .claude/CLAUDE.md
 * section 13.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LoggedOperation {

    String value();
}
