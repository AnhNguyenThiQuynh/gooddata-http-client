/*
 * Copyright (C) 2007-2015, GoodData(R) Corporation. All rights reserved.
 * This program is made available under the terms of the BSD License.
 */
package com.gooddata.http.client;

import static com.gooddata.http.client.GoodDataHttpClient.SST_HEADER;
import static com.gooddata.http.client.GoodDataHttpClient.TT_HEADER;
import static com.gooddata.http.client.GoodDataHttpClient.YAML_CONTENT_TYPE;
import static java.lang.String.format;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * This strategy obtains super-secure token via login and password.
 */
public class LoginSSTRetrievalStrategy implements SSTRetrievalStrategy {

    private static final String X_GDC_REQUEST_HEADER_NAME = "X-GDC-REQUEST";

    public static final String LOGIN_URL = "/gdc/account/login";

    /** SST and TT must be present in the HTTP header. */
    private static final int VERIFICATION_LEVEL = 2;

    private Log log = LogFactory.getLog(getClass());

    private final String login;

    private final String password;

    private final HttpHost httpHost;

    /**
     * Construct object.
     * @deprecated Use {@link #LoginSSTRetrievalStrategy(String, String)}}
     * @param httpClient HTTP client
     * @param httpHost http host
     * @param login user login
     * @param password user password
     */
    @Deprecated
    public LoginSSTRetrievalStrategy(final HttpClient httpClient, final HttpHost httpHost, final String login, final String password) {
        notNull(httpHost, "HTTP host cannot be null");
        notNull(login, "Login cannot be null");
        notNull(password, "Password cannot be null");
        this.login = login;
        this.password = password;
        this.httpHost = httpHost;
    }

    /**
     * Construct object.
     * @param login user login
     * @param password user password
     */
    public LoginSSTRetrievalStrategy(final String login, final String password) {
        notNull(login, "Login cannot be null");
        notNull(password, "Password cannot be null");
        this.login = login;
        this.password = password;
        this.httpHost = null;
    }

    HttpHost getHttpHost() {
        return httpHost;
    }

    @Override
    public String obtainSst(final HttpClient httpClient, final HttpHost httpHost) throws IOException {
        notNull(httpClient, "client can't be null");
        notNull(httpHost, "host can't be null");

        log.debug("Obtaining SST");
        final HttpPost postLogin = new HttpPost(LOGIN_URL);
        try {
            final HttpEntity requestEntity = new StringEntity(createLoginJson(), ContentType.APPLICATION_JSON);
            postLogin.setEntity(requestEntity);
            postLogin.setHeader(HttpHeaders.ACCEPT, YAML_CONTENT_TYPE);
            final HttpResponse response = httpClient.execute(httpHost, postLogin);
            int status = response.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_OK) {
                final Header requestIdHeader = response.getFirstHeader(X_GDC_REQUEST_HEADER_NAME);
                final HttpEntity responseEntity = response.getEntity();
                final String message = format("Unable to login reason='%s'. Request tracking details httpStatus=%s requestId=%s",
                        getReason(responseEntity), status, getRequestId(requestIdHeader));
                log.info(message);
                throw new GoodDataAuthException(message);
            }

            return TokenUtils.extractToken(response);
        } finally {
            postLogin.reset();
        }
    }

    /**
     * Returns the failure reason message if present.
     */
    private String getReason(final HttpEntity responseEntity) throws IOException {
        return (responseEntity != null) ? EntityUtils.toString(responseEntity) : null;
    }

    /**
     * Returns the request id if present. The request id is generated by server
     * and serves for troubleshooting.
     */
    private String getRequestId(final Header requestIdHeader) {
        return (requestIdHeader != null) ? requestIdHeader.getValue() : null;
    }

    @Override
    public void logout(final HttpClient httpClient, final HttpHost httpHost, final String url, final String sst, final String tt)
            throws IOException, GoodDataLogoutException {
        notNull(httpClient, "client can't be null");
        notNull(httpHost, "host can't be null");
        notEmpty(url, "url can't be empty");
        notEmpty(sst, "SST can't be empty");
        notEmpty(tt, "TT can't be empty");

        log.debug("performing logout");
        final HttpDelete request = new HttpDelete(url);
        try {
            request.setHeader(SST_HEADER, sst);
            request.setHeader(TT_HEADER, tt);
            final HttpResponse response = httpClient.execute(httpHost, request);
            final StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_NO_CONTENT) {
                throw new GoodDataLogoutException("Logout unsuccessful using http",
                        statusLine.getStatusCode(), statusLine.getReasonPhrase());
            }
        } finally {
            request.reset();
        }
    }

    private String createLoginJson() {
        return "{\"postUserLogin\":{\"login\":\"" + StringEscapeUtils.escapeJavaScript(login) +
                "\",\"password\":\"" + StringEscapeUtils.escapeJavaScript(password) + "\",\"remember\":0" +
                ",\"verify_level\":" + VERIFICATION_LEVEL + "}}";
    }

    /**
     * Fot tests only
     */
    void setLogger(Log log) {
        this.log = log;
    }

}
