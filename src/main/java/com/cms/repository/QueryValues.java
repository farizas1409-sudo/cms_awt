package com.cms.repository;

final class QueryValues {
    private QueryValues() {}

    static String key(String value) {
        return value == null ? "" : value.trim();
    }

    static String sqlOp(String operation) {
        return switch (key(operation)) {
            case "GT" -> ">";
            case "LT" -> "<";
            case "LE" -> "<=";
            case "EQ" -> "=";
            default -> ">=";
        };
    }
}
