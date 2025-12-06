package backend.service;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import java.util.Arrays;

/**
 * Implementation that uses the Java Print Service API to send jobs to a desktop printer.
 */
public class JavaxPrinterClient implements PrinterClient {
    private final PrintService printService;

    public JavaxPrinterClient() {
        this.printService = resolveDefaultPrinter();
    }

    public JavaxPrinterClient(String printerName) {
        this.printService = Arrays.stream(PrintServiceLookup.lookupPrintServices(null, null))
                .filter(service -> service.getName().equals(printerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Printer not found: " + printerName));
    }

    private PrintService resolveDefaultPrinter() {
        PrintService service = PrintServiceLookup.lookupDefaultPrintService();
        if (service == null) {
            throw new IllegalStateException("No default printer configured on this system");
        }
        return service;
    }

    @Override
    public void print(byte[] payload) {
        Doc doc = new SimpleDoc(payload, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        DocPrintJob job = printService.createPrintJob();
        try {
            job.print(doc, null);
        } catch (PrintException e) {
            throw new IllegalStateException("Failed to send ticket to printer", e);
        }
    }
}

