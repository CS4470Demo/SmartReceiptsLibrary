package co.smartreceipts.android.workers.reports;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import co.smartreceipts.android.model.Column;
import co.smartreceipts.android.model.Distance;
import co.smartreceipts.android.model.Receipt;
import co.smartreceipts.android.model.Trip;
import co.smartreceipts.android.persistence.DatabaseHelper;
import co.smartreceipts.android.persistence.PersistenceManager;
import co.smartreceipts.android.persistence.Preferences;
import co.smartreceipts.android.workers.reports.pdf.pdfbox.PdfBoxReportFile;
import wb.android.flex.Flex;
import wb.android.storage.StorageManager;

public class PdfBoxFullPdfReport extends AbstractReport {

    public PdfBoxFullPdfReport(@NonNull Context context, @NonNull PersistenceManager persistenceManager, Flex flex) {
        super(context, persistenceManager, flex);
    }

    protected PdfBoxFullPdfReport(@NonNull Context context, @NonNull DatabaseHelper db, @NonNull Preferences preferences, @NonNull StorageManager storageManager, Flex flex) {
        super(context, db, preferences, storageManager, flex);
    }

    @NonNull
    @Override
    public File generate(@NonNull Trip trip) throws ReportGenerationException {
        final String outputFileName = getFileName(trip);
        FileOutputStream pdfStream = null;

        try {
            getStorageManager().delete(trip.getDirectory(), outputFileName);

            pdfStream = getStorageManager().getFOS(trip.getDirectory(), outputFileName);

            PdfBoxReportFile pdfBoxReportFile = new PdfBoxReportFile(getContext(), getPreferences());

            final List<Receipt> receipts = new ArrayList<>(getDatabase().getReceiptsTable().getBlocking(trip, false));
            final List<Column<Receipt>> columns = getDatabase().getPDFTable().get().toBlocking().first();
            final List<Distance> distances = new ArrayList<>(getDatabase().getDistanceTable().getBlocking(trip, false));


            pdfBoxReportFile.addSection(
                    pdfBoxReportFile.createReceiptsTableSection(distances,
                            columns));


            pdfBoxReportFile.addSection(
                    pdfBoxReportFile.createReceiptsImagesSection());



            pdfBoxReportFile.writeFile(pdfStream, trip, receipts);

            return getStorageManager().getFile(trip.getDirectory(), outputFileName);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } finally {
            // TODO
        }

    }

    private String getFileName(Trip trip) {
        return trip.getDirectory().getName() + ".pdf";
    }
}