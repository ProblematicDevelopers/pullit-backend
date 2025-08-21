package com.pullit.item.converter;

import com.pullit.item.enums.AreaCode;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AreaCodeConverter implements Converter<String, AreaCode> {
    @Override
    public AreaCode convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }

        String normalized = source.trim();

        // 1. Enum name (대소문자 무시)
        try {
            return AreaCode.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }

        // 2. title 매칭
        return Arrays.stream(AreaCode.values())
                .filter(ac -> ac.getTitle().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown AreaCode: " + source));
    }
}
