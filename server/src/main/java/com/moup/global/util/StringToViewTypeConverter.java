package com.moup.global.util;

import com.moup.global.common.type.ViewType;
import org.springframework.core.convert.converter.Converter;

public class StringToViewTypeConverter implements Converter<String, ViewType> {
    @Override
    public ViewType convert(String view) {
        return ViewType.valueOf(view.toUpperCase());
    }
}
