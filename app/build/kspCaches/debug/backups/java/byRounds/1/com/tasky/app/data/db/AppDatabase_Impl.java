package com.tasky.app.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile TaskDao _taskDao;

  private volatile ReminderDao _reminderDao;

  private volatile ListDao _listDao;

  private volatile FolderDao _folderDao;

  private volatile TagDao _tagDao;

  private volatile FilterDao _filterDao;

  private volatile HabitDao _habitDao;

  private volatile HabitCheckDao _habitCheckDao;

  private volatile FocusSessionDao _focusSessionDao;

  private volatile CalendarEventDao _calendarEventDao;

  private volatile IcsFeedDao _icsFeedDao;

  private volatile AttachmentDao _attachmentDao;

  private volatile MetaDao _metaDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` TEXT NOT NULL, `parentId` TEXT, `title` TEXT NOT NULL, `note` TEXT NOT NULL, `due` INTEGER, `isDueAllDay` INTEGER NOT NULL, `priority` INTEGER NOT NULL, `listId` TEXT, `recurrenceRule` TEXT, `completedAt` INTEGER, `deletedAt` INTEGER, `createdAt` INTEGER NOT NULL, `modifiedAt` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `focusSeconds` INTEGER NOT NULL, `pomoCount` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_parentId` ON `tasks` (`parentId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_listId` ON `tasks` (`listId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_due` ON `tasks` (`due`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_completedAt` ON `tasks` (`completedAt`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_deletedAt` ON `tasks` (`deletedAt`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_sortOrder` ON `tasks` (`sortOrder`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `reminders` (`id` TEXT NOT NULL, `taskId` TEXT NOT NULL, `at` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_taskId` ON `reminders` (`taskId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_at` ON `reminders` (`at`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `lists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `color` INTEGER NOT NULL, `groupId` TEXT, `sortOrder` INTEGER NOT NULL, `collapsed` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_lists_groupId` ON `lists` (`groupId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `folders` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `collapsed` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `color` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `task_tags` (`taskId` TEXT NOT NULL, `tagId` TEXT NOT NULL, PRIMARY KEY(`taskId`, `tagId`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_tags_taskId` ON `task_tags` (`taskId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_tags_tagId` ON `task_tags` (`tagId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `filters` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `listId` TEXT, `tagId` TEXT, `minPriority` INTEGER NOT NULL, `dateType` INTEGER NOT NULL, `dueStart` INTEGER, `dueEnd` INTEGER, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `habits` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `emoji` TEXT NOT NULL, `color` INTEGER NOT NULL, `category` TEXT, `note` TEXT NOT NULL, `freqType` INTEGER NOT NULL, `weekdays` TEXT NOT NULL, `timesPerWeek` INTEGER NOT NULL, `timesPerMonth` INTEGER NOT NULL, `reminderEnabled` INTEGER NOT NULL, `reminderHour` INTEGER NOT NULL, `reminderMinute` INTEGER NOT NULL, `nagEnabled` INTEGER NOT NULL, `nagHour` INTEGER NOT NULL, `nagMinute` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_sortOrder` ON `habits` (`sortOrder`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `habit_checks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `habitId` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, `status` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_checks_habitId` ON `habit_checks` (`habitId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_checks_epochDay` ON `habit_checks` (`epochDay`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_checks_habitId_epochDay` ON `habit_checks` (`habitId`, `epochDay`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` TEXT NOT NULL, `taskId` TEXT, `type` INTEGER NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER NOT NULL, `seconds` INTEGER NOT NULL, `countedTaskSeconds` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_taskId` ON `focus_sessions` (`taskId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_startedAt` ON `focus_sessions` (`startedAt`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `calendar_events` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `note` TEXT NOT NULL, `location` TEXT, `start` INTEGER NOT NULL, `end` INTEGER NOT NULL, `allDay` INTEGER NOT NULL, `source` INTEGER NOT NULL, `deviceId` INTEGER, `deviceCalendarId` INTEGER, `feedId` TEXT, `linkedTaskId` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_start` ON `calendar_events` (`start`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_end` ON `calendar_events` (`end`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_source` ON `calendar_events` (`source`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_deviceId` ON `calendar_events` (`deviceId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_feedId` ON `calendar_events` (`feedId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `ics_feeds` (`id` TEXT NOT NULL, `url` TEXT NOT NULL, `name` TEXT NOT NULL, `color` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `lastSync` INTEGER, PRIMARY KEY(`id`))");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ics_feeds_url` ON `ics_feeds` (`url`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `attachments` (`id` TEXT NOT NULL, `taskId` TEXT NOT NULL, `fileName` TEXT NOT NULL, `mimeType` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_taskId` ON `attachments` (`taskId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `kv_meta` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'ef7f0da1837bee4861cd79d12b282606')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `tasks`");
        db.execSQL("DROP TABLE IF EXISTS `reminders`");
        db.execSQL("DROP TABLE IF EXISTS `lists`");
        db.execSQL("DROP TABLE IF EXISTS `folders`");
        db.execSQL("DROP TABLE IF EXISTS `tags`");
        db.execSQL("DROP TABLE IF EXISTS `task_tags`");
        db.execSQL("DROP TABLE IF EXISTS `filters`");
        db.execSQL("DROP TABLE IF EXISTS `habits`");
        db.execSQL("DROP TABLE IF EXISTS `habit_checks`");
        db.execSQL("DROP TABLE IF EXISTS `focus_sessions`");
        db.execSQL("DROP TABLE IF EXISTS `calendar_events`");
        db.execSQL("DROP TABLE IF EXISTS `ics_feeds`");
        db.execSQL("DROP TABLE IF EXISTS `attachments`");
        db.execSQL("DROP TABLE IF EXISTS `kv_meta`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsTasks = new HashMap<String, TableInfo.Column>(16);
        _columnsTasks.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("parentId", new TableInfo.Column("parentId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("due", new TableInfo.Column("due", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("isDueAllDay", new TableInfo.Column("isDueAllDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("priority", new TableInfo.Column("priority", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("listId", new TableInfo.Column("listId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("recurrenceRule", new TableInfo.Column("recurrenceRule", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("completedAt", new TableInfo.Column("completedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("deletedAt", new TableInfo.Column("deletedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("modifiedAt", new TableInfo.Column("modifiedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("focusSeconds", new TableInfo.Column("focusSeconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTasks.put("pomoCount", new TableInfo.Column("pomoCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTasks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTasks = new HashSet<TableInfo.Index>(6);
        _indicesTasks.add(new TableInfo.Index("index_tasks_parentId", false, Arrays.asList("parentId"), Arrays.asList("ASC")));
        _indicesTasks.add(new TableInfo.Index("index_tasks_listId", false, Arrays.asList("listId"), Arrays.asList("ASC")));
        _indicesTasks.add(new TableInfo.Index("index_tasks_due", false, Arrays.asList("due"), Arrays.asList("ASC")));
        _indicesTasks.add(new TableInfo.Index("index_tasks_completedAt", false, Arrays.asList("completedAt"), Arrays.asList("ASC")));
        _indicesTasks.add(new TableInfo.Index("index_tasks_deletedAt", false, Arrays.asList("deletedAt"), Arrays.asList("ASC")));
        _indicesTasks.add(new TableInfo.Index("index_tasks_sortOrder", false, Arrays.asList("sortOrder"), Arrays.asList("ASC")));
        final TableInfo _infoTasks = new TableInfo("tasks", _columnsTasks, _foreignKeysTasks, _indicesTasks);
        final TableInfo _existingTasks = TableInfo.read(db, "tasks");
        if (!_infoTasks.equals(_existingTasks)) {
          return new RoomOpenHelper.ValidationResult(false, "tasks(com.tasky.app.data.db.TaskEntity).\n"
                  + " Expected:\n" + _infoTasks + "\n"
                  + " Found:\n" + _existingTasks);
        }
        final HashMap<String, TableInfo.Column> _columnsReminders = new HashMap<String, TableInfo.Column>(3);
        _columnsReminders.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReminders.put("taskId", new TableInfo.Column("taskId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReminders.put("at", new TableInfo.Column("at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysReminders = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesReminders = new HashSet<TableInfo.Index>(2);
        _indicesReminders.add(new TableInfo.Index("index_reminders_taskId", false, Arrays.asList("taskId"), Arrays.asList("ASC")));
        _indicesReminders.add(new TableInfo.Index("index_reminders_at", false, Arrays.asList("at"), Arrays.asList("ASC")));
        final TableInfo _infoReminders = new TableInfo("reminders", _columnsReminders, _foreignKeysReminders, _indicesReminders);
        final TableInfo _existingReminders = TableInfo.read(db, "reminders");
        if (!_infoReminders.equals(_existingReminders)) {
          return new RoomOpenHelper.ValidationResult(false, "reminders(com.tasky.app.data.db.ReminderEntity).\n"
                  + " Expected:\n" + _infoReminders + "\n"
                  + " Found:\n" + _existingReminders);
        }
        final HashMap<String, TableInfo.Column> _columnsLists = new HashMap<String, TableInfo.Column>(6);
        _columnsLists.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLists.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLists.put("color", new TableInfo.Column("color", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLists.put("groupId", new TableInfo.Column("groupId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLists.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLists.put("collapsed", new TableInfo.Column("collapsed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLists = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLists = new HashSet<TableInfo.Index>(1);
        _indicesLists.add(new TableInfo.Index("index_lists_groupId", false, Arrays.asList("groupId"), Arrays.asList("ASC")));
        final TableInfo _infoLists = new TableInfo("lists", _columnsLists, _foreignKeysLists, _indicesLists);
        final TableInfo _existingLists = TableInfo.read(db, "lists");
        if (!_infoLists.equals(_existingLists)) {
          return new RoomOpenHelper.ValidationResult(false, "lists(com.tasky.app.data.db.ListEntity).\n"
                  + " Expected:\n" + _infoLists + "\n"
                  + " Found:\n" + _existingLists);
        }
        final HashMap<String, TableInfo.Column> _columnsFolders = new HashMap<String, TableInfo.Column>(4);
        _columnsFolders.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFolders.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFolders.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFolders.put("collapsed", new TableInfo.Column("collapsed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFolders = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFolders = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFolders = new TableInfo("folders", _columnsFolders, _foreignKeysFolders, _indicesFolders);
        final TableInfo _existingFolders = TableInfo.read(db, "folders");
        if (!_infoFolders.equals(_existingFolders)) {
          return new RoomOpenHelper.ValidationResult(false, "folders(com.tasky.app.data.db.FolderEntity).\n"
                  + " Expected:\n" + _infoFolders + "\n"
                  + " Found:\n" + _existingFolders);
        }
        final HashMap<String, TableInfo.Column> _columnsTags = new HashMap<String, TableInfo.Column>(3);
        _columnsTags.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTags.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTags.put("color", new TableInfo.Column("color", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTags = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTags = new HashSet<TableInfo.Index>(1);
        _indicesTags.add(new TableInfo.Index("index_tags_name", true, Arrays.asList("name"), Arrays.asList("ASC")));
        final TableInfo _infoTags = new TableInfo("tags", _columnsTags, _foreignKeysTags, _indicesTags);
        final TableInfo _existingTags = TableInfo.read(db, "tags");
        if (!_infoTags.equals(_existingTags)) {
          return new RoomOpenHelper.ValidationResult(false, "tags(com.tasky.app.data.db.TagEntity).\n"
                  + " Expected:\n" + _infoTags + "\n"
                  + " Found:\n" + _existingTags);
        }
        final HashMap<String, TableInfo.Column> _columnsTaskTags = new HashMap<String, TableInfo.Column>(2);
        _columnsTaskTags.put("taskId", new TableInfo.Column("taskId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTaskTags.put("tagId", new TableInfo.Column("tagId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTaskTags = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTaskTags = new HashSet<TableInfo.Index>(2);
        _indicesTaskTags.add(new TableInfo.Index("index_task_tags_taskId", false, Arrays.asList("taskId"), Arrays.asList("ASC")));
        _indicesTaskTags.add(new TableInfo.Index("index_task_tags_tagId", false, Arrays.asList("tagId"), Arrays.asList("ASC")));
        final TableInfo _infoTaskTags = new TableInfo("task_tags", _columnsTaskTags, _foreignKeysTaskTags, _indicesTaskTags);
        final TableInfo _existingTaskTags = TableInfo.read(db, "task_tags");
        if (!_infoTaskTags.equals(_existingTaskTags)) {
          return new RoomOpenHelper.ValidationResult(false, "task_tags(com.tasky.app.data.db.TaskTagCrossRef).\n"
                  + " Expected:\n" + _infoTaskTags + "\n"
                  + " Found:\n" + _existingTaskTags);
        }
        final HashMap<String, TableInfo.Column> _columnsFilters = new HashMap<String, TableInfo.Column>(9);
        _columnsFilters.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilters.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilters.put("listId", new TableInfo.Column("listId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilters.put("tagId", new TableInfo.Column("tagId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilters.put("minPriority", new TableInfo.Column("minPriority", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilters.put("dateType", new TableInfo.Column("dateType", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilters.put("dueStart", new TableInfo.Column("dueStart", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilters.put("dueEnd", new TableInfo.Column("dueEnd", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilters.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFilters = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFilters = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFilters = new TableInfo("filters", _columnsFilters, _foreignKeysFilters, _indicesFilters);
        final TableInfo _existingFilters = TableInfo.read(db, "filters");
        if (!_infoFilters.equals(_existingFilters)) {
          return new RoomOpenHelper.ValidationResult(false, "filters(com.tasky.app.data.db.FilterEntity).\n"
                  + " Expected:\n" + _infoFilters + "\n"
                  + " Found:\n" + _existingFilters);
        }
        final HashMap<String, TableInfo.Column> _columnsHabits = new HashMap<String, TableInfo.Column>(19);
        _columnsHabits.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("emoji", new TableInfo.Column("emoji", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("color", new TableInfo.Column("color", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("category", new TableInfo.Column("category", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("freqType", new TableInfo.Column("freqType", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("weekdays", new TableInfo.Column("weekdays", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("timesPerWeek", new TableInfo.Column("timesPerWeek", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("timesPerMonth", new TableInfo.Column("timesPerMonth", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("reminderEnabled", new TableInfo.Column("reminderEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("reminderHour", new TableInfo.Column("reminderHour", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("reminderMinute", new TableInfo.Column("reminderMinute", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("nagEnabled", new TableInfo.Column("nagEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("nagHour", new TableInfo.Column("nagHour", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("nagMinute", new TableInfo.Column("nagMinute", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("archived", new TableInfo.Column("archived", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHabits = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHabits = new HashSet<TableInfo.Index>(1);
        _indicesHabits.add(new TableInfo.Index("index_habits_sortOrder", false, Arrays.asList("sortOrder"), Arrays.asList("ASC")));
        final TableInfo _infoHabits = new TableInfo("habits", _columnsHabits, _foreignKeysHabits, _indicesHabits);
        final TableInfo _existingHabits = TableInfo.read(db, "habits");
        if (!_infoHabits.equals(_existingHabits)) {
          return new RoomOpenHelper.ValidationResult(false, "habits(com.tasky.app.data.db.HabitEntity).\n"
                  + " Expected:\n" + _infoHabits + "\n"
                  + " Found:\n" + _existingHabits);
        }
        final HashMap<String, TableInfo.Column> _columnsHabitChecks = new HashMap<String, TableInfo.Column>(5);
        _columnsHabitChecks.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabitChecks.put("habitId", new TableInfo.Column("habitId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabitChecks.put("epochDay", new TableInfo.Column("epochDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabitChecks.put("status", new TableInfo.Column("status", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabitChecks.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHabitChecks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHabitChecks = new HashSet<TableInfo.Index>(3);
        _indicesHabitChecks.add(new TableInfo.Index("index_habit_checks_habitId", false, Arrays.asList("habitId"), Arrays.asList("ASC")));
        _indicesHabitChecks.add(new TableInfo.Index("index_habit_checks_epochDay", false, Arrays.asList("epochDay"), Arrays.asList("ASC")));
        _indicesHabitChecks.add(new TableInfo.Index("index_habit_checks_habitId_epochDay", true, Arrays.asList("habitId", "epochDay"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoHabitChecks = new TableInfo("habit_checks", _columnsHabitChecks, _foreignKeysHabitChecks, _indicesHabitChecks);
        final TableInfo _existingHabitChecks = TableInfo.read(db, "habit_checks");
        if (!_infoHabitChecks.equals(_existingHabitChecks)) {
          return new RoomOpenHelper.ValidationResult(false, "habit_checks(com.tasky.app.data.db.HabitCheckEntity).\n"
                  + " Expected:\n" + _infoHabitChecks + "\n"
                  + " Found:\n" + _existingHabitChecks);
        }
        final HashMap<String, TableInfo.Column> _columnsFocusSessions = new HashMap<String, TableInfo.Column>(7);
        _columnsFocusSessions.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFocusSessions.put("taskId", new TableInfo.Column("taskId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFocusSessions.put("type", new TableInfo.Column("type", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFocusSessions.put("startedAt", new TableInfo.Column("startedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFocusSessions.put("endedAt", new TableInfo.Column("endedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFocusSessions.put("seconds", new TableInfo.Column("seconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFocusSessions.put("countedTaskSeconds", new TableInfo.Column("countedTaskSeconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFocusSessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFocusSessions = new HashSet<TableInfo.Index>(2);
        _indicesFocusSessions.add(new TableInfo.Index("index_focus_sessions_taskId", false, Arrays.asList("taskId"), Arrays.asList("ASC")));
        _indicesFocusSessions.add(new TableInfo.Index("index_focus_sessions_startedAt", false, Arrays.asList("startedAt"), Arrays.asList("ASC")));
        final TableInfo _infoFocusSessions = new TableInfo("focus_sessions", _columnsFocusSessions, _foreignKeysFocusSessions, _indicesFocusSessions);
        final TableInfo _existingFocusSessions = TableInfo.read(db, "focus_sessions");
        if (!_infoFocusSessions.equals(_existingFocusSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "focus_sessions(com.tasky.app.data.db.FocusSessionEntity).\n"
                  + " Expected:\n" + _infoFocusSessions + "\n"
                  + " Found:\n" + _existingFocusSessions);
        }
        final HashMap<String, TableInfo.Column> _columnsCalendarEvents = new HashMap<String, TableInfo.Column>(12);
        _columnsCalendarEvents.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("location", new TableInfo.Column("location", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("start", new TableInfo.Column("start", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("end", new TableInfo.Column("end", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("allDay", new TableInfo.Column("allDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("source", new TableInfo.Column("source", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("deviceId", new TableInfo.Column("deviceId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("deviceCalendarId", new TableInfo.Column("deviceCalendarId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("feedId", new TableInfo.Column("feedId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalendarEvents.put("linkedTaskId", new TableInfo.Column("linkedTaskId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCalendarEvents = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCalendarEvents = new HashSet<TableInfo.Index>(5);
        _indicesCalendarEvents.add(new TableInfo.Index("index_calendar_events_start", false, Arrays.asList("start"), Arrays.asList("ASC")));
        _indicesCalendarEvents.add(new TableInfo.Index("index_calendar_events_end", false, Arrays.asList("end"), Arrays.asList("ASC")));
        _indicesCalendarEvents.add(new TableInfo.Index("index_calendar_events_source", false, Arrays.asList("source"), Arrays.asList("ASC")));
        _indicesCalendarEvents.add(new TableInfo.Index("index_calendar_events_deviceId", false, Arrays.asList("deviceId"), Arrays.asList("ASC")));
        _indicesCalendarEvents.add(new TableInfo.Index("index_calendar_events_feedId", false, Arrays.asList("feedId"), Arrays.asList("ASC")));
        final TableInfo _infoCalendarEvents = new TableInfo("calendar_events", _columnsCalendarEvents, _foreignKeysCalendarEvents, _indicesCalendarEvents);
        final TableInfo _existingCalendarEvents = TableInfo.read(db, "calendar_events");
        if (!_infoCalendarEvents.equals(_existingCalendarEvents)) {
          return new RoomOpenHelper.ValidationResult(false, "calendar_events(com.tasky.app.data.db.CalendarEventEntity).\n"
                  + " Expected:\n" + _infoCalendarEvents + "\n"
                  + " Found:\n" + _existingCalendarEvents);
        }
        final HashMap<String, TableInfo.Column> _columnsIcsFeeds = new HashMap<String, TableInfo.Column>(6);
        _columnsIcsFeeds.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIcsFeeds.put("url", new TableInfo.Column("url", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIcsFeeds.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIcsFeeds.put("color", new TableInfo.Column("color", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIcsFeeds.put("enabled", new TableInfo.Column("enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIcsFeeds.put("lastSync", new TableInfo.Column("lastSync", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysIcsFeeds = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesIcsFeeds = new HashSet<TableInfo.Index>(1);
        _indicesIcsFeeds.add(new TableInfo.Index("index_ics_feeds_url", true, Arrays.asList("url"), Arrays.asList("ASC")));
        final TableInfo _infoIcsFeeds = new TableInfo("ics_feeds", _columnsIcsFeeds, _foreignKeysIcsFeeds, _indicesIcsFeeds);
        final TableInfo _existingIcsFeeds = TableInfo.read(db, "ics_feeds");
        if (!_infoIcsFeeds.equals(_existingIcsFeeds)) {
          return new RoomOpenHelper.ValidationResult(false, "ics_feeds(com.tasky.app.data.db.IcsFeedEntity).\n"
                  + " Expected:\n" + _infoIcsFeeds + "\n"
                  + " Found:\n" + _existingIcsFeeds);
        }
        final HashMap<String, TableInfo.Column> _columnsAttachments = new HashMap<String, TableInfo.Column>(5);
        _columnsAttachments.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttachments.put("taskId", new TableInfo.Column("taskId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttachments.put("fileName", new TableInfo.Column("fileName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttachments.put("mimeType", new TableInfo.Column("mimeType", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttachments.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAttachments = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAttachments = new HashSet<TableInfo.Index>(1);
        _indicesAttachments.add(new TableInfo.Index("index_attachments_taskId", false, Arrays.asList("taskId"), Arrays.asList("ASC")));
        final TableInfo _infoAttachments = new TableInfo("attachments", _columnsAttachments, _foreignKeysAttachments, _indicesAttachments);
        final TableInfo _existingAttachments = TableInfo.read(db, "attachments");
        if (!_infoAttachments.equals(_existingAttachments)) {
          return new RoomOpenHelper.ValidationResult(false, "attachments(com.tasky.app.data.db.AttachmentEntity).\n"
                  + " Expected:\n" + _infoAttachments + "\n"
                  + " Found:\n" + _existingAttachments);
        }
        final HashMap<String, TableInfo.Column> _columnsKvMeta = new HashMap<String, TableInfo.Column>(2);
        _columnsKvMeta.put("key", new TableInfo.Column("key", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKvMeta.put("value", new TableInfo.Column("value", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysKvMeta = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesKvMeta = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoKvMeta = new TableInfo("kv_meta", _columnsKvMeta, _foreignKeysKvMeta, _indicesKvMeta);
        final TableInfo _existingKvMeta = TableInfo.read(db, "kv_meta");
        if (!_infoKvMeta.equals(_existingKvMeta)) {
          return new RoomOpenHelper.ValidationResult(false, "kv_meta(com.tasky.app.data.db.MetaEntity).\n"
                  + " Expected:\n" + _infoKvMeta + "\n"
                  + " Found:\n" + _existingKvMeta);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "ef7f0da1837bee4861cd79d12b282606", "9bf20eb6076d8dacacb732c717482f9b");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "tasks","reminders","lists","folders","tags","task_tags","filters","habits","habit_checks","focus_sessions","calendar_events","ics_feeds","attachments","kv_meta");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `tasks`");
      _db.execSQL("DELETE FROM `reminders`");
      _db.execSQL("DELETE FROM `lists`");
      _db.execSQL("DELETE FROM `folders`");
      _db.execSQL("DELETE FROM `tags`");
      _db.execSQL("DELETE FROM `task_tags`");
      _db.execSQL("DELETE FROM `filters`");
      _db.execSQL("DELETE FROM `habits`");
      _db.execSQL("DELETE FROM `habit_checks`");
      _db.execSQL("DELETE FROM `focus_sessions`");
      _db.execSQL("DELETE FROM `calendar_events`");
      _db.execSQL("DELETE FROM `ics_feeds`");
      _db.execSQL("DELETE FROM `attachments`");
      _db.execSQL("DELETE FROM `kv_meta`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(TaskDao.class, TaskDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ReminderDao.class, ReminderDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ListDao.class, ListDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FolderDao.class, FolderDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TagDao.class, TagDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FilterDao.class, FilterDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HabitDao.class, HabitDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HabitCheckDao.class, HabitCheckDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FocusSessionDao.class, FocusSessionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CalendarEventDao.class, CalendarEventDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(IcsFeedDao.class, IcsFeedDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AttachmentDao.class, AttachmentDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MetaDao.class, MetaDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public TaskDao taskDao() {
    if (_taskDao != null) {
      return _taskDao;
    } else {
      synchronized(this) {
        if(_taskDao == null) {
          _taskDao = new TaskDao_Impl(this);
        }
        return _taskDao;
      }
    }
  }

  @Override
  public ReminderDao reminderDao() {
    if (_reminderDao != null) {
      return _reminderDao;
    } else {
      synchronized(this) {
        if(_reminderDao == null) {
          _reminderDao = new ReminderDao_Impl(this);
        }
        return _reminderDao;
      }
    }
  }

  @Override
  public ListDao listDao() {
    if (_listDao != null) {
      return _listDao;
    } else {
      synchronized(this) {
        if(_listDao == null) {
          _listDao = new ListDao_Impl(this);
        }
        return _listDao;
      }
    }
  }

  @Override
  public FolderDao folderDao() {
    if (_folderDao != null) {
      return _folderDao;
    } else {
      synchronized(this) {
        if(_folderDao == null) {
          _folderDao = new FolderDao_Impl(this);
        }
        return _folderDao;
      }
    }
  }

  @Override
  public TagDao tagDao() {
    if (_tagDao != null) {
      return _tagDao;
    } else {
      synchronized(this) {
        if(_tagDao == null) {
          _tagDao = new TagDao_Impl(this);
        }
        return _tagDao;
      }
    }
  }

  @Override
  public FilterDao filterDao() {
    if (_filterDao != null) {
      return _filterDao;
    } else {
      synchronized(this) {
        if(_filterDao == null) {
          _filterDao = new FilterDao_Impl(this);
        }
        return _filterDao;
      }
    }
  }

  @Override
  public HabitDao habitDao() {
    if (_habitDao != null) {
      return _habitDao;
    } else {
      synchronized(this) {
        if(_habitDao == null) {
          _habitDao = new HabitDao_Impl(this);
        }
        return _habitDao;
      }
    }
  }

  @Override
  public HabitCheckDao habitCheckDao() {
    if (_habitCheckDao != null) {
      return _habitCheckDao;
    } else {
      synchronized(this) {
        if(_habitCheckDao == null) {
          _habitCheckDao = new HabitCheckDao_Impl(this);
        }
        return _habitCheckDao;
      }
    }
  }

  @Override
  public FocusSessionDao focusSessionDao() {
    if (_focusSessionDao != null) {
      return _focusSessionDao;
    } else {
      synchronized(this) {
        if(_focusSessionDao == null) {
          _focusSessionDao = new FocusSessionDao_Impl(this);
        }
        return _focusSessionDao;
      }
    }
  }

  @Override
  public CalendarEventDao calendarEventDao() {
    if (_calendarEventDao != null) {
      return _calendarEventDao;
    } else {
      synchronized(this) {
        if(_calendarEventDao == null) {
          _calendarEventDao = new CalendarEventDao_Impl(this);
        }
        return _calendarEventDao;
      }
    }
  }

  @Override
  public IcsFeedDao icsFeedDao() {
    if (_icsFeedDao != null) {
      return _icsFeedDao;
    } else {
      synchronized(this) {
        if(_icsFeedDao == null) {
          _icsFeedDao = new IcsFeedDao_Impl(this);
        }
        return _icsFeedDao;
      }
    }
  }

  @Override
  public AttachmentDao attachmentDao() {
    if (_attachmentDao != null) {
      return _attachmentDao;
    } else {
      synchronized(this) {
        if(_attachmentDao == null) {
          _attachmentDao = new AttachmentDao_Impl(this);
        }
        return _attachmentDao;
      }
    }
  }

  @Override
  public MetaDao metaDao() {
    if (_metaDao != null) {
      return _metaDao;
    } else {
      synchronized(this) {
        if(_metaDao == null) {
          _metaDao = new MetaDao_Impl(this);
        }
        return _metaDao;
      }
    }
  }
}
