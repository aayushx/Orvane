package com.tasky.app.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CalendarEventDao_Impl implements CalendarEventDao {
  private final RoomDatabase __db;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllDevice;

  private final SharedSQLiteStatement __preparedStmtOfDeleteForFeed;

  private final SharedSQLiteStatement __preparedStmtOfDeleteLinkedToTask;

  private final EntityUpsertionAdapter<CalendarEventEntity> __upsertionAdapterOfCalendarEventEntity;

  public CalendarEventDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM calendar_events WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllDevice = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM calendar_events WHERE source = 1";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteForFeed = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM calendar_events WHERE feedId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteLinkedToTask = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM calendar_events WHERE linkedTaskId = ?";
        return _query;
      }
    };
    this.__upsertionAdapterOfCalendarEventEntity = new EntityUpsertionAdapter<CalendarEventEntity>(new EntityInsertionAdapter<CalendarEventEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `calendar_events` (`id`,`title`,`note`,`location`,`start`,`end`,`allDay`,`source`,`deviceId`,`deviceCalendarId`,`feedId`,`linkedTaskId`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CalendarEventEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getNote());
        if (entity.getLocation() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getLocation());
        }
        statement.bindLong(5, entity.getStart());
        statement.bindLong(6, entity.getEnd());
        final int _tmp = entity.getAllDay() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getSource());
        if (entity.getDeviceId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getDeviceId());
        }
        if (entity.getDeviceCalendarId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getDeviceCalendarId());
        }
        if (entity.getFeedId() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getFeedId());
        }
        if (entity.getLinkedTaskId() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getLinkedTaskId());
        }
      }
    }, new EntityDeletionOrUpdateAdapter<CalendarEventEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `calendar_events` SET `id` = ?,`title` = ?,`note` = ?,`location` = ?,`start` = ?,`end` = ?,`allDay` = ?,`source` = ?,`deviceId` = ?,`deviceCalendarId` = ?,`feedId` = ?,`linkedTaskId` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CalendarEventEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getNote());
        if (entity.getLocation() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getLocation());
        }
        statement.bindLong(5, entity.getStart());
        statement.bindLong(6, entity.getEnd());
        final int _tmp = entity.getAllDay() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getSource());
        if (entity.getDeviceId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getDeviceId());
        }
        if (entity.getDeviceCalendarId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getDeviceCalendarId());
        }
        if (entity.getFeedId() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getFeedId());
        }
        if (entity.getLinkedTaskId() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getLinkedTaskId());
        }
        statement.bindString(13, entity.getId());
      }
    });
  }

  @Override
  public Object delete(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllDevice(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllDevice.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllDevice.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteForFeed(final String feedId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteForFeed.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, feedId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteForFeed.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteLinkedToTask(final String taskId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteLinkedToTask.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, taskId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteLinkedToTask.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object upsert(final CalendarEventEntity event,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfCalendarEventEntity.upsert(event);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<CalendarEventEntity> events,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfCalendarEventEntity.upsert(events);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CalendarEventEntity>> observeInRange(final long rangeStart,
      final long rangeEnd) {
    final String _sql = "SELECT * FROM calendar_events WHERE start < ? AND end > ? ORDER BY start ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, rangeEnd);
    _argIndex = 2;
    _statement.bindLong(_argIndex, rangeStart);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"calendar_events"}, new Callable<List<CalendarEventEntity>>() {
      @Override
      @NonNull
      public List<CalendarEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfStart = CursorUtil.getColumnIndexOrThrow(_cursor, "start");
          final int _cursorIndexOfEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "end");
          final int _cursorIndexOfAllDay = CursorUtil.getColumnIndexOrThrow(_cursor, "allDay");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfDeviceCalendarId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceCalendarId");
          final int _cursorIndexOfFeedId = CursorUtil.getColumnIndexOrThrow(_cursor, "feedId");
          final int _cursorIndexOfLinkedTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedTaskId");
          final List<CalendarEventEntity> _result = new ArrayList<CalendarEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CalendarEventEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpLocation;
            if (_cursor.isNull(_cursorIndexOfLocation)) {
              _tmpLocation = null;
            } else {
              _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            }
            final long _tmpStart;
            _tmpStart = _cursor.getLong(_cursorIndexOfStart);
            final long _tmpEnd;
            _tmpEnd = _cursor.getLong(_cursorIndexOfEnd);
            final boolean _tmpAllDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfAllDay);
            _tmpAllDay = _tmp != 0;
            final int _tmpSource;
            _tmpSource = _cursor.getInt(_cursorIndexOfSource);
            final Long _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getLong(_cursorIndexOfDeviceId);
            }
            final Long _tmpDeviceCalendarId;
            if (_cursor.isNull(_cursorIndexOfDeviceCalendarId)) {
              _tmpDeviceCalendarId = null;
            } else {
              _tmpDeviceCalendarId = _cursor.getLong(_cursorIndexOfDeviceCalendarId);
            }
            final String _tmpFeedId;
            if (_cursor.isNull(_cursorIndexOfFeedId)) {
              _tmpFeedId = null;
            } else {
              _tmpFeedId = _cursor.getString(_cursorIndexOfFeedId);
            }
            final String _tmpLinkedTaskId;
            if (_cursor.isNull(_cursorIndexOfLinkedTaskId)) {
              _tmpLinkedTaskId = null;
            } else {
              _tmpLinkedTaskId = _cursor.getString(_cursorIndexOfLinkedTaskId);
            }
            _item = new CalendarEventEntity(_tmpId,_tmpTitle,_tmpNote,_tmpLocation,_tmpStart,_tmpEnd,_tmpAllDay,_tmpSource,_tmpDeviceId,_tmpDeviceCalendarId,_tmpFeedId,_tmpLinkedTaskId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object inRange(final long rangeStart, final long rangeEnd,
      final Continuation<? super List<CalendarEventEntity>> $completion) {
    final String _sql = "SELECT * FROM calendar_events WHERE start < ? AND end > ? ORDER BY start ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, rangeEnd);
    _argIndex = 2;
    _statement.bindLong(_argIndex, rangeStart);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CalendarEventEntity>>() {
      @Override
      @NonNull
      public List<CalendarEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfStart = CursorUtil.getColumnIndexOrThrow(_cursor, "start");
          final int _cursorIndexOfEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "end");
          final int _cursorIndexOfAllDay = CursorUtil.getColumnIndexOrThrow(_cursor, "allDay");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfDeviceCalendarId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceCalendarId");
          final int _cursorIndexOfFeedId = CursorUtil.getColumnIndexOrThrow(_cursor, "feedId");
          final int _cursorIndexOfLinkedTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedTaskId");
          final List<CalendarEventEntity> _result = new ArrayList<CalendarEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CalendarEventEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpLocation;
            if (_cursor.isNull(_cursorIndexOfLocation)) {
              _tmpLocation = null;
            } else {
              _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            }
            final long _tmpStart;
            _tmpStart = _cursor.getLong(_cursorIndexOfStart);
            final long _tmpEnd;
            _tmpEnd = _cursor.getLong(_cursorIndexOfEnd);
            final boolean _tmpAllDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfAllDay);
            _tmpAllDay = _tmp != 0;
            final int _tmpSource;
            _tmpSource = _cursor.getInt(_cursorIndexOfSource);
            final Long _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getLong(_cursorIndexOfDeviceId);
            }
            final Long _tmpDeviceCalendarId;
            if (_cursor.isNull(_cursorIndexOfDeviceCalendarId)) {
              _tmpDeviceCalendarId = null;
            } else {
              _tmpDeviceCalendarId = _cursor.getLong(_cursorIndexOfDeviceCalendarId);
            }
            final String _tmpFeedId;
            if (_cursor.isNull(_cursorIndexOfFeedId)) {
              _tmpFeedId = null;
            } else {
              _tmpFeedId = _cursor.getString(_cursorIndexOfFeedId);
            }
            final String _tmpLinkedTaskId;
            if (_cursor.isNull(_cursorIndexOfLinkedTaskId)) {
              _tmpLinkedTaskId = null;
            } else {
              _tmpLinkedTaskId = _cursor.getString(_cursorIndexOfLinkedTaskId);
            }
            _item = new CalendarEventEntity(_tmpId,_tmpTitle,_tmpNote,_tmpLocation,_tmpStart,_tmpEnd,_tmpAllDay,_tmpSource,_tmpDeviceId,_tmpDeviceCalendarId,_tmpFeedId,_tmpLinkedTaskId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object byDeviceId(final long deviceId,
      final Continuation<? super CalendarEventEntity> $completion) {
    final String _sql = "SELECT * FROM calendar_events WHERE deviceId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, deviceId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CalendarEventEntity>() {
      @Override
      @Nullable
      public CalendarEventEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfStart = CursorUtil.getColumnIndexOrThrow(_cursor, "start");
          final int _cursorIndexOfEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "end");
          final int _cursorIndexOfAllDay = CursorUtil.getColumnIndexOrThrow(_cursor, "allDay");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfDeviceCalendarId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceCalendarId");
          final int _cursorIndexOfFeedId = CursorUtil.getColumnIndexOrThrow(_cursor, "feedId");
          final int _cursorIndexOfLinkedTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedTaskId");
          final CalendarEventEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpLocation;
            if (_cursor.isNull(_cursorIndexOfLocation)) {
              _tmpLocation = null;
            } else {
              _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            }
            final long _tmpStart;
            _tmpStart = _cursor.getLong(_cursorIndexOfStart);
            final long _tmpEnd;
            _tmpEnd = _cursor.getLong(_cursorIndexOfEnd);
            final boolean _tmpAllDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfAllDay);
            _tmpAllDay = _tmp != 0;
            final int _tmpSource;
            _tmpSource = _cursor.getInt(_cursorIndexOfSource);
            final Long _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getLong(_cursorIndexOfDeviceId);
            }
            final Long _tmpDeviceCalendarId;
            if (_cursor.isNull(_cursorIndexOfDeviceCalendarId)) {
              _tmpDeviceCalendarId = null;
            } else {
              _tmpDeviceCalendarId = _cursor.getLong(_cursorIndexOfDeviceCalendarId);
            }
            final String _tmpFeedId;
            if (_cursor.isNull(_cursorIndexOfFeedId)) {
              _tmpFeedId = null;
            } else {
              _tmpFeedId = _cursor.getString(_cursorIndexOfFeedId);
            }
            final String _tmpLinkedTaskId;
            if (_cursor.isNull(_cursorIndexOfLinkedTaskId)) {
              _tmpLinkedTaskId = null;
            } else {
              _tmpLinkedTaskId = _cursor.getString(_cursorIndexOfLinkedTaskId);
            }
            _result = new CalendarEventEntity(_tmpId,_tmpTitle,_tmpNote,_tmpLocation,_tmpStart,_tmpEnd,_tmpAllDay,_tmpSource,_tmpDeviceId,_tmpDeviceCalendarId,_tmpFeedId,_tmpLinkedTaskId);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object deviceEvents(final Continuation<? super List<CalendarEventEntity>> $completion) {
    final String _sql = "SELECT * FROM calendar_events WHERE source = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CalendarEventEntity>>() {
      @Override
      @NonNull
      public List<CalendarEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfStart = CursorUtil.getColumnIndexOrThrow(_cursor, "start");
          final int _cursorIndexOfEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "end");
          final int _cursorIndexOfAllDay = CursorUtil.getColumnIndexOrThrow(_cursor, "allDay");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfDeviceCalendarId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceCalendarId");
          final int _cursorIndexOfFeedId = CursorUtil.getColumnIndexOrThrow(_cursor, "feedId");
          final int _cursorIndexOfLinkedTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedTaskId");
          final List<CalendarEventEntity> _result = new ArrayList<CalendarEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CalendarEventEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpLocation;
            if (_cursor.isNull(_cursorIndexOfLocation)) {
              _tmpLocation = null;
            } else {
              _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            }
            final long _tmpStart;
            _tmpStart = _cursor.getLong(_cursorIndexOfStart);
            final long _tmpEnd;
            _tmpEnd = _cursor.getLong(_cursorIndexOfEnd);
            final boolean _tmpAllDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfAllDay);
            _tmpAllDay = _tmp != 0;
            final int _tmpSource;
            _tmpSource = _cursor.getInt(_cursorIndexOfSource);
            final Long _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getLong(_cursorIndexOfDeviceId);
            }
            final Long _tmpDeviceCalendarId;
            if (_cursor.isNull(_cursorIndexOfDeviceCalendarId)) {
              _tmpDeviceCalendarId = null;
            } else {
              _tmpDeviceCalendarId = _cursor.getLong(_cursorIndexOfDeviceCalendarId);
            }
            final String _tmpFeedId;
            if (_cursor.isNull(_cursorIndexOfFeedId)) {
              _tmpFeedId = null;
            } else {
              _tmpFeedId = _cursor.getString(_cursorIndexOfFeedId);
            }
            final String _tmpLinkedTaskId;
            if (_cursor.isNull(_cursorIndexOfLinkedTaskId)) {
              _tmpLinkedTaskId = null;
            } else {
              _tmpLinkedTaskId = _cursor.getString(_cursorIndexOfLinkedTaskId);
            }
            _item = new CalendarEventEntity(_tmpId,_tmpTitle,_tmpNote,_tmpLocation,_tmpStart,_tmpEnd,_tmpAllDay,_tmpSource,_tmpDeviceId,_tmpDeviceCalendarId,_tmpFeedId,_tmpLinkedTaskId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object forFeed(final String feedId,
      final Continuation<? super List<CalendarEventEntity>> $completion) {
    final String _sql = "SELECT * FROM calendar_events WHERE feedId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, feedId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CalendarEventEntity>>() {
      @Override
      @NonNull
      public List<CalendarEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfStart = CursorUtil.getColumnIndexOrThrow(_cursor, "start");
          final int _cursorIndexOfEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "end");
          final int _cursorIndexOfAllDay = CursorUtil.getColumnIndexOrThrow(_cursor, "allDay");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfDeviceCalendarId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceCalendarId");
          final int _cursorIndexOfFeedId = CursorUtil.getColumnIndexOrThrow(_cursor, "feedId");
          final int _cursorIndexOfLinkedTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedTaskId");
          final List<CalendarEventEntity> _result = new ArrayList<CalendarEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CalendarEventEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpLocation;
            if (_cursor.isNull(_cursorIndexOfLocation)) {
              _tmpLocation = null;
            } else {
              _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            }
            final long _tmpStart;
            _tmpStart = _cursor.getLong(_cursorIndexOfStart);
            final long _tmpEnd;
            _tmpEnd = _cursor.getLong(_cursorIndexOfEnd);
            final boolean _tmpAllDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfAllDay);
            _tmpAllDay = _tmp != 0;
            final int _tmpSource;
            _tmpSource = _cursor.getInt(_cursorIndexOfSource);
            final Long _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getLong(_cursorIndexOfDeviceId);
            }
            final Long _tmpDeviceCalendarId;
            if (_cursor.isNull(_cursorIndexOfDeviceCalendarId)) {
              _tmpDeviceCalendarId = null;
            } else {
              _tmpDeviceCalendarId = _cursor.getLong(_cursorIndexOfDeviceCalendarId);
            }
            final String _tmpFeedId;
            if (_cursor.isNull(_cursorIndexOfFeedId)) {
              _tmpFeedId = null;
            } else {
              _tmpFeedId = _cursor.getString(_cursorIndexOfFeedId);
            }
            final String _tmpLinkedTaskId;
            if (_cursor.isNull(_cursorIndexOfLinkedTaskId)) {
              _tmpLinkedTaskId = null;
            } else {
              _tmpLinkedTaskId = _cursor.getString(_cursorIndexOfLinkedTaskId);
            }
            _item = new CalendarEventEntity(_tmpId,_tmpTitle,_tmpNote,_tmpLocation,_tmpStart,_tmpEnd,_tmpAllDay,_tmpSource,_tmpDeviceId,_tmpDeviceCalendarId,_tmpFeedId,_tmpLinkedTaskId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object locals(final Continuation<? super List<CalendarEventEntity>> $completion) {
    final String _sql = "SELECT * FROM calendar_events WHERE source = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CalendarEventEntity>>() {
      @Override
      @NonNull
      public List<CalendarEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfStart = CursorUtil.getColumnIndexOrThrow(_cursor, "start");
          final int _cursorIndexOfEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "end");
          final int _cursorIndexOfAllDay = CursorUtil.getColumnIndexOrThrow(_cursor, "allDay");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfDeviceCalendarId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceCalendarId");
          final int _cursorIndexOfFeedId = CursorUtil.getColumnIndexOrThrow(_cursor, "feedId");
          final int _cursorIndexOfLinkedTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedTaskId");
          final List<CalendarEventEntity> _result = new ArrayList<CalendarEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CalendarEventEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpLocation;
            if (_cursor.isNull(_cursorIndexOfLocation)) {
              _tmpLocation = null;
            } else {
              _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            }
            final long _tmpStart;
            _tmpStart = _cursor.getLong(_cursorIndexOfStart);
            final long _tmpEnd;
            _tmpEnd = _cursor.getLong(_cursorIndexOfEnd);
            final boolean _tmpAllDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfAllDay);
            _tmpAllDay = _tmp != 0;
            final int _tmpSource;
            _tmpSource = _cursor.getInt(_cursorIndexOfSource);
            final Long _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getLong(_cursorIndexOfDeviceId);
            }
            final Long _tmpDeviceCalendarId;
            if (_cursor.isNull(_cursorIndexOfDeviceCalendarId)) {
              _tmpDeviceCalendarId = null;
            } else {
              _tmpDeviceCalendarId = _cursor.getLong(_cursorIndexOfDeviceCalendarId);
            }
            final String _tmpFeedId;
            if (_cursor.isNull(_cursorIndexOfFeedId)) {
              _tmpFeedId = null;
            } else {
              _tmpFeedId = _cursor.getString(_cursorIndexOfFeedId);
            }
            final String _tmpLinkedTaskId;
            if (_cursor.isNull(_cursorIndexOfLinkedTaskId)) {
              _tmpLinkedTaskId = null;
            } else {
              _tmpLinkedTaskId = _cursor.getString(_cursorIndexOfLinkedTaskId);
            }
            _item = new CalendarEventEntity(_tmpId,_tmpTitle,_tmpNote,_tmpLocation,_tmpStart,_tmpEnd,_tmpAllDay,_tmpSource,_tmpDeviceId,_tmpDeviceCalendarId,_tmpFeedId,_tmpLinkedTaskId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object byId(final String id, final Continuation<? super CalendarEventEntity> $completion) {
    final String _sql = "SELECT * FROM calendar_events WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CalendarEventEntity>() {
      @Override
      @Nullable
      public CalendarEventEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfStart = CursorUtil.getColumnIndexOrThrow(_cursor, "start");
          final int _cursorIndexOfEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "end");
          final int _cursorIndexOfAllDay = CursorUtil.getColumnIndexOrThrow(_cursor, "allDay");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfDeviceCalendarId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceCalendarId");
          final int _cursorIndexOfFeedId = CursorUtil.getColumnIndexOrThrow(_cursor, "feedId");
          final int _cursorIndexOfLinkedTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedTaskId");
          final CalendarEventEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpLocation;
            if (_cursor.isNull(_cursorIndexOfLocation)) {
              _tmpLocation = null;
            } else {
              _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            }
            final long _tmpStart;
            _tmpStart = _cursor.getLong(_cursorIndexOfStart);
            final long _tmpEnd;
            _tmpEnd = _cursor.getLong(_cursorIndexOfEnd);
            final boolean _tmpAllDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfAllDay);
            _tmpAllDay = _tmp != 0;
            final int _tmpSource;
            _tmpSource = _cursor.getInt(_cursorIndexOfSource);
            final Long _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getLong(_cursorIndexOfDeviceId);
            }
            final Long _tmpDeviceCalendarId;
            if (_cursor.isNull(_cursorIndexOfDeviceCalendarId)) {
              _tmpDeviceCalendarId = null;
            } else {
              _tmpDeviceCalendarId = _cursor.getLong(_cursorIndexOfDeviceCalendarId);
            }
            final String _tmpFeedId;
            if (_cursor.isNull(_cursorIndexOfFeedId)) {
              _tmpFeedId = null;
            } else {
              _tmpFeedId = _cursor.getString(_cursorIndexOfFeedId);
            }
            final String _tmpLinkedTaskId;
            if (_cursor.isNull(_cursorIndexOfLinkedTaskId)) {
              _tmpLinkedTaskId = null;
            } else {
              _tmpLinkedTaskId = _cursor.getString(_cursorIndexOfLinkedTaskId);
            }
            _result = new CalendarEventEntity(_tmpId,_tmpTitle,_tmpNote,_tmpLocation,_tmpStart,_tmpEnd,_tmpAllDay,_tmpSource,_tmpDeviceId,_tmpDeviceCalendarId,_tmpFeedId,_tmpLinkedTaskId);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
