/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.validation;

import java.net.URI;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import javax.validation.constraints.NotNull;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.validation.ValidationFeature;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Test various combinations of enabling/disabling: auto-discovery, bean validation, validation feature.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class PropertyValidationTest {

    @Path("/")
    public static class Resource {

        @POST
        @NotNull
        public String post(@NotNull final String value) {
            return value.isEmpty() ? null : value;
        }
    }

    @Test
    public void testDefaultValidationDefaultDiscoveryFeature() throws Exception {
        _test(500, null, null, true);
    }

    @Test
    public void testDefaultValidationDefaultDiscoveryNoFeature() throws Exception {
        _test(500, null, null, false);
    }

    @Test
    public void testDefaultValidationDiscoveryFeature() throws Exception {
        _test(500, null, false, true);
    }

    @Test
    public void testDefaultValidationDiscoveryNoFeature() throws Exception {
        _test(500, null, false, false);
    }

    @Test
    public void testDefaultValidationNoDiscoveryFeature() throws Exception {
        _test(500, null, true, true);
    }

    @Test
    public void testDefaultValidationNoDiscoveryNoFeature() throws Exception {
        _test(204, null, true, false);
    }

    @Test
    public void testValidationDefaultDiscoveryFeature() throws Exception {
        _test(500, false, null, true);
    }

    @Test
    public void testValidationDefaultDiscoveryNoFeature() throws Exception {
        _test(500, false, null, false);
    }

    @Test
    public void testValidationDiscoveryFeature() throws Exception {
        _test(500, false, false, true);
    }

    @Test
    public void testValidationDiscoveryNoFeature() throws Exception {
        _test(500, false, false, false);
    }

    @Test
    public void testValidationNoDiscoveryFeature() throws Exception {
        _test(500, false, true, true);
    }

    @Test
    public void testValidationNoDiscoveryNoFeature() throws Exception {
        _test(204, false, true, false);
    }

    @Test
    public void testNoValidationDefaultDiscoveryFeature() throws Exception {
        _test(204, true, null, true);
    }

    @Test
    public void testNoValidationDefaultDiscoveryNoFeature() throws Exception {
        _test(204, true, null, false);
    }

    @Test
    public void testNoValidationDiscoveryFeature() throws Exception {
        _test(204, true, false, true);
    }

    @Test
    public void testNoValidationDiscoveryNoFeature() throws Exception {
        _test(204, true, false, false);
    }

    @Test
    public void testNoValidationNoDiscoveryFeature() throws Exception {
        _test(204, true, true, true);
    }

    @Test
    public void testNoValidationNoDiscoveryNoFeature() throws Exception {
        _test(204, true, true, false);
    }

    private void _test(final int responseStatus, final Boolean disableValidation,
                       final Boolean disableAutoDiscovery, final boolean registerFeature) throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig(Resource.class).register(LoggingFilter.class);

        if (registerFeature) {
            resourceConfig.register(ValidationFeature.class);
        }
        if (disableAutoDiscovery != null) {
            resourceConfig.property(ServerProperties.FEATURE_DISABLE_AUTO_DISCOVERY, disableAutoDiscovery);
        }
        if (disableValidation != null) {
            resourceConfig.property(ServerProperties.FEATURE_DISABLE_BEAN_VALIDATION, disableValidation);
        }

        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final URI uri = URI.create("/");
        final ContainerRequest requestContext = new ContainerRequest(uri, uri, "POST", null, new MapPropertiesDelegate());

        final ContainerResponse containerResponse = applicationHandler
                .apply(requestContext).get();

        assertEquals(responseStatus, containerResponse.getStatus());
    }
}
