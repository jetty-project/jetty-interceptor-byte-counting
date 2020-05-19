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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.HandlerWrapper;

public class ByteCountingHandler extends HandlerWrapper
{
    private final ByteCountingListener listener;

    public ByteCountingHandler(ByteCountingListener listener)
    {
        this.listener = listener;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        Response baseResponse = baseRequest.getResponse();

        HttpInput httpInput = baseRequest.getHttpInput();
        ByteCountingRequestInterceptor requestInterceptor = new ByteCountingRequestInterceptor(listener, baseRequest);
        httpInput.addInterceptor(requestInterceptor);

        HttpOutput httpOutput = baseResponse.getHttpOutput();
        HttpOutput.Interceptor nextOutputInterceptor = httpOutput.getInterceptor();
        ByteCountingResponseInterceptor responseInterceptor = new ByteCountingResponseInterceptor(listener, baseRequest, baseResponse, nextOutputInterceptor);
        httpOutput.setInterceptor(responseInterceptor);

        super.handle(target, baseRequest, request, response);
    }
}
