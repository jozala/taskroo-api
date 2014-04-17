package pl.aetas.gtweb.integration.data
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import org.bson.types.ObjectId
import org.joda.time.DateMidnight
import org.joda.time.DateTime
import pl.aetas.gtweb.data.DbTagConverter
import pl.aetas.gtweb.data.DbTasksConverter
import pl.aetas.gtweb.data.TagDao
import pl.aetas.gtweb.data.TaskDao
import pl.aetas.gtweb.data.UnsupportedDataOperationException
import pl.aetas.gtweb.domain.Tag
import pl.aetas.gtweb.domain.Task

class TaskDaoTest extends IntegrationTestBase {

    TaskDao taskDao

    void setup() {
        cleanup()
        def dbTasksConverter = new DbTasksConverter()
        def tagDao = new TagDao(tagsCollection, new DbTagConverter())
        taskDao = new TaskDao(tasksCollection, tagDao, dbTasksConverter)
    }

    void cleanup() {
        tasksCollection.drop()
        tagsCollection.drop()
    }

    def "should create new task in DB"() {
        given:
        tagsCollection.insert(new BasicDBObject([name:'tagA', owner_id:'mariusz', color:null, visible_in_workview:false]))
        tagsCollection.insert(new BasicDBObject([name:'tagB', owner_id:'mariusz', color:'black', visible_in_workview:true]))
        def parentTaskOne = new Task.TaskBuilder().setOwnerId('mariusz').setTitle('parentTask1').setCreatedDate(new Date()).build()
        def parentTaskTwo = new Task.TaskBuilder().setOwnerId('mariusz').setTitle('parentTask2').setCreatedDate(new Date()).setParentTask(parentTaskOne).build()
        def parentTaskThree = new Task.TaskBuilder().setOwnerId('mariusz').setTitle('parentTask3').setCreatedDate(new Date()).setParentTask(parentTaskTwo).build()
        def task = new Task.TaskBuilder().setOwnerId('mariusz').setTitle('taskTitle')
                .setCreatedDate(new Date())
                .setDescription('desc')
                .addTag(new Tag('123', 'mariusz', 'tagA', null, false))
                .addTag(new Tag('546', 'mariusz', 'tagB', 'black', true))
                .setFinished(false)
                .setStartDate(DateMidnight.parse('2014-02-27').toDate())
                .setDueDate(DateMidnight.parse('2014-03-13').toDate())
                .setParentTask(parentTaskThree)
                .build()
        when:
        taskDao.insert(parentTaskOne)
        taskDao.insert(parentTaskTwo)
        taskDao.insert(parentTaskThree)
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
        taskFromDb.path == "$parentTaskOne.id,$parentTaskTwo.id,$parentTaskThree.id"
    }

    def "should set task id in the returned task object after insert"() {
        given:
        def task = new Task.TaskBuilder().setOwnerId('mariusz').setTitle('taskTitle').setCreatedDate(new Date()).build()
        when:
        def taskAfterUpdate = taskDao.insert(task)
        then:
        taskAfterUpdate.getId() != null
    }

    def "should throw exception when creating task with subtasks"() {
        given:
        def task = new Task.TaskBuilder().setOwnerId('mariusz').setTitle('taskTitle').setCreatedDate(new Date())
                .addSubtask(new Task.TaskBuilder().setOwnerId('mariusz').setTitle('sub task title').setCreatedDate(new Date()).build())
                .build()
        when:
        taskDao.insert(task)
        then:
        thrown(UnsupportedDataOperationException)
    }

    def "should throw exception when trying to insert task with non-existing tag's names"() {
        given:
        def task = new Task.TaskBuilder().setOwnerId('mariusz').setTitle('taskTitle')
                .setCreatedDate(new Date())
                .setDescription('desc')
                .addTag(new Tag('123', 'mariusz', 'tagA', null, false))
                .setFinished(false)
                .setStartDate(DateMidnight.parse('2014-02-27').toDate())
                .setDueDate(DateMidnight.parse('2014-03-13').toDate())
                .build()
        when:
        taskDao.insert(task)
        then:
        thrown(UnsupportedDataOperationException)
    }

    def "should throw exception when trying to insert task without ownerId set"() {
        given:
        def task = new Task.TaskBuilder().setTitle("someTitle").setCreatedDate(new Date()).build()
        when:
        taskDao.insert(task)
        then:
        thrown(UnsupportedDataOperationException)
    }

    def "should set task's created date to now when inserting task and created date is not set"() {
        given: "task without created date set"
        def task = new Task.TaskBuilder().setOwnerId("mariusz").setTitle("title").build();
        when: "trying to insert task"
        def taskAfterUpdate = taskDao.insert(task)
        then:
        def taskInDb = tasksCollection.findOne(new BasicDBObject('_id', new ObjectId(taskAfterUpdate.getId())))
        taskInDb.get('created_date') > DateTime.now().minusSeconds(10).toDate()
    }

    def "should return list of tasks when user of given id has tasks"() {
        given:
        def expectedTasks = (1..5).collect {
            new Task.TaskBuilder().setOwnerId('mariusz').setTitle("taskTitle_$it").setCreatedDate(new Date()).build()
        }
        expectedTasks.each { taskDao.insert(it) }
        when:
        def tasksFromDb = taskDao.findAllByOwnerId('mariusz')
        then:
        tasksFromDb.each {
            assert it.getId() != null
            assert it.getTitle() ==~ /taskTitle_\d/
            assert it.getOwnerId() == 'mariusz'
            assert it.getCreatedDate() > DateTime.now().minusSeconds(10).toDate()
        }
    }

    def "should map task with subtasks from db to task object when retrieving tasks from db"() {
        given:
        tagsCollection.insert(new BasicDBObject([name:'next', owner_id:'mariusz', color:'red', visible_in_workview:true]))
        tagsCollection.insert(new BasicDBObject([name:'project', owner_id:'mariusz', color:'blue', visible_in_workview:true]))
        def task = new Task.TaskBuilder().setOwnerId('mariusz').setTitle('tasksTitle')
                .setDescription('desc')
                .setCreatedDate(DateTime.parse('2014-01-21T12:32:11').toDate())
                .setStartDate(DateMidnight.parse('2014-02-11').toDate())
                .setDueDate(DateMidnight.parse('2014-03-31').toDate())
                .setClosedDate(DateTime.parse('2014-04-13T20:16:32').toDate())
                .setFinished(true)
                .addTag(Tag.TagBuilder.start('mariusz', 'next').color('red').visibleInWorkView(true).build())
                .addTag(Tag.TagBuilder.start('mariusz', 'project').color('blue').visibleInWorkView(true).build())
                .build()
        def topTask = taskDao.insert(task)
        def subtask = new Task.TaskBuilder().setOwnerId('mariusz').setTitle('subtaskTitle')
                .setParentTask(topTask)
                .setCreatedDate(DateTime.parse('2014-04-01T20:12:54').toDate())
                .build()
        taskDao.insert(subtask)
        when:
        def retrievedTask = taskDao.findAllByOwnerId('mariusz').first()
        then:
        retrievedTask.ownerId == 'mariusz'
        retrievedTask.title == 'tasksTitle'
        retrievedTask.description == 'desc'
        retrievedTask.createdDate == DateTime.parse('2014-01-21T12:32:11').toDate()
        retrievedTask.startDate == DateMidnight.parse('2014-02-11').toDate()
        retrievedTask.dueDate == DateMidnight.parse('2014-03-31').toDate()
        retrievedTask.closedDate == DateTime.parse('2014-04-13T20:16:32').toDate()
        retrievedTask.finished == true
        retrievedTask.tags == [Tag.TagBuilder.start('mariusz', 'next').build(), Tag.TagBuilder.start('mariusz', 'project').build()] as Set
        retrievedTask.subtasks.first().title == 'subtaskTitle'
        retrievedTask.subtasks.first().createdDate == DateTime.parse('2014-04-01T20:12:54').toDate()
    }
}