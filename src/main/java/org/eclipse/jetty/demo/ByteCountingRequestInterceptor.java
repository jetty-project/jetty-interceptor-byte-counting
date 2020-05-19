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

import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class ByteCountingRequestInterceptor implements HttpInput.Interceptor
{
    private static final Logger LOG = Log.getLogger(ByteCountingRequestInterceptor.class);
    private final ByteCountingListener listener;
    private final Request request;
    private long byteCount;

    public ByteCountingRequestInterceptor(ByteCountingListener listener, Request request)
    {
        this.listener = listener;
        this.request = request;
    }

    @Override
    public HttpInput.Content readFrom(HttpInput.Content content)
    {
        LOG.debug("readFrom({})", content);
        if (content instanceof HttpInput.EofContent)
        {
            // this is EOF on the input
            listener.onRequestByteCount(request, byteCount);
        }

        byteCount += content.remaining();
        return new HttpInput.Content(content.getByteBuffer());
    }
}
