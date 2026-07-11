package com.example.expensetracker.aspect;

import com.example.expensetracker.exception.BusinessException;
import com.example.expensetracker.exception.DuplicateResourceException;
import com.example.expensetracker.exception.ForbiddenException;
import com.example.expensetracker.exception.ResourceNotFoundException;
import com.example.expensetracker.exception.UnauthorizedException;
import com.example.expensetracker.exception.ValidationException;
import com.example.expensetracker.util.SensitiveDataMasker;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Centralized method entry/exit/exception logging for the Repository, Service,
 * and Controller layers, replacing per-method manual log.info/log.error calls
 * (.claude/CLAUDE.md section 13). Nested layers (controller -> service ->
 * repository) each log their own ENTER/EXIT/EXCEPTION independently, so a
 * single request naturally produces a stack of paired log lines per layer -
 * this is intended, not duplication.
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    private static final Set<Class<?>> EXPECTED_EXCEPTIONS = Set.of(
            ValidationException.class,
            ResourceNotFoundException.class,
            DuplicateResourceException.class,
            BadCredentialsException.class,
            UnauthorizedException.class,
            ForbiddenException.class,
            BusinessException.class);

    @Pointcut("execution(public * com.example.expensetracker.repository.impl..*.*(..))")
    private void repositoryLayer() {
    }

    @Pointcut("execution(public * com.example.expensetracker.service..*.*(..))")
    private void serviceLayer() {
    }

    @Pointcut("execution(public * com.example.expensetracker.controller..*.*(..))")
    private void controllerLayer() {
    }

    @Pointcut("repositoryLayer() || serviceLayer() || controllerLayer()")
    private void loggedLayer() {
    }

    @Around("loggedLayer()")
    public Object logInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String label = resolveLabel(signature);
        String args = SensitiveDataMasker.describeArgs(signature.getParameterNames(), joinPoint.getArgs());

        log.info("ENTER {} args=[{}]", label, args);
        long startNanos = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("EXIT {} durationMs={} result={}", label, durationMs, SensitiveDataMasker.describeResult(result));
            return result;
        } catch (Throwable ex) {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            if (EXPECTED_EXCEPTIONS.contains(ex.getClass())) {
                log.warn("EXCEPTION {} durationMs={} args=[{}] exceptionType={} message={}",
                        label, durationMs, args, ex.getClass().getSimpleName(), ex.getMessage());
            } else {
                log.error("EXCEPTION {} durationMs={} args=[{}] exceptionType={} message={}",
                        label, durationMs, args, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            }
            throw ex;
        }
    }

    private String resolveLabel(MethodSignature signature) {
        LoggedOperation annotation = signature.getMethod().getAnnotation(LoggedOperation.class);
        if (annotation != null) {
            return annotation.value();
        }
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }
}
