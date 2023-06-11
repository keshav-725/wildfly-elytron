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
package org.wildfly.security.http.client.utils;

import org.wildfly.security.auth.callback.CredentialCallback;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.AuthenticationContextConfigurationClient;
import org.wildfly.security.credential.BearerTokenCredential;
import org.wildfly.security.http.client.exception.ElytronHttpClientException;

import static org.wildfly.security.http.client.utils.ElytronMessages.log;

import javax.net.ssl.SSLContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.net.URI;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.PrivilegedAction;

/**
 * WebServices client provider implementation.
 *
 * @author <a href="mailto:kekumar@redhat.com">Keshav Kumar</a>
 */
public class HttpMechClientConfigUtil {

    static final AuthenticationContextConfigurationClient AUTH_CONTEXT_CLIENT = AccessController.doPrivileged((PrivilegedAction<AuthenticationContextConfigurationClient>) AuthenticationContextConfigurationClient::new);

    public static String getUsername(URI uri) throws ElytronHttpClientException {
        final CallbackHandler callbackHandler = AUTH_CONTEXT_CLIENT.getCallbackHandler(AUTH_CONTEXT_CLIENT.getAuthenticationConfiguration(uri, AuthenticationContext.captureCurrent()));
        NameCallback nameCallback = new NameCallback("user name");
        try {
            callbackHandler.handle(new Callback[]{nameCallback});
            return nameCallback.getName();
        } catch (IOException | UnsupportedCallbackException e) {
            throw new ElytronHttpClientException(log.nameCallBackHandlingFailed());
        }
    }

    public static String getPassword(URI uri) throws ElytronHttpClientException {
        final CallbackHandler callbackHandler = AUTH_CONTEXT_CLIENT.getCallbackHandler(AUTH_CONTEXT_CLIENT.getAuthenticationConfiguration(uri, AuthenticationContext.captureCurrent()));
        PasswordCallback passwordCallback = new PasswordCallback("password", false);
        try {
            callbackHandler.handle(new Callback[]{passwordCallback});
            char[] password = passwordCallback.getPassword();
            if (password == null) {
                return null;
            }
            return new String(password);
        } catch (IOException | UnsupportedCallbackException e) {
            throw new ElytronHttpClientException(log.passwordCallBackHandlingFailed());
        }
    }

    public static String getHttpAuthenticationType(URI uri) throws ElytronHttpClientException {
        return AUTH_CONTEXT_CLIENT.getHttpMechanismType(AUTH_CONTEXT_CLIENT.getAuthenticationConfiguration(uri, AuthenticationContext.captureCurrent()));
    }

    public static String getToken(URI uri) {
        final CallbackHandler callbackHandler = AUTH_CONTEXT_CLIENT.getCallbackHandler(AUTH_CONTEXT_CLIENT.getAuthenticationConfiguration(uri, AuthenticationContext.captureCurrent()));
        final CredentialCallback credentialCallback = new CredentialCallback(BearerTokenCredential.class);
        try {
            callbackHandler.handle(new Callback[]{credentialCallback});
            BearerTokenCredential token = credentialCallback.getCredential(BearerTokenCredential.class);
            if (token == null) {
                return null;
            }
            return token.getToken();
        } catch (IOException | UnsupportedCallbackException e) {
            throw new ElytronHttpClientException(log.credentialCallbackHandlingFailed());
        }
    }

    public static SSLContext getSSLContext(URI uri) {
        try {
            return AUTH_CONTEXT_CLIENT.getSSLContext(uri, AuthenticationContext.captureCurrent());
        } catch (GeneralSecurityException e) {
            throw new ElytronHttpClientException(log.unableToObtainSslContext());
        }
    }
}
