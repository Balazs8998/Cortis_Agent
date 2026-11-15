package dev.rehm.lingua.client;

public interface ClientInterface<REQ, RES> {

    RES sendMessageToClient(REQ request);
}
