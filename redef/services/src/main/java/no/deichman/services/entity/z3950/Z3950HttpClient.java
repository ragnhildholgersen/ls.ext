package no.deichman.services.entity.z3950;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Responsibility: connect to z3950 proxy.
 */
public class Z3950HttpClient {

    private static final String Z3950_PROXY_PORT = System.getProperty("Z3950_ENDPOINT", "http://z3950proxy:3000");
    public static final String ISBN = "isbn";
    private static final String EAN = "ean";

    private String baseURI;
    private int proxyPort;

    public Z3950HttpClient() throws MalformedURLException {
        this(Z3950_PROXY_PORT);
    }

    Z3950HttpClient(String baseURI, int port) {
        this.baseURI = baseURI;
        this.proxyPort = port;
    }

    Z3950HttpClient(String url) throws MalformedURLException {
        URL resource = new URL(url);
        this.baseURI = resource.getProtocol() + "://" + resource.getHost();
        this.proxyPort = resource.getPort();
    }

    public final String getBaseURI() {
        return baseURI;
    }

    final int getProxyPort() {
        return proxyPort;
    }

    public final void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    final void setPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public final String getByIsbn(String targetString, String isbn) throws IOException {
        return doGet(targetString, isbn, ISBN);
    }

    public final String getByEan(String target, String ean) throws IOException {
        return doGet(target, ean, EAN);
    }

    private String doGet(String targetString, String searhTerm, String parameter) throws IOException {
        Target target = Target.valueOf(targetString.toUpperCase());
        String response = null;
        try (
                CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse httpResponse = httpClient.execute(new HttpGet(getURL(target, searhTerm, parameter)))
        ) {
            if (httpResponse.getStatusLine().getStatusCode() == Response.Status.OK.getStatusCode()) {
                HttpEntity httpEntity = httpResponse.getEntity();
                response = IOUtils.toString(httpEntity.getContent());
                EntityUtils.consume(httpEntity);
            }
        }
        return response;
    }

    private String getURL(Target target, String isbn, final String searchTerm) {
        return baseURI
                + ":" + proxyPort
                + "?apikey=somethingsomething"
                + "&base="
                + target.getDatabaseName()
                + "&" + searchTerm + "="
                + isbn
                + "&format="
                + target.getDataFormat();
    }
}
