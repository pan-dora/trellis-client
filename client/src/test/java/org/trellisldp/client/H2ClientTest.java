/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.trellisldp.client;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static org.apache.jena.riot.WebContent.contentTypeJSONLD;
import static org.apache.jena.riot.WebContent.contentTypeNTriples;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.dropwizard.testing.DropwizardTestSupport;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLContext;

import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.jena.JenaRDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.trellisldp.app.config.TrellisConfiguration;
import org.trellisldp.app.triplestore.TrellisApplication;


/**
 * H2ClientTest.
 *
 * @author christopher-johnson
 */
public class H2ClientTest {
    private static final DropwizardTestSupport<TrellisConfiguration> APP = new DropwizardTestSupport<>(
            TrellisApplication.class, resourceFilePath("trellis-config.yml"), config("server"
                    + ".applicationConnectors[1].port", "8446"), config("binaries", resourceFilePath("data")
                    + "/binaries"), config("mementos", resourceFilePath("data") + "/mementos"), config("namespaces",
                    resourceFilePath("data/namespaces.json")), config("server.applicationConnectors[1].keyStorePath",
                    resourceFilePath("keystore/trellis.jks")));
    private static final JenaRDF rdf = new JenaRDF();
    private static String baseUrl;
    private static String pid;
    private static LdpClient h2client = null;

    @BeforeAll
    static void initAll() {
        APP.before();
        baseUrl = "https://localhost:8446/";
        try {
            final SimpleSSLContext sslct = new SimpleSSLContext();
            final SSLContext sslContext = sslct.get();
            h2client = new LdpClientImpl(sslContext);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterAll
    static void tearDownAll() {
        APP.after();
    }

    @BeforeEach
    void init() {
        pid = "ldp-test-" + UUID.randomUUID().toString();
    }

    @AfterEach
    void tearDown() {
    }

    private static InputStream getTestJsonResource() {
        return LdpClientTest.class.getResourceAsStream("/webanno.complete-embedded.json");
    }

    private static InputStream getTestN3Resource() {
        return LdpClientTest.class.getResourceAsStream("/webanno.complete.nt");
    }

    @RepeatedTest(200)
    void testRepeatedPutH2N3Resource() throws Exception {
        try {
            final IRI identifier = rdf.createIRI(baseUrl + pid);
            h2client.put(identifier, getTestN3Resource(), contentTypeNTriples);
            final Map<String, List<String>> headers = h2client.head(identifier);
            assertTrue(headers.containsKey(LINK));
        } catch (Exception ex) {
            throw new LdpClientException(ex.toString(), ex.getCause());
        }
    }

    @RepeatedTest(200)
    void testRepeatedPutH2JsonResource() throws Exception {
        try {
            final IRI identifier = rdf.createIRI(baseUrl + pid);
            h2client.put(identifier, getTestJsonResource(), contentTypeJSONLD);
            final Map<String, List<String>> headers = h2client.head(identifier);
            assertTrue(headers.containsKey(LINK));
        } catch (Exception ex) {
            throw new LdpClientException(ex.toString(), ex.getCause());
        }
    }

    @Test
    void testNonBlockingAsyncGet() throws Exception {
        try {
            final IRI identifier = rdf.createIRI(baseUrl + pid);
            final Map<HttpRequest, CompletableFuture<HttpResponse<String>>> results = h2client.multiSubscriberAsyncGet(
                    identifier);
        } catch (Exception ex) {
            throw new LdpClientException(ex.toString(), ex.getCause());
        }
    }

    @Test
    void testJoiningCompletableFuturePut() throws Exception {
        try {
            final Map<URI, InputStream> map = new HashMap<>();
            final int LOOPS = 400;
            for (int i = 0; i < LOOPS; i++) {
                pid = "ldp-test-" + UUID.randomUUID().toString();
                final IRI identifier = rdf.createIRI(baseUrl + pid);
                final URI uri = new URI(identifier.getIRIString());
                final InputStream is = getTestN3Resource();
                map.put(uri, is);
            }
            h2client.joiningCompletableFuturePut(map, contentTypeNTriples);
        } catch (Exception ex) {
            throw new LdpClientException(ex.toString(), ex.getCause());
        }
    }

}