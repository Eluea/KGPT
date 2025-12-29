package tn.eluea.kgpt.llm.internet;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import tn.eluea.kgpt.llm.service.InternetRequestListener;

public interface InternetProvider {
    InputStream sendRequest(HttpURLConnection con, String body, InternetRequestListener irl) throws IOException;
}
