package dev.rehm.lingua.common.exception.classes;

import dev.rehm.lingua.common.exception.BaseException;

public class SearxngTimeOutException extends BaseException {

    public SearxngTimeOutException(
            String functionName,
            String fieldName,
            Object value
    ) {
        super(
                functionName,
                fieldName,
                value,
                "SearXNG request timed out",
                "SEARXNG_TIMEOUT"
        );
    }
}
