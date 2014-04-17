package pl.aetas.gtweb.data
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import spock.lang.Specification

class DbTasksConverterTest extends Specification {

    DbTasksConverter dbTasksConverter

    List<DBObject> dbTasks = [new BasicDBObject([_id:'1',  title:'title 1',  finished: false, created_date: new Date(), owner_id: 'ownerId', path: null]),
                              new BasicDBObject([_id:'9',  title:'title 9',  finished: false, created_date: new Date(), owner_id: 'ownerId', path: null]),
                              new BasicDBObject([_id:'10', title:'title 10', finished: false, created_date: new Date(), owner_id: 'ownerId', path: null]),
                              new BasicDBObject([_id:'2',  title:'title 2',  finished: false, created_date: new Date(), owner_id: 'ownerId', path: '1,']),
                              new BasicDBObject([_id:'3',  title:'title 3',  finished: false, created_date: new Date(), owner_id: 'ownerId', path: '1,']),
                              new BasicDBObject([_id:'5',  title:'title 5',  finished: false, created_date: new Date(), owner_id: 'ownerId', path: '1,']),
                              new BasicDBObject([_id:'4',  title:'title 4',  finished: false, created_date: new Date(), owner_id: 'ownerId', path: '1,3,']),
                              new BasicDBObject([_id:'6',  title:'title 6',  finished: false, created_date: new Date(), owner_id: 'ownerId', path: '1,5,']),
                              new BasicDBObject([_id:'7',  title:'title 7',  finished: false, created_date: new Date(), owner_id: 'ownerId', path: '1,5,6,']),
                              new BasicDBObject([_id:'8',  title:'title 8',  finished: false, created_date: new Date(), owner_id: 'ownerId', path: '1,5,6,']),
                              new BasicDBObject([_id:'11', title:'title 11', finished: false, created_date: new Date(), owner_id: 'ownerId', path: '10,']),
                              new BasicDBObject([_id:'12', title:'title 12', finished: false, created_date: new Date(), owner_id: 'ownerId', path: '10,11,'])]

    void setup() {
        dbTasksConverter = new DbTasksConverter();
    }

    def "should put tasks with one element in path as top level tasks"() {
        when:
        def tasks = dbTasksConverter.convertToTasksTree(dbTasks, Collections.emptyList())
        then:
        tasks.collect { it.id } as Set == ['1','9','10'] as Set
    }

    def "should add subtasks of top level tasks when subtasks given"() {
        when:
        def tasks = dbTasksConverter.convertToTasksTree(dbTasks, Collections.emptyList())
        then:
        tasks.find { it.id == '1'}.subtasks.collect { it.id } as Set == ['2','3','5'] as Set
        tasks.find { it.id == '10'}.subtasks.collect { it.id } == ['11']
    }

    def "should add lower level subtasks to proper tasks"() {
        when:
        def tasks = dbTasksConverter.convertToTasksTree(dbTasks, Collections.emptyList())
        then:
        tasks.find { it.id == '1'}.subtasks.find {it.id == '3'}.subtasks.collect { it.id } == ['4']
        tasks.find { it.id == '1'}.subtasks.find {it.id == '5'}.subtasks.collect { it.id } == ['6']
        tasks.find { it.id == '1'}.subtasks.find {it.id == '5'}.subtasks.find {it.id == '6' }.subtasks.collect { it.id } as Set == ['7','8'] as Set
        tasks.find { it.id == '10'}.subtasks.find {it.id == '11'}.subtasks.collect { it.id } == ['12']
    }

    def "should set parentId for the tasks when task is a subtask"() {
        when:
        def tasks = dbTasksConverter.convertToTasksTree(dbTasks, Collections.emptyList())
        then:
        def topLevelTask = tasks.find { it.id == '1' }
        topLevelTask.subtasks.each { assert it.parentTask == topLevelTask}
    }

    def "should add tags to tasks when specified in the DB objects"() {
        given:
        def dbTask = new BasicDBObject([_id:'1',  title:'title 1',  finished: false, created_date: new Date(),
                                        owner_id: 'ownerId', path: null])
    }
}
