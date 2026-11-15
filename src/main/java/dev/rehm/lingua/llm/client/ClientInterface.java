package dev.rehm.lingua.llm.client;

public interface ClientInterface<REQ, RES> {

    RES sendMessageToClient(REQ request);
}
