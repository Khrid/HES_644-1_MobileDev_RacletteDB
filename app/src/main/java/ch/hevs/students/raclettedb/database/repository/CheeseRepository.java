package ch.hevs.students.raclettedb.database.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

import ch.hevs.students.raclettedb.BaseApp;
import ch.hevs.students.raclettedb.database.async.cheese.CreateCheese;
import ch.hevs.students.raclettedb.database.async.cheese.DeleteCheese;
import ch.hevs.students.raclettedb.database.async.cheese.UpdateCheese;
import ch.hevs.students.raclettedb.database.entity.CheeseEntity;
import ch.hevs.students.raclettedb.util.OnAsyncEventListener;

public class CheeseRepository {

    private static CheeseRepository instance;

    private CheeseRepository() {
    }

    public static CheeseRepository getInstance() {
        if (instance == null) {
            instance = new CheeseRepository();
            // TODO Ajouter le check de la deuxième table de la DB
            /*synchronized (AccountRepository.class) {
                if (instance == null) {
                    instance = new ClientRepository();
                }
            }*/
        }
        return instance;
    }

    public LiveData<CheeseEntity> getCheese(final Long cheeseId, Application application) {
        return ((BaseApp) application).getDatabase().cheeseDao().getById(cheeseId);
    }


    public LiveData<List<CheeseEntity>> getAllCheeses(Application application) {
        return ((BaseApp) application).getDatabase().cheeseDao().getAll();
    }

    public void insert(final CheeseEntity client, OnAsyncEventListener callback,
                       Application application) {
        new CreateCheese(application, callback).execute(client);
    }

    public void update(final CheeseEntity client, OnAsyncEventListener callback,
                       Application application) {
        new UpdateCheese(application, callback).execute(client);
    }

    public void delete(final CheeseEntity client, OnAsyncEventListener callback,
                       Application application) {
        new DeleteCheese(application, callback).execute(client);
    }
}

