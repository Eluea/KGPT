package tn.eluea.kgpt.llm.service;

public interface InternetRequestListener {
    void onRequestStatusCode(int code);
    void onRequestComplete();
}
