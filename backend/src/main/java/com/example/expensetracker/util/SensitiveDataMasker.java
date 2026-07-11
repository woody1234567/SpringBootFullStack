package com.example.expensetracker.util;

import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Redacts sensitive values before they are written to logs, per
 * .claude/CLAUDE.md section 13 (never log plaintext passwords, password
 * hashes, JWT tokens, session IDs, or confidential PII). Used by
 * {@link com.example.expensetracker.aspect.LoggingAspect} to describe method
 * arguments and return values without ever calling a domain object's own
 * {@code toString()}, since that could leak a field this class doesn't know
 * about (e.g. a record component added later).
 */
public final class SensitiveDataMasker {

    private static final int MAX_DEPTH = 4;
    private static final String MASK = "***";
    private static final Set<String> FULL_MASK_KEYWORDS = Set.of(
            "password", "token", "jwt", "secret", "hash", "sessionid", "credential", "authorization");

    private SensitiveDataMasker() {
    }

    public static String describeArgs(String[] paramNames, Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            String name = (paramNames != null && i < paramNames.length) ? paramNames[i] : "arg" + i;
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(name).append('=').append(describe(name, args[i], 0));
        }
        return sb.toString();
    }

    public static String describeResult(Object value) {
        return describe(null, value, 0);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return MASK;
        }
        int at = email.indexOf('@');
        return email.charAt(0) + MASK + email.substring(at);
    }

    private static String describe(String name, Object value, int depth) {
        if (value == null) {
            return "null";
        }
        if (depth >= MAX_DEPTH) {
            return value.getClass().getSimpleName() + "[...]";
        }
        if (isSensitiveName(name)) {
            return maskValue(name, value);
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean
                || value instanceof Enum<?> || value instanceof LocalDate || value instanceof LocalDateTime
                || value instanceof BigDecimal) {
            return String.valueOf(value);
        }
        if (value instanceof Optional<?> optional) {
            return optional.isPresent() ? describe(null, optional.get(), depth + 1) : "empty";
        }
        if (value instanceof Map<?, ?> map) {
            return "Map[size=" + map.size() + "]";
        }
        if (value instanceof Collection<?> collection) {
            return value.getClass().getSimpleName() + "[size=" + collection.size() + "]";
        }
        if (value instanceof MultipartFile file) {
            return "MultipartFile[name=" + file.getOriginalFilename() + ", size=" + file.getSize() + "]";
        }
        if (value instanceof org.springframework.http.ResponseEntity<?> responseEntity) {
            return "ResponseEntity[status=" + responseEntity.getStatusCode().value()
                    + ", body=" + describe(null, responseEntity.getBody(), depth + 1) + "]";
        }
        if (value.getClass().isRecord()) {
            return describeRecord(value, depth);
        }
        return value.getClass().getSimpleName();
    }

    private static String describeRecord(Object record, int depth) {
        RecordComponent[] components = record.getClass().getRecordComponents();
        StringBuilder sb = new StringBuilder(record.getClass().getSimpleName()).append('[');
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            if (i > 0) {
                sb.append(", ");
            }
            try {
                Object componentValue = component.getAccessor().invoke(record);
                sb.append(component.getName()).append('=').append(describe(component.getName(), componentValue, depth + 1));
            } catch (ReflectiveOperationException ex) {
                sb.append(component.getName()).append('=').append("<unreadable>");
            }
        }
        return sb.append(']').toString();
    }

    private static boolean isSensitiveName(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("email")) {
            return true;
        }
        for (String keyword : FULL_MASK_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String maskValue(String name, Object value) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("email") && value instanceof String email) {
            return maskEmail(email);
        }
        return MASK;
    }
}
