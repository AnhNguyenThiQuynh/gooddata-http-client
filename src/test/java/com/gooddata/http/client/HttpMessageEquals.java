package com.gooddata.http.client;

import com.sun.org.apache.xpath.internal.operations.Equals;
import org.apache.http.HttpMessage;
import org.apache.http.client.methods.HttpUriRequest;
import org.hamcrest.Description;
import org.hamcrest.SelfDescribing;
import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.ContainsExtraTypeInformation;
import org.mockito.internal.matchers.Equality;

import java.io.Serializable;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Created by martin on 31/07/15.
 */
public class HttpMessageEquals extends ArgumentMatcher<HttpUriRequest> implements ContainsExtraTypeInformation, Serializable {

    private final HttpUriRequest wanted;

    public HttpMessageEquals(final HttpUriRequest wanted) {
        this.wanted = notNull(wanted);
    }

    public boolean matches(Object actual) {
        if (!(actual instanceof HttpUriRequest)) {
            return false;
        }
        final HttpUriRequest value = (HttpUriRequest) actual;

        value.getAllHeaders()

        return Equality.areEqual(this.wanted.getURI(), value.getURI()) &&
        if (value.getURI())


        return Equality.areEqual(this.wanted, actual);
    }

    public void describeTo(Description description) {
        description.appendText(describe(wanted));
    }

    public String describe(Object object) {
        return object.toString();
    }

    protected final Object getWanted() {
        return wanted;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !this.getClass().equals(o.getClass())) {
            return false;
        }
        HttpMessageEquals other = (HttpMessageEquals) o;
        return this.wanted == null && other.wanted == null || this.wanted != null && this.wanted.equals(other.wanted);
    }

    @Override
    public int hashCode() {
        return 1;
    }

    public SelfDescribing withExtraTypeInfo() {
        return new SelfDescribing() {
            public void describeTo(Description description) {
                description.appendText(describe("("+ wanted.getClass().getSimpleName() +") " + wanted));
            }};
    }

    public boolean typeMatches(Object object) {
        return wanted != null && object != null && object.getClass() == wanted.getClass();
    }
}
