//
//  ========================================================================
//  Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.demo;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpRequest;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ByteCountingTest
{
    private static final Logger LOG = Log.getLogger(ByteCountingTest.class);

    private Server server;
    private HttpClient client;
    private ByteCountingEventListener byteCountingEvents;

    @BeforeEach
    public void startAll() throws Exception
    {
        server = new Server();

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(0);
        server.addConnector(connector);

        byteCountingEvents = new ByteCountingEventListener();
        ByteCountingHandler byteCountingHandler = new ByteCountingHandler(byteCountingEvents);

        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath("/");
        contextHandler.addServlet(DemoServlet.class, "/demo/*");

        byteCountingHandler.setHandler(contextHandler);
        server.setHandler(byteCountingHandler);
        server.start();

        client = new HttpClient();
        client.start();
    }

    @AfterEach
    public void stopAll()
    {
        LifeCycle.stop(client);
        LifeCycle.stop(server);
    }

    @Test
    public void testGetResponseSize() throws InterruptedException, ExecutionException, TimeoutException
    {
        URI uri = server.getURI().resolve("/demo/");
        HttpRequest request = (HttpRequest)client.newRequest(uri).method(HttpMethod.GET);
        ContentResponse response = request.send();
        assertThat("response status code", response.getStatus(), is(HttpStatus.OK_200));

        String reqID = ByteCountingEventListener.toRequestID(request);
        Long actualCount = byteCountingEvents.responseCounts.get(reqID);
        assertThat("Response byte count", actualCount, is(20L));
    }

    @Test
    public void testPostRequestAndResponseSize() throws InterruptedException, ExecutionException, TimeoutException
    {
        URI uri = server.getURI().resolve("/demo/");
        Fields fields = new Fields();
        fields.put("foo", "bar");
        fields.put("id", "form");
        FormContentProvider formFields = new FormContentProvider(fields);
        HttpRequest request = (HttpRequest)client.newRequest(uri)
            .method(HttpMethod.POST)
            .content(formFields);
        ContentResponse response = request.send();
        assertThat("response status code", response.getStatus(), is(HttpStatus.OK_200));

        String reqID = ByteCountingEventListener.toRequestID(request);

        Long actualRequestCount = byteCountingEvents.requestCounts.get(reqID);
        // Example request body "foo=bar&id=form";
        assertThat("Request byte count", actualRequestCount, is(15L));

        Long actualResponseCount = byteCountingEvents.responseCounts.get(reqID);
        assertThat("Response byte count", actualResponseCount, is(38L));
    }

    public static class ByteCountingEventListener implements ByteCountingListener
    {
        private static final Logger LOG = Log.getLogger(ByteCountingEventListener.class);
        private final Map<String, Long> requestCounts = new HashMap<>();
        private final Map<String, Long> responseCounts = new HashMap<>();

        @Override
        public void onResponseByteCount(Request request, Response response, long byteCount)
        {
            LOG.info("onResponseByteCount({}, {}, {})", request, response, byteCount);
            responseCounts.put(toRequestID(request), byteCount);
        }

        @Override
        public void onRequestByteCount(Request request, long byteCount)
        {
            LOG.info("onRequestByteCount({}, {})", request, byteCount);
            requestCounts.put(toRequestID(request), byteCount);
        }

        public static String toRequestID(HttpRequest request)
        {
            return String.format("[%s]%s", request.getMethod(), request.getPath());
        }

        private static String toRequestID(Request request)
        {
            return String.format("[%s]%s", request.getMethod(), request.getHttpURI().getPath());
        }
    }

    public static class DemoServlet extends HttpServlet
    {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
        {
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("utf-8");
            resp.getWriter().println("Simple get response");
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
        {
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("utf-8");
            resp.getWriter().println("Simple POST response for " + req.getParameterMap().size() + " parameters");
        }
    }
}
