package com.oracle.cloud.baremetal.jenkins;

import hudson.util.FormValidation;

public class FormValidationValue<T> {
    public static <T> FormValidationValue<T> ok(T value) {
        return new FormValidationValue<>(FormValidation.ok(), value);
    }

    public static <T> FormValidationValue<T> error(String s) {
        return error(s, null);
    }

    public static <T> FormValidationValue<T> error(String s, T defaultValue) {
        return error(FormValidation.error(s), defaultValue);
    }

    public static <T> FormValidationValue<T> error(FormValidation formValidation) {
        return error(formValidation, null);
    }

    public static <T> FormValidationValue<T> error(FormValidation formValidation, T defaultValue) {
        if (formValidation.kind == FormValidation.Kind.OK) {
            throw new IllegalArgumentException();
        }
        return new FormValidationValue<>(formValidation, defaultValue);
    }

    public static FormValidationValue<Integer> validatePositiveInteger(String value, int defaultValue) {
        FormValidation fv = FormValidation.validatePositiveInteger(value);
        if (fv.kind != FormValidation.Kind.OK) {
            return error(fv, defaultValue);
        }
        return ok(Integer.parseInt(value));
    }

    public static FormValidationValue<Integer> validateNonNegativeInteger(String value, int defaultValue) {
        FormValidation fv = FormValidation.validateNonNegativeInteger(value);
        if (fv.kind != FormValidation.Kind.OK) {
            return error(fv, defaultValue);
        }
        return ok(Integer.parseInt(value));
    }

    private final FormValidation formValidation;
    private final T value;

    private FormValidationValue(FormValidation formValidation, T value) {
        this.formValidation = formValidation;
        this.value = value;
    }

    @Override
    public String toString() {
        return isOk() ? "OK: " + value : formValidation.toString();
    }

    public FormValidation getFormValidation() {
        return formValidation;
    }

    public boolean isOk() {
        return formValidation.kind == FormValidation.Kind.OK;
    }

    public T getValue() {
        return value;
    }
}
