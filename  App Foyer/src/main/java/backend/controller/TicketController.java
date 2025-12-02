package backend.controller;

import backend.service.TicketPrinterService;

/**
 * Backend controller exposed to the UI layer for ticket printing actions.
 */
public class TicketController {
    private final TicketPrinterService ticketPrinterService;

    public TicketController(TicketPrinterService ticketPrinterService) {
        this.ticketPrinterService = ticketPrinterService;
    }

    public void printTicket(long orderId) {
        ticketPrinterService.printTicket(orderId);
    }
}

