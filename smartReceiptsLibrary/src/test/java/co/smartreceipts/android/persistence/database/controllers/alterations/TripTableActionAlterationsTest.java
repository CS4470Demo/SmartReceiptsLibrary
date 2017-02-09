package co.smartreceipts.android.persistence.database.controllers.alterations;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executor;

import co.smartreceipts.android.ImmediateExecutor;
import co.smartreceipts.android.model.Price;
import co.smartreceipts.android.model.Trip;
import co.smartreceipts.android.persistence.DatabaseHelper;
import co.smartreceipts.android.persistence.database.operations.DatabaseOperationMetadata;
import co.smartreceipts.android.persistence.database.tables.DistanceTable;
import co.smartreceipts.android.persistence.database.tables.ReceiptsTable;
import co.smartreceipts.android.persistence.database.tables.Table;
import rx.Observable;
import wb.android.storage.StorageManager;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class TripTableActionAlterationsTest {

    TripTableActionAlterations mTripTableActionAlterations;

    @Mock
    Table<Trip, String> mTripsTable;

    @Mock
    ReceiptsTable mReceiptsTable;

    @Mock
    DistanceTable mDistanceTable;

    @Mock
    DatabaseHelper mDatabaseHelper;

    @Mock
    StorageManager mStorageManager;

    @Mock
    Trip mTrip1;

    @Mock
    Trip mTrip2;

    @Mock
    Price mPrice1;

    @Mock
    Price mPrice2;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mTripTableActionAlterations = new TripTableActionAlterations(mTripsTable, mReceiptsTable, mDistanceTable, mDatabaseHelper, mStorageManager, new ImmediateExecutor());
        when(mTrip1.getPrice()).thenReturn(mPrice1);
        when(mTrip1.getDailySubTotal()).thenReturn(mPrice2);
    }

    @Test
    public void postGet() throws Exception {
        mTripTableActionAlterations.postGet(Arrays.asList(mTrip1, mTrip2));
        verify(mDatabaseHelper).getTripPriceAndDailyPrice(mTrip1);
        verify(mDatabaseHelper).getTripPriceAndDailyPrice(mTrip2);
    }

    @Test
    public void postInsertForNullTrip() throws Exception {
        mTripTableActionAlterations.postInsert(null); // No exceptions
        verify(mTripsTable, never()).delete(mTrip1, new DatabaseOperationMetadata());
    }

    @Test
    public void postInsertForValidTrip() throws Exception {
        final String name = "name";
        when(mTrip1.getName()).thenReturn(name);
        when(mStorageManager.mkdir(name)).thenReturn(new File(name));
        mTripTableActionAlterations.postInsert(mTrip1); // No exceptions
        verify(mTripsTable, never()).delete(mTrip1, new DatabaseOperationMetadata());
    }

    @Test (expected = IOException.class)
    public void postInsertForValidTripButIOFails() throws Exception {
        final String name = "name";
        when(mTrip1.getName()).thenReturn(name);
        when(mStorageManager.mkdir(name)).thenReturn(null);
        when(mTripsTable.delete(mTrip1, new DatabaseOperationMetadata())).thenReturn(Observable.just(mTrip1));
        mTripTableActionAlterations.postInsert(mTrip1);
        verify(mTripsTable).delete(mTrip1, new DatabaseOperationMetadata());
    }

    @Test
    public void postUpdateForNullTrip() throws Exception {
        mTripTableActionAlterations.postUpdate(mTrip1, null);
    }

    @Test
    public void postUpdateForTripWithSameName() throws Exception {
        final String name = "name";
        when(mTrip1.getName()).thenReturn(name);
        when(mTrip2.getName()).thenReturn(name);
        when(mTrip1.getDirectory()).thenReturn(new File(name));
        when(mTrip2.getDirectory()).thenReturn(new File(name));
        mTripTableActionAlterations.postUpdate(mTrip1, mTrip2);
        verify(mTrip2).setPrice(mPrice1);
        verify(mTrip2).setDailySubTotal(mPrice2);
        verifyZeroInteractions(mStorageManager, mReceiptsTable, mDistanceTable);
    }

    @Test
    public void postUpdateForValidTrip() throws Exception {
        final String name1 = "name1";
        final String name2 = "name2";
        when(mTrip1.getName()).thenReturn(name1);
        when(mTrip2.getName()).thenReturn(name2);
        when(mTrip1.getDirectory()).thenReturn(new File(name1));
        when(mStorageManager.rename(new File(name1), name2)).thenReturn(new File(name2));
        mTripTableActionAlterations.postUpdate(mTrip1, mTrip2);

        verify(mTrip2).setPrice(mPrice1);
        verify(mTrip2).setDailySubTotal(mPrice2);
        verify(mReceiptsTable).updateParentBlocking(mTrip1, mTrip2);
        verify(mDistanceTable).updateParentBlocking(mTrip1, mTrip2);
        verify(mTripsTable, never()).update(mTrip2, mTrip1, new DatabaseOperationMetadata());
        verify(mReceiptsTable, never()).updateParentBlocking(mTrip2, mTrip1);
        verify(mDistanceTable, never()).updateParentBlocking(mTrip2, mTrip1);
    }

    @Test (expected = IOException.class)
    public void postUpdateForValidTripButIOFails() throws Exception {
        final String name1 = "name1";
        final String name2 = "name2";
        when(mTrip1.getName()).thenReturn(name1);
        when(mTrip2.getName()).thenReturn(name2);
        when(mTrip1.getDirectory()).thenReturn(new File(name1));
        when(mStorageManager.rename(new File(name1), name2)).thenReturn(new File(name1));
        when(mTripsTable.update(mTrip2, mTrip1, new DatabaseOperationMetadata())).thenReturn(Observable.just(mTrip1));
        mTripTableActionAlterations.postUpdate(mTrip1, mTrip2);

        verify(mTrip2).setPrice(mPrice1);
        verify(mTrip2).setDailySubTotal(mPrice2);
        verify(mReceiptsTable).updateParentBlocking(mTrip1, mTrip2);
        verify(mDistanceTable).updateParentBlocking(mTrip1, mTrip2);
        verify(mReceiptsTable).updateParentBlocking(mTrip2, mTrip1);
        verify(mDistanceTable).updateParentBlocking(mTrip2, mTrip1);
        verify(mTripsTable).update(mTrip2, mTrip1, new DatabaseOperationMetadata());
    }

    @Test
    public void postDeleteFailed() throws Exception {
        mTripTableActionAlterations.postDelete(null);
        verifyZeroInteractions(mReceiptsTable, mDistanceTable);
    }

    @Test
    public void postDeleteSuccess() throws Exception {
        final File dir = new File("name");
        when(mTrip1.getDirectory()).thenReturn(dir);
        mTripTableActionAlterations.postDelete(mTrip1);
        verify(mReceiptsTable).deleteParentBlocking(mTrip1);
        verify(mDistanceTable).deleteParentBlocking(mTrip1);
        verify(mStorageManager).deleteRecursively(dir);
    }

}