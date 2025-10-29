package com.moup.server.converter;

import com.moup.server.model.dto.ViewType;
import org.springframework.core.convert.converter.Converter;

public class StringToViewTypeConverter implements Converter<String, ViewType> {
    @Override
    public ViewType convert(String view) {
        return ViewType.valueOf(view.toUpperCase());
    }
}
