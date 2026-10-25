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
public final class HabitDao_Impl implements HabitDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HabitEntity> __insertionAdapterOfHabitEntity;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  private final SharedSQLiteStatement __preparedStmtOfSetArchived;

  private final SharedSQLiteStatement __preparedStmtOfSetSortOrder;

  public HabitDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHabitEntity = new EntityInsertionAdapter<HabitEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `habits` (`id`,`name`,`emoji`,`color`,`category`,`note`,`freqType`,`weekdays`,`timesPerWeek`,`timesPerMonth`,`reminderEnabled`,`reminderHour`,`reminderMinute`,`nagEnabled`,`nagHour`,`nagMinute`,`archived`,`sortOrder`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HabitEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getEmoji());
        statement.bindLong(4, entity.getColor());
        if (entity.getCategory() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getCategory());
        }
        statement.bindString(6, entity.getNote());
        statement.bindLong(7, entity.getFreqType());
        final String _tmp = __converters.fromStringList(entity.getWeekdays());
        statement.bindString(8, _tmp);
        statement.bindLong(9, entity.getTimesPerWeek());
        statement.bindLong(10, entity.getTimesPerMonth());
        final int _tmp_1 = entity.getReminderEnabled() ? 1 : 0;
        statement.bindLong(11, _tmp_1);
        statement.bindLong(12, entity.getReminderHour());
        statement.bindLong(13, entity.getReminderMinute());
        final int _tmp_2 = entity.getNagEnabled() ? 1 : 0;
        statement.bindLong(14, _tmp_2);
        statement.bindLong(15, entity.getNagHour());
        statement.bindLong(16, entity.getNagMinute());
        final int _tmp_3 = entity.getArchived() ? 1 : 0;
        statement.bindLong(17, _tmp_3);
        statement.bindLong(18, entity.getSortOrder());
        statement.bindLong(19, entity.getCreatedAt());
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM habits WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetArchived = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE habits SET archived = ?, sortOrder = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetSortOrder = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE habits SET sortOrder = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final HabitEntity habit, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHabitEntity.insert(habit);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<HabitEntity> habits,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHabitEntity.insert(habits);
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
  public Object setArchived(final String id, final boolean archived, final int sortOrder,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetArchived.acquire();
        int _argIndex = 1;
        final int _tmp = archived ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, sortOrder);
        _argIndex = 3;
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
          __preparedStmtOfSetArchived.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setSortOrder(final String id, final int sortOrder,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetSortOrder.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, sortOrder);
        _argIndex = 2;
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
          __preparedStmtOfSetSortOrder.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<HabitEntity>> observeActive() {
    final String _sql = "SELECT * FROM habits WHERE archived = 0 ORDER BY sortOrder ASC, createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"habits"}, new Callable<List<HabitEntity>>() {
      @Override
      @NonNull
      public List<HabitEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEmoji = CursorUtil.getColumnIndexOrThrow(_cursor, "emoji");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfFreqType = CursorUtil.getColumnIndexOrThrow(_cursor, "freqType");
          final int _cursorIndexOfWeekdays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdays");
          final int _cursorIndexOfTimesPerWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "timesPerWeek");
          final int _cursorIndexOfTimesPerMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "timesPerMonth");
          final int _cursorIndexOfReminderEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderEnabled");
          final int _cursorIndexOfReminderHour = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderHour");
          final int _cursorIndexOfReminderMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderMinute");
          final int _cursorIndexOfNagEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "nagEnabled");
          final int _cursorIndexOfNagHour = CursorUtil.getColumnIndexOrThrow(_cursor, "nagHour");
          final int _cursorIndexOfNagMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "nagMinute");
          final int _cursorIndexOfArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "archived");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<HabitEntity> _result = new ArrayList<HabitEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HabitEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpEmoji;
            _tmpEmoji = _cursor.getString(_cursorIndexOfEmoji);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final int _tmpFreqType;
            _tmpFreqType = _cursor.getInt(_cursorIndexOfFreqType);
            final List<String> _tmpWeekdays;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeekdays);
            _tmpWeekdays = __converters.toStringList(_tmp);
            final int _tmpTimesPerWeek;
            _tmpTimesPerWeek = _cursor.getInt(_cursorIndexOfTimesPerWeek);
            final int _tmpTimesPerMonth;
            _tmpTimesPerMonth = _cursor.getInt(_cursorIndexOfTimesPerMonth);
            final boolean _tmpReminderEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfReminderEnabled);
            _tmpReminderEnabled = _tmp_1 != 0;
            final int _tmpReminderHour;
            _tmpReminderHour = _cursor.getInt(_cursorIndexOfReminderHour);
            final int _tmpReminderMinute;
            _tmpReminderMinute = _cursor.getInt(_cursorIndexOfReminderMinute);
            final boolean _tmpNagEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNagEnabled);
            _tmpNagEnabled = _tmp_2 != 0;
            final int _tmpNagHour;
            _tmpNagHour = _cursor.getInt(_cursorIndexOfNagHour);
            final int _tmpNagMinute;
            _tmpNagMinute = _cursor.getInt(_cursorIndexOfNagMinute);
            final boolean _tmpArchived;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfArchived);
            _tmpArchived = _tmp_3 != 0;
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new HabitEntity(_tmpId,_tmpName,_tmpEmoji,_tmpColor,_tmpCategory,_tmpNote,_tmpFreqType,_tmpWeekdays,_tmpTimesPerWeek,_tmpTimesPerMonth,_tmpReminderEnabled,_tmpReminderHour,_tmpReminderMinute,_tmpNagEnabled,_tmpNagHour,_tmpNagMinute,_tmpArchived,_tmpSortOrder,_tmpCreatedAt);
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
  public Flow<List<HabitEntity>> observeArchived() {
    final String _sql = "SELECT * FROM habits WHERE archived = 1 ORDER BY sortOrder ASC, createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"habits"}, new Callable<List<HabitEntity>>() {
      @Override
      @NonNull
      public List<HabitEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEmoji = CursorUtil.getColumnIndexOrThrow(_cursor, "emoji");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfFreqType = CursorUtil.getColumnIndexOrThrow(_cursor, "freqType");
          final int _cursorIndexOfWeekdays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdays");
          final int _cursorIndexOfTimesPerWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "timesPerWeek");
          final int _cursorIndexOfTimesPerMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "timesPerMonth");
          final int _cursorIndexOfReminderEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderEnabled");
          final int _cursorIndexOfReminderHour = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderHour");
          final int _cursorIndexOfReminderMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderMinute");
          final int _cursorIndexOfNagEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "nagEnabled");
          final int _cursorIndexOfNagHour = CursorUtil.getColumnIndexOrThrow(_cursor, "nagHour");
          final int _cursorIndexOfNagMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "nagMinute");
          final int _cursorIndexOfArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "archived");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<HabitEntity> _result = new ArrayList<HabitEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HabitEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpEmoji;
            _tmpEmoji = _cursor.getString(_cursorIndexOfEmoji);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final int _tmpFreqType;
            _tmpFreqType = _cursor.getInt(_cursorIndexOfFreqType);
            final List<String> _tmpWeekdays;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeekdays);
            _tmpWeekdays = __converters.toStringList(_tmp);
            final int _tmpTimesPerWeek;
            _tmpTimesPerWeek = _cursor.getInt(_cursorIndexOfTimesPerWeek);
            final int _tmpTimesPerMonth;
            _tmpTimesPerMonth = _cursor.getInt(_cursorIndexOfTimesPerMonth);
            final boolean _tmpReminderEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfReminderEnabled);
            _tmpReminderEnabled = _tmp_1 != 0;
            final int _tmpReminderHour;
            _tmpReminderHour = _cursor.getInt(_cursorIndexOfReminderHour);
            final int _tmpReminderMinute;
            _tmpReminderMinute = _cursor.getInt(_cursorIndexOfReminderMinute);
            final boolean _tmpNagEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNagEnabled);
            _tmpNagEnabled = _tmp_2 != 0;
            final int _tmpNagHour;
            _tmpNagHour = _cursor.getInt(_cursorIndexOfNagHour);
            final int _tmpNagMinute;
            _tmpNagMinute = _cursor.getInt(_cursorIndexOfNagMinute);
            final boolean _tmpArchived;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfArchived);
            _tmpArchived = _tmp_3 != 0;
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new HabitEntity(_tmpId,_tmpName,_tmpEmoji,_tmpColor,_tmpCategory,_tmpNote,_tmpFreqType,_tmpWeekdays,_tmpTimesPerWeek,_tmpTimesPerMonth,_tmpReminderEnabled,_tmpReminderHour,_tmpReminderMinute,_tmpNagEnabled,_tmpNagHour,_tmpNagMinute,_tmpArchived,_tmpSortOrder,_tmpCreatedAt);
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
  public Flow<List<HabitEntity>> observeAll() {
    final String _sql = "SELECT * FROM habits ORDER BY sortOrder ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"habits"}, new Callable<List<HabitEntity>>() {
      @Override
      @NonNull
      public List<HabitEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEmoji = CursorUtil.getColumnIndexOrThrow(_cursor, "emoji");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfFreqType = CursorUtil.getColumnIndexOrThrow(_cursor, "freqType");
          final int _cursorIndexOfWeekdays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdays");
          final int _cursorIndexOfTimesPerWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "timesPerWeek");
          final int _cursorIndexOfTimesPerMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "timesPerMonth");
          final int _cursorIndexOfReminderEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderEnabled");
          final int _cursorIndexOfReminderHour = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderHour");
          final int _cursorIndexOfReminderMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderMinute");
          final int _cursorIndexOfNagEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "nagEnabled");
          final int _cursorIndexOfNagHour = CursorUtil.getColumnIndexOrThrow(_cursor, "nagHour");
          final int _cursorIndexOfNagMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "nagMinute");
          final int _cursorIndexOfArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "archived");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<HabitEntity> _result = new ArrayList<HabitEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HabitEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpEmoji;
            _tmpEmoji = _cursor.getString(_cursorIndexOfEmoji);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final int _tmpFreqType;
            _tmpFreqType = _cursor.getInt(_cursorIndexOfFreqType);
            final List<String> _tmpWeekdays;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeekdays);
            _tmpWeekdays = __converters.toStringList(_tmp);
            final int _tmpTimesPerWeek;
            _tmpTimesPerWeek = _cursor.getInt(_cursorIndexOfTimesPerWeek);
            final int _tmpTimesPerMonth;
            _tmpTimesPerMonth = _cursor.getInt(_cursorIndexOfTimesPerMonth);
            final boolean _tmpReminderEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfReminderEnabled);
            _tmpReminderEnabled = _tmp_1 != 0;
            final int _tmpReminderHour;
            _tmpReminderHour = _cursor.getInt(_cursorIndexOfReminderHour);
            final int _tmpReminderMinute;
            _tmpReminderMinute = _cursor.getInt(_cursorIndexOfReminderMinute);
            final boolean _tmpNagEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNagEnabled);
            _tmpNagEnabled = _tmp_2 != 0;
            final int _tmpNagHour;
            _tmpNagHour = _cursor.getInt(_cursorIndexOfNagHour);
            final int _tmpNagMinute;
            _tmpNagMinute = _cursor.getInt(_cursorIndexOfNagMinute);
            final boolean _tmpArchived;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfArchived);
            _tmpArchived = _tmp_3 != 0;
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new HabitEntity(_tmpId,_tmpName,_tmpEmoji,_tmpColor,_tmpCategory,_tmpNote,_tmpFreqType,_tmpWeekdays,_tmpTimesPerWeek,_tmpTimesPerMonth,_tmpReminderEnabled,_tmpReminderHour,_tmpReminderMinute,_tmpNagEnabled,_tmpNagHour,_tmpNagMinute,_tmpArchived,_tmpSortOrder,_tmpCreatedAt);
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
  public Object all(final Continuation<? super List<HabitEntity>> $completion) {
    final String _sql = "SELECT * FROM habits ORDER BY sortOrder ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<HabitEntity>>() {
      @Override
      @NonNull
      public List<HabitEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEmoji = CursorUtil.getColumnIndexOrThrow(_cursor, "emoji");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfFreqType = CursorUtil.getColumnIndexOrThrow(_cursor, "freqType");
          final int _cursorIndexOfWeekdays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdays");
          final int _cursorIndexOfTimesPerWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "timesPerWeek");
          final int _cursorIndexOfTimesPerMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "timesPerMonth");
          final int _cursorIndexOfReminderEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderEnabled");
          final int _cursorIndexOfReminderHour = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderHour");
          final int _cursorIndexOfReminderMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderMinute");
          final int _cursorIndexOfNagEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "nagEnabled");
          final int _cursorIndexOfNagHour = CursorUtil.getColumnIndexOrThrow(_cursor, "nagHour");
          final int _cursorIndexOfNagMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "nagMinute");
          final int _cursorIndexOfArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "archived");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<HabitEntity> _result = new ArrayList<HabitEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HabitEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpEmoji;
            _tmpEmoji = _cursor.getString(_cursorIndexOfEmoji);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final int _tmpFreqType;
            _tmpFreqType = _cursor.getInt(_cursorIndexOfFreqType);
            final List<String> _tmpWeekdays;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeekdays);
            _tmpWeekdays = __converters.toStringList(_tmp);
            final int _tmpTimesPerWeek;
            _tmpTimesPerWeek = _cursor.getInt(_cursorIndexOfTimesPerWeek);
            final int _tmpTimesPerMonth;
            _tmpTimesPerMonth = _cursor.getInt(_cursorIndexOfTimesPerMonth);
            final boolean _tmpReminderEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfReminderEnabled);
            _tmpReminderEnabled = _tmp_1 != 0;
            final int _tmpReminderHour;
            _tmpReminderHour = _cursor.getInt(_cursorIndexOfReminderHour);
            final int _tmpReminderMinute;
            _tmpReminderMinute = _cursor.getInt(_cursorIndexOfReminderMinute);
            final boolean _tmpNagEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNagEnabled);
            _tmpNagEnabled = _tmp_2 != 0;
            final int _tmpNagHour;
            _tmpNagHour = _cursor.getInt(_cursorIndexOfNagHour);
            final int _tmpNagMinute;
            _tmpNagMinute = _cursor.getInt(_cursorIndexOfNagMinute);
            final boolean _tmpArchived;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfArchived);
            _tmpArchived = _tmp_3 != 0;
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new HabitEntity(_tmpId,_tmpName,_tmpEmoji,_tmpColor,_tmpCategory,_tmpNote,_tmpFreqType,_tmpWeekdays,_tmpTimesPerWeek,_tmpTimesPerMonth,_tmpReminderEnabled,_tmpReminderHour,_tmpReminderMinute,_tmpNagEnabled,_tmpNagHour,_tmpNagMinute,_tmpArchived,_tmpSortOrder,_tmpCreatedAt);
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
  public Object byId(final String id, final Continuation<? super HabitEntity> $completion) {
    final String _sql = "SELECT * FROM habits WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<HabitEntity>() {
      @Override
      @Nullable
      public HabitEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEmoji = CursorUtil.getColumnIndexOrThrow(_cursor, "emoji");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfFreqType = CursorUtil.getColumnIndexOrThrow(_cursor, "freqType");
          final int _cursorIndexOfWeekdays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdays");
          final int _cursorIndexOfTimesPerWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "timesPerWeek");
          final int _cursorIndexOfTimesPerMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "timesPerMonth");
          final int _cursorIndexOfReminderEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderEnabled");
          final int _cursorIndexOfReminderHour = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderHour");
          final int _cursorIndexOfReminderMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderMinute");
          final int _cursorIndexOfNagEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "nagEnabled");
          final int _cursorIndexOfNagHour = CursorUtil.getColumnIndexOrThrow(_cursor, "nagHour");
          final int _cursorIndexOfNagMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "nagMinute");
          final int _cursorIndexOfArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "archived");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final HabitEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpEmoji;
            _tmpEmoji = _cursor.getString(_cursorIndexOfEmoji);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final int _tmpFreqType;
            _tmpFreqType = _cursor.getInt(_cursorIndexOfFreqType);
            final List<String> _tmpWeekdays;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeekdays);
            _tmpWeekdays = __converters.toStringList(_tmp);
            final int _tmpTimesPerWeek;
            _tmpTimesPerWeek = _cursor.getInt(_cursorIndexOfTimesPerWeek);
            final int _tmpTimesPerMonth;
            _tmpTimesPerMonth = _cursor.getInt(_cursorIndexOfTimesPerMonth);
            final boolean _tmpReminderEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfReminderEnabled);
            _tmpReminderEnabled = _tmp_1 != 0;
            final int _tmpReminderHour;
            _tmpReminderHour = _cursor.getInt(_cursorIndexOfReminderHour);
            final int _tmpReminderMinute;
            _tmpReminderMinute = _cursor.getInt(_cursorIndexOfReminderMinute);
            final boolean _tmpNagEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNagEnabled);
            _tmpNagEnabled = _tmp_2 != 0;
            final int _tmpNagHour;
            _tmpNagHour = _cursor.getInt(_cursorIndexOfNagHour);
            final int _tmpNagMinute;
            _tmpNagMinute = _cursor.getInt(_cursorIndexOfNagMinute);
            final boolean _tmpArchived;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfArchived);
            _tmpArchived = _tmp_3 != 0;
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new HabitEntity(_tmpId,_tmpName,_tmpEmoji,_tmpColor,_tmpCategory,_tmpNote,_tmpFreqType,_tmpWeekdays,_tmpTimesPerWeek,_tmpTimesPerMonth,_tmpReminderEnabled,_tmpReminderHour,_tmpReminderMinute,_tmpNagEnabled,_tmpNagHour,_tmpNagMinute,_tmpArchived,_tmpSortOrder,_tmpCreatedAt);
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
  public Flow<HabitEntity> observeById(final String id) {
    final String _sql = "SELECT * FROM habits WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"habits"}, new Callable<HabitEntity>() {
      @Override
      @Nullable
      public HabitEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEmoji = CursorUtil.getColumnIndexOrThrow(_cursor, "emoji");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfFreqType = CursorUtil.getColumnIndexOrThrow(_cursor, "freqType");
          final int _cursorIndexOfWeekdays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdays");
          final int _cursorIndexOfTimesPerWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "timesPerWeek");
          final int _cursorIndexOfTimesPerMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "timesPerMonth");
          final int _cursorIndexOfReminderEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderEnabled");
          final int _cursorIndexOfReminderHour = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderHour");
          final int _cursorIndexOfReminderMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderMinute");
          final int _cursorIndexOfNagEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "nagEnabled");
          final int _cursorIndexOfNagHour = CursorUtil.getColumnIndexOrThrow(_cursor, "nagHour");
          final int _cursorIndexOfNagMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "nagMinute");
          final int _cursorIndexOfArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "archived");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final HabitEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpEmoji;
            _tmpEmoji = _cursor.getString(_cursorIndexOfEmoji);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final int _tmpFreqType;
            _tmpFreqType = _cursor.getInt(_cursorIndexOfFreqType);
            final List<String> _tmpWeekdays;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeekdays);
            _tmpWeekdays = __converters.toStringList(_tmp);
            final int _tmpTimesPerWeek;
            _tmpTimesPerWeek = _cursor.getInt(_cursorIndexOfTimesPerWeek);
            final int _tmpTimesPerMonth;
            _tmpTimesPerMonth = _cursor.getInt(_cursorIndexOfTimesPerMonth);
            final boolean _tmpReminderEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfReminderEnabled);
            _tmpReminderEnabled = _tmp_1 != 0;
            final int _tmpReminderHour;
            _tmpReminderHour = _cursor.getInt(_cursorIndexOfReminderHour);
            final int _tmpReminderMinute;
            _tmpReminderMinute = _cursor.getInt(_cursorIndexOfReminderMinute);
            final boolean _tmpNagEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNagEnabled);
            _tmpNagEnabled = _tmp_2 != 0;
            final int _tmpNagHour;
            _tmpNagHour = _cursor.getInt(_cursorIndexOfNagHour);
            final int _tmpNagMinute;
            _tmpNagMinute = _cursor.getInt(_cursorIndexOfNagMinute);
            final boolean _tmpArchived;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfArchived);
            _tmpArchived = _tmp_3 != 0;
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new HabitEntity(_tmpId,_tmpName,_tmpEmoji,_tmpColor,_tmpCategory,_tmpNote,_tmpFreqType,_tmpWeekdays,_tmpTimesPerWeek,_tmpTimesPerMonth,_tmpReminderEnabled,_tmpReminderHour,_tmpReminderMinute,_tmpNagEnabled,_tmpNagHour,_tmpNagMinute,_tmpArchived,_tmpSortOrder,_tmpCreatedAt);
          } else {
            _result = null;
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
  public Object withReminder(final Continuation<? super List<HabitEntity>> $completion) {
    final String _sql = "SELECT * FROM habits WHERE archived = 0 AND reminderEnabled = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<HabitEntity>>() {
      @Override
      @NonNull
      public List<HabitEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEmoji = CursorUtil.getColumnIndexOrThrow(_cursor, "emoji");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfFreqType = CursorUtil.getColumnIndexOrThrow(_cursor, "freqType");
          final int _cursorIndexOfWeekdays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdays");
          final int _cursorIndexOfTimesPerWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "timesPerWeek");
          final int _cursorIndexOfTimesPerMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "timesPerMonth");
          final int _cursorIndexOfReminderEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderEnabled");
          final int _cursorIndexOfReminderHour = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderHour");
          final int _cursorIndexOfReminderMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderMinute");
          final int _cursorIndexOfNagEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "nagEnabled");
          final int _cursorIndexOfNagHour = CursorUtil.getColumnIndexOrThrow(_cursor, "nagHour");
          final int _cursorIndexOfNagMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "nagMinute");
          final int _cursorIndexOfArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "archived");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<HabitEntity> _result = new ArrayList<HabitEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HabitEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpEmoji;
            _tmpEmoji = _cursor.getString(_cursorIndexOfEmoji);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final int _tmpFreqType;
            _tmpFreqType = _cursor.getInt(_cursorIndexOfFreqType);
            final List<String> _tmpWeekdays;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeekdays);
            _tmpWeekdays = __converters.toStringList(_tmp);
            final int _tmpTimesPerWeek;
            _tmpTimesPerWeek = _cursor.getInt(_cursorIndexOfTimesPerWeek);
            final int _tmpTimesPerMonth;
            _tmpTimesPerMonth = _cursor.getInt(_cursorIndexOfTimesPerMonth);
            final boolean _tmpReminderEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfReminderEnabled);
            _tmpReminderEnabled = _tmp_1 != 0;
            final int _tmpReminderHour;
            _tmpReminderHour = _cursor.getInt(_cursorIndexOfReminderHour);
            final int _tmpReminderMinute;
            _tmpReminderMinute = _cursor.getInt(_cursorIndexOfReminderMinute);
            final boolean _tmpNagEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNagEnabled);
            _tmpNagEnabled = _tmp_2 != 0;
            final int _tmpNagHour;
            _tmpNagHour = _cursor.getInt(_cursorIndexOfNagHour);
            final int _tmpNagMinute;
            _tmpNagMinute = _cursor.getInt(_cursorIndexOfNagMinute);
            final boolean _tmpArchived;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfArchived);
            _tmpArchived = _tmp_3 != 0;
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new HabitEntity(_tmpId,_tmpName,_tmpEmoji,_tmpColor,_tmpCategory,_tmpNote,_tmpFreqType,_tmpWeekdays,_tmpTimesPerWeek,_tmpTimesPerMonth,_tmpReminderEnabled,_tmpReminderHour,_tmpReminderMinute,_tmpNagEnabled,_tmpNagHour,_tmpNagMinute,_tmpArchived,_tmpSortOrder,_tmpCreatedAt);
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
  public Object withNag(final Continuation<? super List<HabitEntity>> $completion) {
    final String _sql = "SELECT * FROM habits WHERE archived = 0 AND nagEnabled = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<HabitEntity>>() {
      @Override
      @NonNull
      public List<HabitEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEmoji = CursorUtil.getColumnIndexOrThrow(_cursor, "emoji");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfFreqType = CursorUtil.getColumnIndexOrThrow(_cursor, "freqType");
          final int _cursorIndexOfWeekdays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdays");
          final int _cursorIndexOfTimesPerWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "timesPerWeek");
          final int _cursorIndexOfTimesPerMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "timesPerMonth");
          final int _cursorIndexOfReminderEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderEnabled");
          final int _cursorIndexOfReminderHour = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderHour");
          final int _cursorIndexOfReminderMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "reminderMinute");
          final int _cursorIndexOfNagEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "nagEnabled");
          final int _cursorIndexOfNagHour = CursorUtil.getColumnIndexOrThrow(_cursor, "nagHour");
          final int _cursorIndexOfNagMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "nagMinute");
          final int _cursorIndexOfArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "archived");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<HabitEntity> _result = new ArrayList<HabitEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HabitEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpEmoji;
            _tmpEmoji = _cursor.getString(_cursorIndexOfEmoji);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final int _tmpFreqType;
            _tmpFreqType = _cursor.getInt(_cursorIndexOfFreqType);
            final List<String> _tmpWeekdays;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfWeekdays);
            _tmpWeekdays = __converters.toStringList(_tmp);
            final int _tmpTimesPerWeek;
            _tmpTimesPerWeek = _cursor.getInt(_cursorIndexOfTimesPerWeek);
            final int _tmpTimesPerMonth;
            _tmpTimesPerMonth = _cursor.getInt(_cursorIndexOfTimesPerMonth);
            final boolean _tmpReminderEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfReminderEnabled);
            _tmpReminderEnabled = _tmp_1 != 0;
            final int _tmpReminderHour;
            _tmpReminderHour = _cursor.getInt(_cursorIndexOfReminderHour);
            final int _tmpReminderMinute;
            _tmpReminderMinute = _cursor.getInt(_cursorIndexOfReminderMinute);
            final boolean _tmpNagEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNagEnabled);
            _tmpNagEnabled = _tmp_2 != 0;
            final int _tmpNagHour;
            _tmpNagHour = _cursor.getInt(_cursorIndexOfNagHour);
            final int _tmpNagMinute;
            _tmpNagMinute = _cursor.getInt(_cursorIndexOfNagMinute);
            final boolean _tmpArchived;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfArchived);
            _tmpArchived = _tmp_3 != 0;
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new HabitEntity(_tmpId,_tmpName,_tmpEmoji,_tmpColor,_tmpCategory,_tmpNote,_tmpFreqType,_tmpWeekdays,_tmpTimesPerWeek,_tmpTimesPerMonth,_tmpReminderEnabled,_tmpReminderHour,_tmpReminderMinute,_tmpNagEnabled,_tmpNagHour,_tmpNagMinute,_tmpArchived,_tmpSortOrder,_tmpCreatedAt);
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
    final String _sql = "SELECT MAX(sortOrder) FROM habits WHERE archived = 0";
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
