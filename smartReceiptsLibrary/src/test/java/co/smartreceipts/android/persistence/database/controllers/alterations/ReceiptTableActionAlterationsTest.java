package co.smartreceipts.android.persistence.database.controllers.alterations;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import co.smartreceipts.android.model.Price;
import co.smartreceipts.android.model.Receipt;
import co.smartreceipts.android.model.Trip;
import co.smartreceipts.android.model.factory.BuilderFactory1;
import co.smartreceipts.android.model.factory.ReceiptBuilderFactory;
import co.smartreceipts.android.persistence.database.tables.ReceiptsTable;
import rx.Observable;
import wb.android.storage.StorageManager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ReceiptTableActionAlterationsTest {

    // Class under test
    ReceiptTableActionAlterations mReceiptTableActionAlterations;

    @Mock
    ReceiptsTable mReceiptsTable;

    @Mock
    StorageManager mStorageManager;

    @Mock
    BuilderFactory1<Receipt, ReceiptBuilderFactory> mReceiptBuilderFactoryFactory;

    @Mock
    ReceiptBuilderFactory mReceiptBuilderFactory;

    @Mock
    Receipt mReceipt;

    @Mock
    Trip mTrip;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mReceipt.getTrip()).thenReturn(mTrip);
        when(mReceiptBuilderFactory.build()).thenReturn(mReceipt);
        when(mReceiptBuilderFactoryFactory.build(mReceipt)).thenReturn(mReceiptBuilderFactory);
        when(mReceiptBuilderFactory.setIndex(anyInt())).thenReturn(mReceiptBuilderFactory);

        doAnswer(new Answer() {
            @Override
            public File answer(InvocationOnMock invocation) throws Throwable {
                return new File((String)invocation.getArguments()[1]);
            }
        }).when(mStorageManager).getFile(any(File.class), anyString());
        doAnswer(new Answer() {
            @Override
            public File answer(InvocationOnMock invocation) throws Throwable {
                return new File((String)invocation.getArguments()[1]);
            }
        }).when(mStorageManager).rename(any(File.class), anyString());
        doAnswer(new Answer() {
            @Override
            public ReceiptBuilderFactory answer(InvocationOnMock invocation) throws Throwable {
                when(mReceipt.getFile()).thenReturn((File)invocation.getArguments()[0]);
                return mReceiptBuilderFactory;
            }
        }).when(mReceiptBuilderFactory).setFile(any(File.class));
        doAnswer(new Answer() {
            @Override
            public ReceiptBuilderFactory answer(InvocationOnMock invocation) throws Throwable {
                when(mReceipt.getIndex()).thenReturn((Integer) invocation.getArguments()[0]);
                return mReceiptBuilderFactory;
            }
        }).when(mReceiptBuilderFactory).setIndex(anyInt());

        mReceiptTableActionAlterations = new ReceiptTableActionAlterations(mReceiptsTable, mStorageManager, mReceiptBuilderFactoryFactory);
    }

    @Test
    public void preInsertWithoutFile() {
        final String name = "name";
        final List<Receipt> receiptsInTrip = Arrays.asList(mock(Receipt.class), mock(Receipt.class), mock(Receipt.class));
        when(mReceiptsTable.get(mTrip)).thenReturn(Observable.just(receiptsInTrip));
        when(mReceipt.hasFile()).thenReturn(false);
        when(mReceipt.getName()).thenReturn(name);
        assertEquals(mReceipt, mReceiptTableActionAlterations.preInsert(mReceipt).toBlocking().first());
        verify(mReceiptBuilderFactory).setIndex(4);
        assertNull(mReceipt.getFile());
    }

    @Test
    public void preInsertWithFile() {
        final String name = "name";
        final List<Receipt> receiptsInTrip = Arrays.asList(mock(Receipt.class), mock(Receipt.class), mock(Receipt.class));
        when(mReceiptsTable.get(mTrip)).thenReturn(Observable.just(receiptsInTrip));
        when(mReceipt.hasFile()).thenReturn(true);
        when(mReceipt.getName()).thenReturn(name);
        when(mReceipt.getFile()).thenReturn(new File("12345.jpg"));
        assertEquals(new File("4_name.jpg"), mReceiptTableActionAlterations.preInsert(mReceipt).toBlocking().first().getFile());
        verify(mReceiptBuilderFactory).setIndex(4);
    }

    @Test
    public void preInsertWithIllegalCharactersInName() {
        final String name = "before_|\\\\?*<\\:>+[]/'\n\r\t\0\f_after";
        final List<Receipt> receiptsInTrip = Arrays.asList(mock(Receipt.class), mock(Receipt.class), mock(Receipt.class));
        when(mReceiptsTable.get(mTrip)).thenReturn(Observable.just(receiptsInTrip));
        when(mReceipt.hasFile()).thenReturn(true);
        when(mReceipt.getName()).thenReturn(name);
        when(mReceipt.getFile()).thenReturn(new File("12345.jpg"));
        assertEquals(new File("4_before__after.jpg"), mReceiptTableActionAlterations.preInsert(mReceipt).toBlocking().first().getFile());
        verify(mReceiptBuilderFactory).setIndex(4);
    }

    @Test
    public void receiptNameWithIllegalCharacters() {
        final List<Receipt> receiptsInTrip = Arrays.asList(mock(Receipt.class), mock(Receipt.class), mock(Receipt.class));
        when(mReceiptsTable.get(mTrip)).thenReturn(Observable.just(receiptsInTrip));
    }

    @Test
    public void postUpdateFailure() throws Exception {
        final File file = new File("abc");
        when(mReceipt.hasFile()).thenReturn(true);
        when(mReceipt.getFile()).thenReturn(file);
        mReceiptTableActionAlterations.postUpdate(mReceipt, null);
        verifyZeroInteractions(mStorageManager);
    }

    @Test
    public void postUpdateSuccessWithoutFile() throws Exception {
        when(mReceipt.hasFile()).thenReturn(false);
        mReceiptTableActionAlterations.postUpdate(mReceipt, mock(Receipt.class));
        verifyZeroInteractions(mStorageManager);
    }

    @Test
    public void postUpdateSuccessWithSameFile() throws Exception {
        final Receipt updatedReceipt = mock(Receipt.class);
        final File file = new File("abc");
        when(mReceipt.hasFile()).thenReturn(true);
        when(updatedReceipt.hasFile()).thenReturn(true);
        when(mReceipt.getFile()).thenReturn(file);
        when(updatedReceipt.getFile()).thenReturn(file);
        mReceiptTableActionAlterations.postUpdate(mReceipt, updatedReceipt);
        verify(mStorageManager, never()).delete(file);
    }

    @Test
    public void postUpdateSuccessWithNewFile() throws Exception {
        final Receipt updatedReceipt = mock(Receipt.class);
        final File file = new File("abc");
        final File newFile = new File("efg");
        when(mReceipt.hasFile()).thenReturn(true);
        when(updatedReceipt.hasFile()).thenReturn(true);
        when(mReceipt.getFile()).thenReturn(file);
        when(updatedReceipt.getFile()).thenReturn(newFile);
        mReceiptTableActionAlterations.postUpdate(mReceipt, updatedReceipt);
        verify(mStorageManager).delete(file);
    }

    @Test
    public void postDeleteFailure() throws Exception {
        final File file = new File("abc");
        when(mReceipt.hasFile()).thenReturn(true);
        when(mReceipt.getFile()).thenReturn(file);
        mReceiptTableActionAlterations.postDelete(null);
        verifyZeroInteractions(mStorageManager);
    }

    @Test
    public void postDeleteSuccessWithoutFile() throws Exception {
        when(mReceipt.hasFile()).thenReturn(false);
        mReceiptTableActionAlterations.postDelete(mReceipt);
        verifyZeroInteractions(mStorageManager);
    }

    @Test
    public void postDeleteSuccessWithFile() throws Exception {
        final File file = new File("abc");
        when(mReceipt.hasFile()).thenReturn(true);
        when(mReceipt.getFile()).thenReturn(file);
        mReceiptTableActionAlterations.postDelete(mReceipt);
        verify(mStorageManager).delete(file);
    }


}