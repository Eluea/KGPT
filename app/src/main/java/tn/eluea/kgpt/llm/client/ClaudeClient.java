package tn.eluea.kgpt.llm.client;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.reactivestreams.Publisher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import tn.eluea.kgpt.MainHook;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.llm.publisher.ExceptionPublisher;
import tn.eluea.kgpt.llm.publisher.InputStreamPublisher;

public class ClaudeClient extends ChatGPTClient {
    @Override
    public LanguageModel getLanguageModel() {
        return LanguageModel.Claude;
    }
}
