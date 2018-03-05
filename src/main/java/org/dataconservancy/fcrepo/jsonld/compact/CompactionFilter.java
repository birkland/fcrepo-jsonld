/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataconservancy.fcrepo.jsonld.compact;

import static org.dataconservancy.fcrepo.jsonld.compact.ConfigUtil.getValue;
import static org.dataconservancy.fcrepo.jsonld.compact.JsonldUtil.loadContexts;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.core.JsonLdOptions;

/**
 * Servlet filter which compacts responses according to
 *
 * @author apb@jhu.edu
 */
public class CompactionFilter implements Filter {

    public static final String CONTEXT_COMPACTION_URI_PROP = "compaction.uri";

    private URL defaultContext;

    private final Compactor compactor = new Compactor();

    Logger LOG = LoggerFactory.getLogger(CompactionFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        final String context = Optional.ofNullable(filterConfig.getInitParameter("context")).orElse(getValue(
                CONTEXT_COMPACTION_URI_PROP));

        if (context != null) {
            try {
                defaultContext = new URL(context);
            } catch (final MalformedURLException e) {
                throw new ServletException(String.format("Bad context URL '%s'", context), e);
            }
        }

        final JsonLdOptions options = new JsonLdOptions();

        loadContexts(options);

        compactor.setOptions(options);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        chain.doFilter(request, new CompactionWrapper((HttpServletResponse) response, compactor, defaultContext));
    }

    @Override
    public void destroy() {
        // nothing
    }
}