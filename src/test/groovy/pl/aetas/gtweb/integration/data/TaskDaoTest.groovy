package pl.aetas.gtweb.integration.data
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import org.bson.types.ObjectId
import org.joda.time.DateMidnight
import pl.aetas.gtweb.data.TaskDao
import pl.aetas.gtweb.data.UnsupportedDataOperationException
import pl.aetas.gtweb.domain.Tag
import pl.aetas.gtweb.domain.Task

class TaskDaoTest extends IntegrationTestBase {

    TaskDao taskDao

    void setup() {
        taskDao = new TaskDao(tasksCollection)
    }

    def "should create new task in DB"() {
        given:
        def parentTaskOne = Task.TaskBuilder.start('mariusz', 'parentTask').setId('one').createTask()
        def parentTaskTwo = Task.TaskBuilder.start('mariusz', 'parentTask').setId('two').setParentTask(parentTaskOne)
                .createTask()
        def parentTaskThree = Task.TaskBuilder.start('mariusz', 'parentTask').setId('three')
                .setParentTask(parentTaskTwo).createTask()
        def task = Task.TaskBuilder.start('mariusz', 'taskTitle')
                .setDescription('desc')
                .addTag(new Tag('mariusz', 'tagA', null, false))
                .addTag(new Tag('mariusz', 'tagB', 'black', true))
                .setFinished(false)
                .setStartDate(DateMidnight.parse('2014-02-27').toDate())
                .setDueDate(DateMidnight.parse('2014-03-13').toDate())
                .setParentTask(parentTaskThree)
                .createTask()
        when:
        taskDao.insert(task)
        then:
        DBObject taskFromDb = tasksCollection.findOne(new BasicDBObject('_id', new ObjectId(task.getId())))
        taskFromDb.owner_id == 'mariusz'
        taskFromDb.title == 'taskTitle'
        taskFromDb.description == 'desc'
        taskFromDb.tags.size() == 2
        taskFromDb.finished == false
        taskFromDb.closed_date == null
        taskFromDb.start_date == DateMidnight.parse('2014-02-27').toDate()
        taskFromDb.due_date == DateMidnight.parse('2014-03-13').toDate()
        taskFromDb.subtasks == []
        taskFromDb.ancestors == ['one', 'two', 'three']
    }

    def "should set task id to returned task object from db object"() {
        given:
        def task = Task.TaskBuilder.start('mariusz', 'taskTitle').createTask()
        when:
        def taskAfterUpdate = taskDao.insert(task)
        then:
        taskAfterUpdate.getId() != null
    }

    def "should throw exception when creating task with subtasks"() {
        given:
        def task = Task.TaskBuilder.start('mariusz', 'taskTitle')
                .addSubtask(Task.TaskBuilder.start('mariusz', 'sub task title').createTask())
                .createTask()
        when:
        taskDao.insert(task)
        then:
        thrown(UnsupportedDataOperationException)
    }
}