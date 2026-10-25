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
public final class FocusSessionDao_Impl implements FocusSessionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FocusSessionEntity> __insertionAdapterOfFocusSessionEntity;

  private final EntityUpsertionAdapter<FocusSessionEntity> __upsertionAdapterOfFocusSessionEntity;

  public FocusSessionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFocusSessionEntity = new EntityInsertionAdapter<FocusSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `focus_sessions` (`id`,`taskId`,`type`,`startedAt`,`endedAt`,`seconds`,`countedTaskSeconds`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FocusSessionEntity entity) {
        statement.bindString(1, entity.getId());
        if (entity.getTaskId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getTaskId());
        }
        statement.bindLong(3, entity.getType());
        statement.bindLong(4, entity.getStartedAt());
        statement.bindLong(5, entity.getEndedAt());
        statement.bindLong(6, entity.getSeconds());
        final int _tmp = entity.getCountedTaskSeconds() ? 1 : 0;
        statement.bindLong(7, _tmp);
      }
    };
    this.__upsertionAdapterOfFocusSessionEntity = new EntityUpsertionAdapter<FocusSessionEntity>(new EntityInsertionAdapter<FocusSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `focus_sessions` (`id`,`taskId`,`type`,`startedAt`,`endedAt`,`seconds`,`countedTaskSeconds`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FocusSessionEntity entity) {
        statement.bindString(1, entity.getId());
        if (entity.getTaskId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getTaskId());
        }
        statement.bindLong(3, entity.getType());
        statement.bindLong(4, entity.getStartedAt());
        statement.bindLong(5, entity.getEndedAt());
        statement.bindLong(6, entity.getSeconds());
        final int _tmp = entity.getCountedTaskSeconds() ? 1 : 0;
        statement.bindLong(7, _tmp);
      }
    }, new EntityDeletionOrUpdateAdapter<FocusSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `focus_sessions` SET `id` = ?,`taskId` = ?,`type` = ?,`startedAt` = ?,`endedAt` = ?,`seconds` = ?,`countedTaskSeconds` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FocusSessionEntity entity) {
        statement.bindString(1, entity.getId());
        if (entity.getTaskId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getTaskId());
        }
        statement.bindLong(3, entity.getType());
        statement.bindLong(4, entity.getStartedAt());
        statement.bindLong(5, entity.getEndedAt());
        statement.bindLong(6, entity.getSeconds());
        final int _tmp = entity.getCountedTaskSeconds() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindString(8, entity.getId());
      }
    });
  }

  @Override
  public Object insert(final FocusSessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfFocusSessionEntity.insert(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<FocusSessionEntity> sessions,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfFocusSessionEntity.insert(sessions);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsert(final FocusSessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfFocusSessionEntity.upsert(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object since(final long from,
      final Continuation<? super List<FocusSessionEntity>> $completion) {
    final String _sql = "SELECT * FROM focus_sessions WHERE startedAt >= ? ORDER BY startedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, from);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<FocusSessionEntity>>() {
      @Override
      @NonNull
      public List<FocusSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfEndedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAt");
          final int _cursorIndexOfSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "seconds");
          final int _cursorIndexOfCountedTaskSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "countedTaskSeconds");
          final List<FocusSessionEntity> _result = new ArrayList<FocusSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FocusSessionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTaskId;
            if (_cursor.isNull(_cursorIndexOfTaskId)) {
              _tmpTaskId = null;
            } else {
              _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            }
            final int _tmpType;
            _tmpType = _cursor.getInt(_cursorIndexOfType);
            final long _tmpStartedAt;
            _tmpStartedAt = _cursor.getLong(_cursorIndexOfStartedAt);
            final long _tmpEndedAt;
            _tmpEndedAt = _cursor.getLong(_cursorIndexOfEndedAt);
            final long _tmpSeconds;
            _tmpSeconds = _cursor.getLong(_cursorIndexOfSeconds);
            final boolean _tmpCountedTaskSeconds;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCountedTaskSeconds);
            _tmpCountedTaskSeconds = _tmp != 0;
            _item = new FocusSessionEntity(_tmpId,_tmpTaskId,_tmpType,_tmpStartedAt,_tmpEndedAt,_tmpSeconds,_tmpCountedTaskSeconds);
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
  public Flow<List<FocusSessionEntity>> observeSince(final long from) {
    final String _sql = "SELECT * FROM focus_sessions WHERE startedAt >= ? ORDER BY startedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, from);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"focus_sessions"}, new Callable<List<FocusSessionEntity>>() {
      @Override
      @NonNull
      public List<FocusSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfEndedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAt");
          final int _cursorIndexOfSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "seconds");
          final int _cursorIndexOfCountedTaskSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "countedTaskSeconds");
          final List<FocusSessionEntity> _result = new ArrayList<FocusSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FocusSessionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTaskId;
            if (_cursor.isNull(_cursorIndexOfTaskId)) {
              _tmpTaskId = null;
            } else {
              _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            }
            final int _tmpType;
            _tmpType = _cursor.getInt(_cursorIndexOfType);
            final long _tmpStartedAt;
            _tmpStartedAt = _cursor.getLong(_cursorIndexOfStartedAt);
            final long _tmpEndedAt;
            _tmpEndedAt = _cursor.getLong(_cursorIndexOfEndedAt);
            final long _tmpSeconds;
            _tmpSeconds = _cursor.getLong(_cursorIndexOfSeconds);
            final boolean _tmpCountedTaskSeconds;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCountedTaskSeconds);
            _tmpCountedTaskSeconds = _tmp != 0;
            _item = new FocusSessionEntity(_tmpId,_tmpTaskId,_tmpType,_tmpStartedAt,_tmpEndedAt,_tmpSeconds,_tmpCountedTaskSeconds);
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
  public Object forTask(final String taskId,
      final Continuation<? super List<FocusSessionEntity>> $completion) {
    final String _sql = "SELECT * FROM focus_sessions WHERE taskId = ? ORDER BY startedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, taskId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<FocusSessionEntity>>() {
      @Override
      @NonNull
      public List<FocusSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfEndedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAt");
          final int _cursorIndexOfSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "seconds");
          final int _cursorIndexOfCountedTaskSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "countedTaskSeconds");
          final List<FocusSessionEntity> _result = new ArrayList<FocusSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FocusSessionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTaskId;
            if (_cursor.isNull(_cursorIndexOfTaskId)) {
              _tmpTaskId = null;
            } else {
              _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            }
            final int _tmpType;
            _tmpType = _cursor.getInt(_cursorIndexOfType);
            final long _tmpStartedAt;
            _tmpStartedAt = _cursor.getLong(_cursorIndexOfStartedAt);
            final long _tmpEndedAt;
            _tmpEndedAt = _cursor.getLong(_cursorIndexOfEndedAt);
            final long _tmpSeconds;
            _tmpSeconds = _cursor.getLong(_cursorIndexOfSeconds);
            final boolean _tmpCountedTaskSeconds;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCountedTaskSeconds);
            _tmpCountedTaskSeconds = _tmp != 0;
            _item = new FocusSessionEntity(_tmpId,_tmpTaskId,_tmpType,_tmpStartedAt,_tmpEndedAt,_tmpSeconds,_tmpCountedTaskSeconds);
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
  public Flow<List<FocusSessionEntity>> observeAll() {
    final String _sql = "SELECT * FROM focus_sessions ORDER BY startedAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"focus_sessions"}, new Callable<List<FocusSessionEntity>>() {
      @Override
      @NonNull
      public List<FocusSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfEndedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAt");
          final int _cursorIndexOfSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "seconds");
          final int _cursorIndexOfCountedTaskSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "countedTaskSeconds");
          final List<FocusSessionEntity> _result = new ArrayList<FocusSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FocusSessionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTaskId;
            if (_cursor.isNull(_cursorIndexOfTaskId)) {
              _tmpTaskId = null;
            } else {
              _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            }
            final int _tmpType;
            _tmpType = _cursor.getInt(_cursorIndexOfType);
            final long _tmpStartedAt;
            _tmpStartedAt = _cursor.getLong(_cursorIndexOfStartedAt);
            final long _tmpEndedAt;
            _tmpEndedAt = _cursor.getLong(_cursorIndexOfEndedAt);
            final long _tmpSeconds;
            _tmpSeconds = _cursor.getLong(_cursorIndexOfSeconds);
            final boolean _tmpCountedTaskSeconds;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCountedTaskSeconds);
            _tmpCountedTaskSeconds = _tmp != 0;
            _item = new FocusSessionEntity(_tmpId,_tmpTaskId,_tmpType,_tmpStartedAt,_tmpEndedAt,_tmpSeconds,_tmpCountedTaskSeconds);
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
  public Object all(final Continuation<? super List<FocusSessionEntity>> $completion) {
    final String _sql = "SELECT * FROM focus_sessions ORDER BY startedAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<FocusSessionEntity>>() {
      @Override
      @NonNull
      public List<FocusSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfEndedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAt");
          final int _cursorIndexOfSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "seconds");
          final int _cursorIndexOfCountedTaskSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "countedTaskSeconds");
          final List<FocusSessionEntity> _result = new ArrayList<FocusSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FocusSessionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTaskId;
            if (_cursor.isNull(_cursorIndexOfTaskId)) {
              _tmpTaskId = null;
            } else {
              _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            }
            final int _tmpType;
            _tmpType = _cursor.getInt(_cursorIndexOfType);
            final long _tmpStartedAt;
            _tmpStartedAt = _cursor.getLong(_cursorIndexOfStartedAt);
            final long _tmpEndedAt;
            _tmpEndedAt = _cursor.getLong(_cursorIndexOfEndedAt);
            final long _tmpSeconds;
            _tmpSeconds = _cursor.getLong(_cursorIndexOfSeconds);
            final boolean _tmpCountedTaskSeconds;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCountedTaskSeconds);
            _tmpCountedTaskSeconds = _tmp != 0;
            _item = new FocusSessionEntity(_tmpId,_tmpTaskId,_tmpType,_tmpStartedAt,_tmpEndedAt,_tmpSeconds,_tmpCountedTaskSeconds);
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
  public Object focusedSecondsSince(final long from, final Continuation<? super Long> $completion) {
    final String _sql = "SELECT COALESCE(SUM(seconds), 0) FROM focus_sessions WHERE startedAt >= ? AND type = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, from);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final long _tmp;
            _tmp = _cursor.getLong(0);
            _result = _tmp;
          } else {
            _result = 0L;
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
  public Flow<Long> observeFocusedSecondsSince(final long from) {
    final String _sql = "SELECT COALESCE(SUM(seconds), 0) FROM focus_sessions WHERE startedAt >= ? AND type = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, from);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"focus_sessions"}, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final long _tmp;
            _tmp = _cursor.getLong(0);
            _result = _tmp;
          } else {
            _result = 0L;
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
  public Flow<Long> observeSecondsForTask(final String taskId) {
    final String _sql = "SELECT COALESCE(SUM(seconds), 0) FROM focus_sessions WHERE taskId = ? AND type = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, taskId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"focus_sessions"}, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final long _tmp;
            _tmp = _cursor.getLong(0);
            _result = _tmp;
          } else {
            _result = 0L;
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
  public Object byId(final String id, final Continuation<? super FocusSessionEntity> $completion) {
    final String _sql = "SELECT * FROM focus_sessions WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<FocusSessionEntity>() {
      @Override
      @Nullable
      public FocusSessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskId = CursorUtil.getColumnIndexOrThrow(_cursor, "taskId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfEndedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAt");
          final int _cursorIndexOfSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "seconds");
          final int _cursorIndexOfCountedTaskSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "countedTaskSeconds");
          final FocusSessionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTaskId;
            if (_cursor.isNull(_cursorIndexOfTaskId)) {
              _tmpTaskId = null;
            } else {
              _tmpTaskId = _cursor.getString(_cursorIndexOfTaskId);
            }
            final int _tmpType;
            _tmpType = _cursor.getInt(_cursorIndexOfType);
            final long _tmpStartedAt;
            _tmpStartedAt = _cursor.getLong(_cursorIndexOfStartedAt);
            final long _tmpEndedAt;
            _tmpEndedAt = _cursor.getLong(_cursorIndexOfEndedAt);
            final long _tmpSeconds;
            _tmpSeconds = _cursor.getLong(_cursorIndexOfSeconds);
            final boolean _tmpCountedTaskSeconds;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCountedTaskSeconds);
            _tmpCountedTaskSeconds = _tmp != 0;
            _result = new FocusSessionEntity(_tmpId,_tmpTaskId,_tmpType,_tmpStartedAt,_tmpEndedAt,_tmpSeconds,_tmpCountedTaskSeconds);
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
