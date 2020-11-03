package ch.hevs.students.raclettedb.database.dao;

import android.database.sqlite.SQLiteConstraintException;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ch.hevs.students.raclettedb.database.entity.CheeseEntity;
import ch.hevs.students.raclettedb.database.entity.ShielingEntity;

@Dao
public interface ShielingDao {

    @Query("SELECT * FROM shieling WHERE id = :id")
    LiveData<ShielingEntity> getById(Long id);

    @Query("SELECT * from shieling ORDER BY name ASC")
    LiveData<List<ShielingEntity>> getAll();

    @Insert()
    long insert(ShielingEntity shieling) throws SQLiteConstraintException;

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ShielingEntity> shielings);

    @Update
    void update(ShielingEntity shieling);

    @Delete
    void delete(ShielingEntity shieling);

    @Query("DELETE FROM shieling")
    void deleteAll();

}
