package com.oracle.cloud.baremetal.jenkins;

import org.junit.Assert;
import org.junit.Test;

import hudson.util.FormValidation;

public class FormValidationValueUnitTest {
    @Test
    public void testOk() {
        FormValidationValue<String> fvv = FormValidationValue.ok("value");
        Assert.assertNotNull(fvv.toString());
        Assert.assertEquals(FormValidation.Kind.OK, fvv.getFormValidation().kind);
        Assert.assertTrue(fvv.isOk());
        Assert.assertEquals("value", fvv.getValue());
    }

    @Test
    public void testError() {
        FormValidationValue<String> fvv = FormValidationValue.error("error");
        Assert.assertNotNull(fvv.toString());
        Assert.assertEquals(FormValidation.Kind.ERROR, fvv.getFormValidation().kind);
        Assert.assertFalse(fvv.isOk());
        Assert.assertEquals("error", fvv.getFormValidation().getMessage());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testErrorOk() {
        FormValidationValue.error(FormValidation.ok());
    }

    @Test
    public void testErrorDefaultValue() {
        Assert.assertNull(FormValidationValue.error("error").getValue());
        Assert.assertEquals(1, (int)FormValidationValue.error("error", 1).getValue());
    }

    @Test
    public void testValidatePositiveInteger() {
        FormValidationValue<Integer> fv = FormValidationValue.validatePositiveInteger("2", 0);
        Assert.assertTrue(fv.isOk());
        Assert.assertEquals(2, (int)fv.getValue());

        fv = FormValidationValue.validatePositiveInteger("0", 2);
        Assert.assertFalse(fv.isOk());
        Assert.assertEquals(2, (int)fv.getValue());
    }

    @Test
    public void testValidateNonNegativeInteger() {
        FormValidationValue<Integer> fv = FormValidationValue.validateNonNegativeInteger("0", 2);
        Assert.assertTrue(fv.isOk());
        Assert.assertEquals(0, (int)fv.getValue());

        fv = FormValidationValue.validatePositiveInteger("-1", 2);
        Assert.assertFalse(fv.isOk());
        Assert.assertEquals(2, (int)fv.getValue());
    }
}
