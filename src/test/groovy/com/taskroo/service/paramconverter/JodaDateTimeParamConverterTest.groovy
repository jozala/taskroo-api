package com.taskroo.service.paramconverter
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spock.lang.Specification

class JodaDateTimeParamConverterTest extends Specification {

    JodaDateTimeParamConverter converter;

    void setup() {
        converter = new JodaDateTimeParamConverter();

    }

    def "convert ISO date time string to DateTime object"() {
        given:
        def isoStringDate = '2015-01-15T12:34'
        when:
        DateTime dateTime = converter.fromString(isoStringDate)
        then:
        dateTime.isEqual(DateTime.parse(isoStringDate, ISODateTimeFormat.dateTimeParser()))
    }

    def "convert DateTime object to ISO date time string"() {
        given:
        def dateTime = DateTime.parse('2015-01-13T01:22')
        when:
        def dateIsoString = converter.toString(dateTime)
        then:
        dateIsoString.startsWith('2015-01-13T01:22:00')
    }
}
