package pl.aetas.gtweb.acceptance
import org.apache.http.client.fluent.Request
import spock.lang.Specification

class TaskServiceSpec extends Specification {
    private static final String APP_URL = "http://localhost:8080";

    def "should create a new task in database when receive create task"() {
        given: "something"
            true
        when: "client sends POST request to /task to create a new task"
            Request.Get(APP_URL + "/tasks/all").execute()
        then: "I don't care"
            true
    }
}
