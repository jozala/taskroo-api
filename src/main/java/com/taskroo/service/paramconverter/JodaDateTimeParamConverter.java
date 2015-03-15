package com.taskroo.service.paramconverter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
public class JodaDateTimeParamConverter implements ParamConverter<DateTime>, ParamConverterProvider {

    @Override
    public DateTime fromString(String value) {
        return DateTime.parse(value, ISODateTimeFormat.dateTimeParser().withZoneUTC());
    }

    @Override
    public String toString(DateTime value) {
        return value.withZone(DateTimeZone.UTC).toString();
    }

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType.equals(DateTime.class)) {
            return (ParamConverter<T>) this;
        }
        return null;
    }
}
