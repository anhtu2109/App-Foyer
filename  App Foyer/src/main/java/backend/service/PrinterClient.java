package backend.service;

/**
 * Abstraction responsible for sending bytes to a printer device.
 */
public interface PrinterClient {
    void print(byte[] payload);
}

