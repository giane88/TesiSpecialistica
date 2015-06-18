public boolean fermaTelegestione(String ce_id) {
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket socket = context.socket(ZMQ.REQ);
    System.out.println("CONNESSIONE AL GESTORE RICHIESTE/RISPOSTE: FERMA TELEGESTIONE");
    socket.connect(SERVER_ENDPOINT);
    int retriesLeft = REQUEST_RETRIES;
    boolean result = false;
    while (retriesLeft > 0) {
        // We send a request, then we work to get a reply
        String requestString = ce_id + ";FINE", risposta = ce_id+ ";FINE;OK";
        System.out.println("INVIO STRINGA " + requestString);
        byte[] request = requestString.getBytes();
        socket.send(request, 0);
        boolean expectReply = true;
        while (expectReply) {
            // Poll socket for a reply, with timeout
            ZMQ.Poller items = new ZMQ.Poller(1);
            items.register(socket, ZMQ.Poller.POLLIN);
            items.poll(REQUEST_TIMEOUT);
            if (items.pollin(0)) {
                final byte[] reply = socket.recv(0);
                final String replyString = new String(reply).trim();
                if (replyString.equals(risposta)) {
                    System.out.println("I: server replied: " + replyString);
                    retriesLeft = 0;
                    expectReply = false;
                    result = true;
                    socket.close();
                    items.unregister(socket);
                } else {
                    System.out.printf("E: malformed reply from server: (%s)\n",replyString);
                }
            } else {
                System.out.println("W: no response from server, retrying");
                // Old socket is confused; close it and open a new one
                socket.setLinger(0); // drop pending messages immediately
                socket.close();
                items.unregister(socket);
                if (--retriesLeft == 0) {
                    System.out.println("E: server seems to be offline, abandoning");
                    break;
                }
                System.out.println("I: reconnecting to server");
                socket = context.socket(ZMQ.REQ);
                socket.connect(SERVER_ENDPOINT);
                // Send request again, on new socket//
                socket.send(request, 0);
            }
        }
    }
    return result;
}