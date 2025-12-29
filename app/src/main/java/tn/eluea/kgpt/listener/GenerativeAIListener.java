package tn.eluea.kgpt.listener;

public interface GenerativeAIListener {
    void onAIPrepare();
    void onAINext(String chunk);
    void onAIError(Throwable t);
    void onAIComplete();
}
