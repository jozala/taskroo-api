package com.taskroo.service.paramconverter

import org.joda.time.DateTime
import spock.lang.Specification

class JodaDateTimeParamConverterTest extends Specification {

    JodaDateTimeParamConverter converter;

    void setup() {
        converter = new JodaDateTimeParamConverter();

    }

    def "convert milliseconds timestamp string to DateTime object"() {
        given:
        def stringTimestamp = '1421708400000'
        when:
        DateTime dateTime = converter.fromString(stringTimestamp)
        then:
        dateTime.millis == stringTimestamp.toLong()
    }

    def "convert DateTime object to milliseconds timestamp string"() {
        given:
        def dateTime = new DateTime('1421708400000'.toLong())
        when:
        def dateTimestamp = converter.toString(dateTime)
        then:
        dateTimestamp == '1421708400000'
    }
}
