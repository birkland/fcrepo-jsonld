/*
 * Copyright 2017 Johns Hopkins University
 *
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

package org.dataconservancy.fcrepo.jsonld.integration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.dataconservancy.fcrepo.jsonld.compact.JsonldTestUtil.getUncompactedJsonld;
import static org.dataconservancy.fcrepo.jsonld.compact.JsonldTestUtil.isCompact;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.Callable;

import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoClient.FcrepoClientBuilder;
import org.fcrepo.client.FcrepoResponse;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 * @author apb@jhu.edu
 */
public class CompactionIT {

    static final URI SERVER_MANAGED = URI.create("http://fedora.info/definitions/v4/repository#ServerManaged");

    String fcrepoBaseURI = String.format("http://localhost:%s/%s/rest/", System.getProperty(
            "fcrepo.dynamic.test.port", "8080"), System.getProperty("fcrepo.cxtPath", "fcrepo"));

    @Test
    public void CompactionTest() throws Exception {
        final FcrepoClient client = new FcrepoClientBuilder().build();

        final URI jsonldResource = attempt(60, () -> {
            try (FcrepoResponse response = client
                    .post(URI.create(fcrepoBaseURI))
                    .body(toInputStream(getUncompactedJsonld(), UTF_8), "application/ld+json")
                    .perform()) {
                return response.getLocation();
            }
        });

        try (FcrepoResponse response = client
                .get(jsonldResource)
                .preferRepresentation(emptyList(), Arrays.asList(SERVER_MANAGED))
                .accept("application/ld+json")
                .perform()) {

            final String body = IOUtils.toString(response.getBody(), UTF_8);

            assertTrue(isCompact(body));
        }

    }

    static <T> T attempt(final int times, final Callable<T> it) {

        Throwable caught = null;

        for (int tries = 0; tries < times; tries++) {
            try {
                return it.call();
            } catch (final Throwable e) {
                caught = e;
                try {
                    Thread.sleep(1000);
                    System.out.println(".");
                } catch (final InterruptedException i) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        throw new RuntimeException("Failed executing task", caught);
    }

}
