/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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
package org.wildfly.security.http.client;

import org.junit.Assert;
import org.junit.Test;
import static org.wildfly.security.http.HttpConstants.CONFIG_REALM;

import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.AuthenticationContextConfigurationClient;
import org.wildfly.security.auth.client.ElytronXmlParser;
import org.wildfly.security.auth.client.InvalidAuthenticationConfigurationException;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.digest.WildFlyElytronDigestProvider;
import org.wildfly.security.http.HttpServerAuthenticationMechanism;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;
import org.wildfly.security.http.basic.BasicMechanismFactory;
import org.wildfly.security.http.digest.DigestMechanismFactory;
import org.wildfly.security.http.impl.AbstractBaseHttpTest;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;

import java.net.URL;
import java.net.http.HttpRequest;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Security;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.security.AccessController.doPrivileged;
import static org.wildfly.security.http.HttpConstants.FORBIDDEN;
import static org.wildfly.security.http.HttpConstants.UNAUTHORIZED;

/**
 * Test for the ElytronHttpClient class
 *
 * @author <a href="mailto:kekumar@redhat.com">Keshav Kumar</a>
 */
public class ElytronHttpClientTest extends AbstractBaseHttpTest {

    private static final String NAME = ElytronHttpClientTest.class.getSimpleName();

    public static Supplier<Provider[]> ELYTRON_PASSWORD_PROVIDERS = () -> new Provider[]{
            WildFlyElytronPasswordProvider.getInstance()
    };
    protected HttpServerAuthenticationMechanismFactory basicFactory = new BasicMechanismFactory(ELYTRON_PASSWORD_PROVIDERS.get());
    protected HttpServerAuthenticationMechanismFactory digestFactory = new DigestMechanismFactory(ELYTRON_PASSWORD_PROVIDERS.get());
    ElytronHttpClient elytronHttpClient = new ElytronHttpClient();

    @Test
    public void testElytonHttpClientBasicAuthenticationMechanism() {
        AuthenticationContextConfigurationClient AUTH_CONTEXT_CLIENT =
                doPrivileged((PrivilegedAction<AuthenticationContextConfigurationClient>) AuthenticationContextConfigurationClient::new);

        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-http-client-basic.xml");
                return ElytronXmlParser.parseAuthenticationClientConfiguration(config.toURI()).create();
            } catch (Throwable t) {
                throw new InvalidAuthenticationConfigurationException(t);
            }
        });
        context.run(() -> {
            try {
                HttpServerAuthenticationMechanism mechanism = basicFactory.createAuthenticationMechanism("BASIC", Collections.emptyMap(), getCallbackHandler("user1", "test-realm", "password1"));
                HttpRequest request = elytronHttpClient.getRequest("http://localhost:8080/servlet-security/SecuredServlet");

                //Test successful authentication
                TestingHttpServerRequest testingHttpServerRequest = new TestingHttpServerRequest(new String[]{request.headers().allValues("Authorization").get(0)});
                mechanism.evaluateRequest(testingHttpServerRequest);
                Assert.assertEquals(Status.COMPLETE, testingHttpServerRequest.getResult());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testElytonHttpClientBasicAuthenticationMechanismUnauthorizedUser() {
        AuthenticationConfiguration configuration = AuthenticationConfiguration.empty().useName("unauthorizedUser").usePassword("password").useHttpMechanism("BASIC");
        AuthenticationContext context = AuthenticationContext.empty();
        context = context.with(MatchRule.ALL,configuration);
        context.run(() -> {
            try {
                HttpServerAuthenticationMechanism mechanism = basicFactory.createAuthenticationMechanism("BASIC", Collections.emptyMap(), getCallbackHandler("unauthorizedUser", "test-realm", "password"));
                HttpRequest request = elytronHttpClient.getRequest("http://localhost:8080/servlet-security/SecuredServlet");

                //Test successful authentication
                TestingHttpServerRequest testingHttpServerRequest = new TestingHttpServerRequest(new String[]{request.headers().allValues("Authorization").get(0)});
                mechanism.evaluateRequest(testingHttpServerRequest);
                Assert.assertEquals(Status.FAILED, testingHttpServerRequest.getResult());
                Assert.assertEquals(FORBIDDEN,testingHttpServerRequest.getResponse().getStatusCode());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testElytonHttpClientBasicAuthenticationMechanismInvalidUser() {
        AuthenticationContextConfigurationClient AUTH_CONTEXT_CLIENT =
                doPrivileged((PrivilegedAction<AuthenticationContextConfigurationClient>) AuthenticationContextConfigurationClient::new);

        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-http-client-basic.xml");
                return ElytronXmlParser.parseAuthenticationClientConfiguration(config.toURI()).create();
            } catch (Throwable t) {
                throw new InvalidAuthenticationConfigurationException(t);
            }
        });
        context.run(() -> {
            try {
                HttpServerAuthenticationMechanism mechanism = basicFactory.createAuthenticationMechanism("BASIC", Collections.emptyMap(), getCallbackHandler("user1", "test-realm", "password"));
                HttpRequest request = elytronHttpClient.getRequest("http://localhost:8080/servlet-security/SecuredServlet");

                //Test successful authentication
                TestingHttpServerRequest testingHttpServerRequest = new TestingHttpServerRequest(new String[]{request.headers().allValues("Authorization").get(0)});
                mechanism.evaluateRequest(testingHttpServerRequest);
                Assert.assertEquals(Status.FAILED, testingHttpServerRequest.getResult());
                Assert.assertEquals(UNAUTHORIZED,testingHttpServerRequest.getResponse().getStatusCode());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testElytonHttpClientDigestAuthenticationMechanism() {
        AuthenticationContextConfigurationClient AUTH_CONTEXT_CLIENT =
                doPrivileged((PrivilegedAction<AuthenticationContextConfigurationClient>) AuthenticationContextConfigurationClient::new);

        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-http-client-basic.xml");
                return ElytronXmlParser.parseAuthenticationClientConfiguration(config.toURI()).create();
            } catch (Throwable t) {
                throw new InvalidAuthenticationConfigurationException(t);
            }
        });
        context.run(() -> {
            try {
                Map<String, Object> props = new HashMap<>();
                props.put(CONFIG_REALM, "RealmUsersRoles");
                props.put("org.wildfly.security.http.validate-digest-uri", "false");
                String uri = "http://localhost:8080/hello";
                HttpServerAuthenticationMechanism mechanism = digestFactory.createAuthenticationMechanism("DIGEST", props,getCallbackHandler("user1", "RealmUsersRoles", "password1"));
                TestingHttpServerRequest request1 = new TestingHttpServerRequest(null);
                mechanism.evaluateRequest(request1);
                TestingHttpServerResponse response = request1.getResponse();
                HttpRequest request2 = elytronHttpClient.getResponseHeader(response.getAuthenticateHeader(), uri);

                //Test successful authentication
                TestingHttpServerRequest testingHttpServerRequest = new TestingHttpServerRequest(new String[]{request2.headers().allValues("Authorization").get(0)});
                mechanism.evaluateRequest(testingHttpServerRequest);
                Assert.assertEquals(Status.COMPLETE,testingHttpServerRequest.getResult());
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testElytonHttpClientDigestAuthenticationMechanismUnauthorizedUser() {
        AuthenticationConfiguration configuration = AuthenticationConfiguration.empty().useName("unauthorizedUser").usePassword("password").useHttpMechanism("BASIC");
        AuthenticationContext context = AuthenticationContext.empty();
        context = context.with(MatchRule.ALL,configuration);
        context.run(() -> {
            try {
                Map<String, Object> props = new HashMap<>();
                props.put(CONFIG_REALM, "RealmUsersRoles");
                props.put("org.wildfly.security.http.validate-digest-uri", "false");
                String uri = "http://localhost:8080/hello";
                HttpServerAuthenticationMechanism mechanism = digestFactory.createAuthenticationMechanism("DIGEST", props,getCallbackHandler("unauthorizedUser", "RealmUsersRoles", "password"));
                TestingHttpServerRequest request1 = new TestingHttpServerRequest(null);
                mechanism.evaluateRequest(request1);
                TestingHttpServerResponse response = request1.getResponse();
                HttpRequest request2 = elytronHttpClient.getResponseHeader(response.getAuthenticateHeader(), uri);

                //Test successful authentication
                TestingHttpServerRequest testingHttpServerRequest = new TestingHttpServerRequest(new String[]{request2.headers().allValues("Authorization").get(0)});
                mechanism.evaluateRequest(testingHttpServerRequest);
                Assert.assertEquals(Status.FAILED,testingHttpServerRequest.getResult());
                Assert.assertEquals(FORBIDDEN,testingHttpServerRequest.getResponse().getStatusCode());
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testElytonHttpClientDigestAuthenticationMechanismInvalidUser() {
        AuthenticationContextConfigurationClient AUTH_CONTEXT_CLIENT =
                doPrivileged((PrivilegedAction<AuthenticationContextConfigurationClient>) AuthenticationContextConfigurationClient::new);

        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-http-client-basic.xml");
                return ElytronXmlParser.parseAuthenticationClientConfiguration(config.toURI()).create();
            } catch (Throwable t) {
                throw new InvalidAuthenticationConfigurationException(t);
            }
        });
        context.run(() -> {
            try {
                Map<String, Object> props = new HashMap<>();
                props.put(CONFIG_REALM, "RealmUsersRoles");
                props.put("org.wildfly.security.http.validate-digest-uri", "false");
                String uri = "http://localhost:8080/hello";
                HttpServerAuthenticationMechanism mechanism = digestFactory.createAuthenticationMechanism("DIGEST", props,getCallbackHandler("user1", "RealmUsersRoles", "password"));
                TestingHttpServerRequest request1 = new TestingHttpServerRequest(null);
                mechanism.evaluateRequest(request1);
                TestingHttpServerResponse response = request1.getResponse();
                HttpRequest request2 = elytronHttpClient.getResponseHeader(response.getAuthenticateHeader(), uri);

                //Test successful authentication
                TestingHttpServerRequest testingHttpServerRequest = new TestingHttpServerRequest(new String[]{request2.headers().allValues("Authorization").get(0)});
                mechanism.evaluateRequest(testingHttpServerRequest);
                Assert.assertEquals(Status.FAILED,testingHttpServerRequest.getResult());
                Assert.assertEquals(UNAUTHORIZED,testingHttpServerRequest.getResponse().getStatusCode());
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testElytonHttpClientDigestAuthenticationMechanismSHA_256Algorithm() {
        AuthenticationContextConfigurationClient AUTH_CONTEXT_CLIENT =
                doPrivileged((PrivilegedAction<AuthenticationContextConfigurationClient>) AuthenticationContextConfigurationClient::new);

        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-http-client-basic.xml");
                return ElytronXmlParser.parseAuthenticationClientConfiguration(config.toURI()).create();
            } catch (Throwable t) {
                throw new InvalidAuthenticationConfigurationException(t);
            }
        });
        context.run(() -> {
            try {
                Map<String, Object> props = new HashMap<>();
                props.put(CONFIG_REALM, "RealmUsersRoles");
                props.put("org.wildfly.security.http.validate-digest-uri", "false");
                String uri = "http://localhost:8080/hello";
                HttpServerAuthenticationMechanism mechanism = digestFactory.createAuthenticationMechanism("DIGEST-SHA-256", props,getCallbackHandler("user1", "RealmUsersRoles", "password1"));
                TestingHttpServerRequest request1 = new TestingHttpServerRequest(null);
                mechanism.evaluateRequest(request1);
                TestingHttpServerResponse response = request1.getResponse();
                HttpRequest request2 = elytronHttpClient.getResponseHeader(response.getAuthenticateHeader(), uri);

                //Test successful authentication
                TestingHttpServerRequest testingHttpServerRequest = new TestingHttpServerRequest(new String[]{request2.headers().allValues("Authorization").get(0)});
                mechanism.evaluateRequest(testingHttpServerRequest);
                Assert.assertEquals(Status.COMPLETE,testingHttpServerRequest.getResult());
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testElytonHttpClientDigestAuthenticationMechanismSHA_512_256Algorithm() {
        AuthenticationContextConfigurationClient AUTH_CONTEXT_CLIENT =
                doPrivileged((PrivilegedAction<AuthenticationContextConfigurationClient>) AuthenticationContextConfigurationClient::new);

        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-http-client-basic.xml");
                return ElytronXmlParser.parseAuthenticationClientConfiguration(config.toURI()).create();
            } catch (Throwable t) {
                throw new InvalidAuthenticationConfigurationException(t);
            }
        });
        context.run(() -> {
            try {
                Map<String, Object> props = new HashMap<>();
                props.put(CONFIG_REALM, "RealmUsersRoles");
                props.put("org.wildfly.security.http.validate-digest-uri", "false");
                String uri = "http://localhost:8080/hello";
                Security.addProvider(new WildFlyElytronDigestProvider());
                HttpServerAuthenticationMechanism mechanism = digestFactory.createAuthenticationMechanism("DIGEST-SHA-512-256", props,getCallbackHandler("user1", "RealmUsersRoles", "password1"));
                TestingHttpServerRequest request1 = new TestingHttpServerRequest(null);
                mechanism.evaluateRequest(request1);
                TestingHttpServerResponse response = request1.getResponse();
                HttpRequest request2 = elytronHttpClient.getResponseHeader(response.getAuthenticateHeader(), uri);

                //Test successful authentication
                TestingHttpServerRequest testingHttpServerRequest = new TestingHttpServerRequest(new String[]{request2.headers().allValues("Authorization").get(0)});
                mechanism.evaluateRequest(testingHttpServerRequest);
                Assert.assertEquals(Status.COMPLETE,testingHttpServerRequest.getResult());
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        });
    }
}