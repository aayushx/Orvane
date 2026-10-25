package com.tasky.app.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class FilterDao_Impl implements FilterDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FilterEntity> __insertionAdapterOfFilterEntity;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  public FilterDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFilterEntity = new EntityInsertionAdapter<FilterEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `filters` (`id`,`name`,`listId`,`tagId`,`minPriority`,`dateType`,`dueStart`,`dueEnd`,`sortOrder`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FilterEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getListId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getListId());
        }
        if (entity.getTagId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getTagId());
        }
        statement.bindLong(5, entity.getMinPriority());
        statement.bindLong(6, entity.getDateType());
        if (entity.getDueStart() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getDueStart());
        }
        if (entity.getDueEnd() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getDueEnd());
        }
        statement.bindLong(9, entity.getSortOrder());
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM filters WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final FilterEntity filter, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfFilterEntity.insert(filter);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
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
  public Flow<List<FilterEntity>> observeAll() {
    final String _sql = "SELECT * FROM filters ORDER BY sortOrder ASC, name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"filters"}, new Callable<List<FilterEntity>>() {
      @Override
      @NonNull
      public List<FilterEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfListId = CursorUtil.getColumnIndexOrThrow(_cursor, "listId");
          final int _cursorIndexOfTagId = CursorUtil.getColumnIndexOrThrow(_cursor, "tagId");
          final int _cursorIndexOfMinPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "minPriority");
          final int _cursorIndexOfDateType = CursorUtil.getColumnIndexOrThrow(_cursor, "dateType");
          final int _cursorIndexOfDueStart = CursorUtil.getColumnIndexOrThrow(_cursor, "dueStart");
          final int _cursorIndexOfDueEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "dueEnd");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final List<FilterEntity> _result = new ArrayList<FilterEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FilterEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpListId;
            if (_cursor.isNull(_cursorIndexOfListId)) {
              _tmpListId = null;
            } else {
              _tmpListId = _cursor.getString(_cursorIndexOfListId);
            }
            final String _tmpTagId;
            if (_cursor.isNull(_cursorIndexOfTagId)) {
              _tmpTagId = null;
            } else {
              _tmpTagId = _cursor.getString(_cursorIndexOfTagId);
            }
            final int _tmpMinPriority;
            _tmpMinPriority = _cursor.getInt(_cursorIndexOfMinPriority);
            final int _tmpDateType;
            _tmpDateType = _cursor.getInt(_cursorIndexOfDateType);
            final Long _tmpDueStart;
            if (_cursor.isNull(_cursorIndexOfDueStart)) {
              _tmpDueStart = null;
            } else {
              _tmpDueStart = _cursor.getLong(_cursorIndexOfDueStart);
            }
            final Long _tmpDueEnd;
            if (_cursor.isNull(_cursorIndexOfDueEnd)) {
              _tmpDueEnd = null;
            } else {
              _tmpDueEnd = _cursor.getLong(_cursorIndexOfDueEnd);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _item = new FilterEntity(_tmpId,_tmpName,_tmpListId,_tmpTagId,_tmpMinPriority,_tmpDateType,_tmpDueStart,_tmpDueEnd,_tmpSortOrder);
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
  public Object all(final Continuation<? super List<FilterEntity>> $completion) {
    final String _sql = "SELECT * FROM filters ORDER BY sortOrder ASC, name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<FilterEntity>>() {
      @Override
      @NonNull
      public List<FilterEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfListId = CursorUtil.getColumnIndexOrThrow(_cursor, "listId");
          final int _cursorIndexOfTagId = CursorUtil.getColumnIndexOrThrow(_cursor, "tagId");
          final int _cursorIndexOfMinPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "minPriority");
          final int _cursorIndexOfDateType = CursorUtil.getColumnIndexOrThrow(_cursor, "dateType");
          final int _cursorIndexOfDueStart = CursorUtil.getColumnIndexOrThrow(_cursor, "dueStart");
          final int _cursorIndexOfDueEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "dueEnd");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final List<FilterEntity> _result = new ArrayList<FilterEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FilterEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpListId;
            if (_cursor.isNull(_cursorIndexOfListId)) {
              _tmpListId = null;
            } else {
              _tmpListId = _cursor.getString(_cursorIndexOfListId);
            }
            final String _tmpTagId;
            if (_cursor.isNull(_cursorIndexOfTagId)) {
              _tmpTagId = null;
            } else {
              _tmpTagId = _cursor.getString(_cursorIndexOfTagId);
            }
            final int _tmpMinPriority;
            _tmpMinPriority = _cursor.getInt(_cursorIndexOfMinPriority);
            final int _tmpDateType;
            _tmpDateType = _cursor.getInt(_cursorIndexOfDateType);
            final Long _tmpDueStart;
            if (_cursor.isNull(_cursorIndexOfDueStart)) {
              _tmpDueStart = null;
            } else {
              _tmpDueStart = _cursor.getLong(_cursorIndexOfDueStart);
            }
            final Long _tmpDueEnd;
            if (_cursor.isNull(_cursorIndexOfDueEnd)) {
              _tmpDueEnd = null;
            } else {
              _tmpDueEnd = _cursor.getLong(_cursorIndexOfDueEnd);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            _item = new FilterEntity(_tmpId,_tmpName,_tmpListId,_tmpTagId,_tmpMinPriority,_tmpDateType,_tmpDueStart,_tmpDueEnd,_tmpSortOrder);
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
  public Object maxSortOrder(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT MAX(sortOrder) FROM filters";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @Nullable
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp;
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
