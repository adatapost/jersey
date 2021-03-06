/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jersey.server;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.Suspended;

import javax.inject.Singleton;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests {@link CompletionCallback}.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class AsyncCallbackServerTest {
    public static boolean onResumeCalled;
    public static boolean onCompletionCalled;
    public static boolean onCompletionCalledWithError;
    public static boolean onResumeFailedCalled;

    @Before
    public void reset() {
        onResumeCalled = false;
        onCompletionCalled = false;
        onCompletionCalledWithError = false;
        onResumeFailedCalled = false;
    }

    @Test
    public void testCompletionCallback() throws ExecutionException, InterruptedException {
        ApplicationHandler app = new ApplicationHandler(new ResourceConfig(CompletionResource.class,
                CheckingCompletionFilter.class));
        ContainerRequest req = RequestContextBuilder.from(
                "/completion/onCompletion", "GET").build();

        final ContainerResponse response = app.apply(req).get();
        assertEquals(200, response.getStatus());
        assertTrue("onComplete() was not called.", onCompletionCalled);
    }

    @Test
    public void testCompletionFail() throws ExecutionException, InterruptedException {
        ApplicationHandler app = new ApplicationHandler(new ResourceConfig(CompletionResource.class,
                CheckingCompletionFilter.class));

        try {
            final ContainerResponse response = app.apply(RequestContextBuilder.from(
                    "/completion/onError", "GET").build()).get();
            fail("should fail");
        } catch (Exception e) {
            // ok - should throw an exception
        }
        assertTrue("onError().", onCompletionCalledWithError);
    }

    @Test
    public void testRegisterNullClass() throws ExecutionException, InterruptedException {
        final ApplicationHandler app = new ApplicationHandler(new ResourceConfig(NullCallbackResource.class));
        final ContainerRequest req = RequestContextBuilder.from("/null-callback/class", "GET").build();

        final ContainerResponse response = app.apply(req).get();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testRegisterNullObject() throws ExecutionException, InterruptedException {
        final ApplicationHandler app = new ApplicationHandler(new ResourceConfig(NullCallbackResource.class));
        final ContainerRequest req = RequestContextBuilder.from("/null-callback/object", "GET").build();

        final ContainerResponse response = app.apply(req).get();
        assertEquals(200, response.getStatus());
    }

    @CompletionBinding
    public static class CheckingCompletionFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            assertFalse("onComplete() callback has already been called.",
                    onCompletionCalled);
        }
    }


    public static class MyCompletionCallback implements CompletionCallback {
        @Override
        public void onComplete(Throwable throwable) {
            assertFalse("onComplete() has already been called.", onCompletionCalled);
            assertFalse("onComplete() has already been called with error.", onCompletionCalledWithError);
            if (throwable == null) {
                onCompletionCalled = true;
            } else {
                onCompletionCalledWithError = true;
            }
        }
    }

    @Path("completion")
    @Singleton
    public static class CompletionResource {
        @GET
        @Path("onCompletion")
        @CompletionBinding
        public void onComplete(@Suspended AsyncResponse asyncResponse) {
            assertFalse(onCompletionCalled);
            asyncResponse.register(MyCompletionCallback.class);
            asyncResponse.resume("ok");
            assertTrue(onCompletionCalled);
        }

        @GET
        @Path("onError")
        @CompletionBinding
        public void onError(@Suspended AsyncResponse asyncResponse) {
            assertFalse(onCompletionCalledWithError);
            asyncResponse.register(MyCompletionCallback.class);
            asyncResponse.resume(new RuntimeException("test-exception"));
            assertTrue(onCompletionCalledWithError);
        }
    }

    @Path("null-callback")
    @Singleton
    public static class NullCallbackResource {

        @GET
        @Path("class")
        @CompletionBinding
        public void registerClass(@Suspended AsyncResponse asyncResponse) {
            try {
                asyncResponse.register(null);
                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // Expected.
            }

            try {
                asyncResponse.register(null, MyCompletionCallback.class);
                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // Expected.
            }

            try {
                asyncResponse.register(MyCompletionCallback.class, null);
                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // Expected.
            }

            try {
                asyncResponse.register(MyCompletionCallback.class, MyCompletionCallback.class, null);
                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // Expected.
            }

            asyncResponse.resume("ok");
        }

        @GET
        @Path("object")
        @CompletionBinding
        public void registerObject(@Suspended AsyncResponse asyncResponse) {
            try {
                asyncResponse.register((Object) null);
                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // Expected.
            }

            try {
                asyncResponse.register(null, new MyCompletionCallback());
                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // Expected.
            }

            try {
                asyncResponse.register(new MyCompletionCallback(), null);
                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // Expected.
            }

            try {
                asyncResponse.register(new MyCompletionCallback(), new MyCompletionCallback(), null);
                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // Expected.
            }

            asyncResponse.resume("ok");
        }
    }

    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(value = RetentionPolicy.RUNTIME)
    public @interface CompletionBinding {
    }
}
