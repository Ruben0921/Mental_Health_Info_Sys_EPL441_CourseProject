package com.skillonnet.automation;

import com.skillonnet.automation.api.MentalHealthApplication;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

/**
 * Entry point: loads {@code config.properties}, starts Grizzly HTTP with the Jersey application.
 */
public class Main {

    /**
     * Starts the REST server on {@code api.baseUri} (default {@code http://localhost:8080/}) and blocks.
     *
     * @param args unused
     * @throws Exception if the server fails to start
     */
    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        try (InputStream in = Main.class.getResourceAsStream("/config.properties")) {
            if (in != null) {
                p.load(in);
            }
        }
        String uri = p.getProperty("api.baseUri", "http://localhost:8080/");
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(uri), new MentalHealthApplication());
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
        server.start();
        Thread.currentThread().join();
    }
}
