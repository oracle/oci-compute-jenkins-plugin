package com.oracle.cloud.baremetal.jenkins;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import hudson.Util;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

public class FormFillFailureUnitTest {
    static { TestMessages.init(); }

    public static ListBoxModel.Option getErrorOption(FormFillFailure e) {
        if (e.response instanceof ListBoxModel) {
            ListBoxModel model = (ListBoxModel)e.response;
            Assert.assertEquals(1, model.size());
            return model.get(0);
        }

        // We are running with Jenkins 2.50+, so
        // hudson.util.FormFillFailure maintains the value.
        return null;
    }

    public static ListBoxModel.Option assumeGetErrorOption(FormFillFailure e) {
        ListBoxModel.Option option = getErrorOption(e);
        Assume.assumeNotNull(option);
        return option;
    }

    @Test(expected = NullPointerException.class)
    public void testErrorWithValueNullValue() {
        FormFillFailure.errorWithValue("a<>b", null);
    }

    @Test
    public void testErrorWithValueErrorString() {
        Assert.assertEquals(Util.escape("a<>b"), FormFillFailure.errorWithValue("a<>b", "").getMessage());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testErrorWithValueFormValidationWarning() {
        FormFillFailure.errorWithValue(FormValidation.warning("a<>b"), "");
    }

    @Test
    public void testErrorWithValueFormValidation() {
        Assert.assertEquals(Util.escape("a<>b"), FormFillFailure.errorWithValue(FormValidation.error("a<>b"), "").getMessage());
    }

    @Test
    public void testIsError() {
        Assert.assertFalse(FormFillFailure.isError(null));
        Assert.assertFalse(FormFillFailure.isError(""));
        Assert.assertFalse(FormFillFailure.isError("x"));
    }

    @Test
    public void testIsErrorWithFallback() {
        Assert.assertTrue(FormFillFailure.isError(assumeGetErrorOption(FormFillFailure.errorWithValue("e", "x")).value));
    }

    @Test
    public void testGetErrorValue() {
        Assert.assertNull(FormFillFailure.getErrorValue(null));
        Assert.assertEquals("", FormFillFailure.getErrorValue(""));
        Assert.assertEquals("x", FormFillFailure.getErrorValue("x"));
    }

    @Test
    public void testGetErrorValueWithFallback() {
        String encodedValue = assumeGetErrorOption(FormFillFailure.errorWithValue("e", "x")).value;
        Assert.assertEquals("x", FormFillFailure.getErrorValue(encodedValue));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetErrorValueWithFallbackEncoded() {
        String encodedValue = assumeGetErrorOption(FormFillFailure.errorWithValue("e", "x")).value;
        Assert.assertEquals("x", FormFillFailure.getErrorValue(assumeGetErrorOption(FormFillFailure.errorWithValue("e", encodedValue)).value));
    }

    @Test
    public void testValidateRequired() {
        Assert.assertEquals(FormValidation.Kind.ERROR, FormFillFailure.validateRequired(null).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, FormFillFailure.validateRequired("").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, FormFillFailure.validateRequired(" ").kind);
        Assert.assertEquals(FormValidation.Kind.OK, FormFillFailure.validateRequired("x").kind);
    }

    @Test
    public void testValidateRequiredWithFallback() {
        Assert.assertEquals(FormValidation.Kind.ERROR, FormFillFailure.validateRequired(assumeGetErrorOption(FormFillFailure.errorWithValue("e", "x")).value).kind);
    }
}
