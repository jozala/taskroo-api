package com.taskroo.service.paramconverter;

import org.joda.time.DateTime;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
public class JodaDateTimeParamConverter implements ParamConverter<DateTime>, ParamConverterProvider {

    @Override
    public DateTime fromString(String value) {
        return new DateTime(Long.parseLong(value));
    }

    @Override
    public String toString(DateTime value) {
        return Long.toString(value.getMillis());
    }

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType.equals(DateTime.class)) {
            return (ParamConverter<T>) this;
        }
        return null;
    }
}
