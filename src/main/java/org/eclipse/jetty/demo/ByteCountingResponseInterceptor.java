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

import java.nio.ByteBuffer;

import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class ByteCountingResponseInterceptor implements HttpOutput.Interceptor
{
    private static final Logger LOG = Log.getLogger(ByteCountingResponseInterceptor.class);
    private final ByteCountingListener listener;
    private final Request request;
    private final Response response;
    private final HttpOutput.Interceptor next;
    private long byteCount;

    public ByteCountingResponseInterceptor(ByteCountingListener listener, Request request, Response response, HttpOutput.Interceptor next)
    {
        this.listener = listener;
        this.request = request;
        this.response = response;
        this.next = next;
        this.byteCount = 0;
    }

    @Override
    public void write(ByteBuffer content, boolean last, Callback callback)
    {
        LOG.debug("write({}, {}, {})", content, last, callback);
        byteCount += content.remaining();
        if (last)
        {
            listener.onResponseByteCount(request, response, byteCount);
        }
        next.write(content, last, callback);
    }

    @Override
    public HttpOutput.Interceptor getNextInterceptor()
    {
        return next;
    }

    @Override
    public boolean isOptimizedForDirectBuffers()
    {
        return false;
    }
}
