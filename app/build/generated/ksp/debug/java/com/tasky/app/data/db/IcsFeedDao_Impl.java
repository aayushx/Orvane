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
public final class IcsFeedDao_Impl implements IcsFeedDao {
  private final RoomDatabase __db;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  private final EntityUpsertionAdapter<IcsFeedEntity> __upsertionAdapterOfIcsFeedEntity;

  public IcsFeedDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM ics_feeds WHERE id = ?";
        return _query;
      }
    };
    this.__upsertionAdapterOfIcsFeedEntity = new EntityUpsertionAdapter<IcsFeedEntity>(new EntityInsertionAdapter<IcsFeedEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `ics_feeds` (`id`,`url`,`name`,`color`,`enabled`,`lastSync`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final IcsFeedEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUrl());
        statement.bindString(3, entity.getName());
        statement.bindLong(4, entity.getColor());
        final int _tmp = entity.getEnabled() ? 1 : 0;
        statement.bindLong(5, _tmp);
        if (entity.getLastSync() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getLastSync());
        }
      }
    }, new EntityDeletionOrUpdateAdapter<IcsFeedEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `ics_feeds` SET `id` = ?,`url` = ?,`name` = ?,`color` = ?,`enabled` = ?,`lastSync` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final IcsFeedEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUrl());
        statement.bindString(3, entity.getName());
        statement.bindLong(4, entity.getColor());
        final int _tmp = entity.getEnabled() ? 1 : 0;
        statement.bindLong(5, _tmp);
        if (entity.getLastSync() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getLastSync());
        }
        statement.bindString(7, entity.getId());
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
  public Object upsert(final IcsFeedEntity feed, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfIcsFeedEntity.upsert(feed);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<IcsFeedEntity>> observeAll() {
    final String _sql = "SELECT * FROM ics_feeds ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"ics_feeds"}, new Callable<List<IcsFeedEntity>>() {
      @Override
      @NonNull
      public List<IcsFeedEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfLastSync = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSync");
          final List<IcsFeedEntity> _result = new ArrayList<IcsFeedEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final IcsFeedEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final boolean _tmpEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp != 0;
            final Long _tmpLastSync;
            if (_cursor.isNull(_cursorIndexOfLastSync)) {
              _tmpLastSync = null;
            } else {
              _tmpLastSync = _cursor.getLong(_cursorIndexOfLastSync);
            }
            _item = new IcsFeedEntity(_tmpId,_tmpUrl,_tmpName,_tmpColor,_tmpEnabled,_tmpLastSync);
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
  public Object all(final Continuation<? super List<IcsFeedEntity>> $completion) {
    final String _sql = "SELECT * FROM ics_feeds";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<IcsFeedEntity>>() {
      @Override
      @NonNull
      public List<IcsFeedEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfLastSync = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSync");
          final List<IcsFeedEntity> _result = new ArrayList<IcsFeedEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final IcsFeedEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final boolean _tmpEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp != 0;
            final Long _tmpLastSync;
            if (_cursor.isNull(_cursorIndexOfLastSync)) {
              _tmpLastSync = null;
            } else {
              _tmpLastSync = _cursor.getLong(_cursorIndexOfLastSync);
            }
            _item = new IcsFeedEntity(_tmpId,_tmpUrl,_tmpName,_tmpColor,_tmpEnabled,_tmpLastSync);
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
  public Object byId(final String id, final Continuation<? super IcsFeedEntity> $completion) {
    final String _sql = "SELECT * FROM ics_feeds WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<IcsFeedEntity>() {
      @Override
      @Nullable
      public IcsFeedEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfLastSync = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSync");
          final IcsFeedEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final boolean _tmpEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp != 0;
            final Long _tmpLastSync;
            if (_cursor.isNull(_cursorIndexOfLastSync)) {
              _tmpLastSync = null;
            } else {
              _tmpLastSync = _cursor.getLong(_cursorIndexOfLastSync);
            }
            _result = new IcsFeedEntity(_tmpId,_tmpUrl,_tmpName,_tmpColor,_tmpEnabled,_tmpLastSync);
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
