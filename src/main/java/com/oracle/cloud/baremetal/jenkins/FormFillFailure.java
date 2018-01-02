package com.oracle.cloud.baremetal.jenkins;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Objects;

import javax.servlet.ServletException;

import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Util;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

/**
 * Helper for {@code hudson.util.FormFillFailure}, which was not added until
 * <a href="https://issues.jenkins-ci.org/browse/JENKINS-42443">JENKINS-42443</a>,
 * which was added in Jenkins 2.50.  If an older version of Jenkins is being
 * used at runtime, then a fallback implementation is used.
 *
 * <p><b>Sample usage</b>
 * <pre>
 *   public class TheClass {
 *     {@literal @}DataBoundConstructor
 *     public TheClass(String theField) {
 *       // Decode in case the control form had an encoded error.
 *       this.theField = FormFillFailure.getErrorValue(theField);
 *     }
 *
 *     public class DescriptorImpl ... {
 *       public ListBoxModel doFillTheFieldItems({@literal @}QueryParameter String theField) throws FormFillFailure {
 *         // Decode in case the control form had an encoded error.
 *         theField = FormFillFailure.getErrorValue(theField);
 *         if (...error...) {
 *           throw FormFillFailure.error(...message..., theField);
 *         }
 *
 *         // Add an empty value to require selection.
 *         ListBoxModel model = new ListBoxModel().add("");
 *
 *         // Explicitly set the selected parameter in case the control form
 *         // value had an encoded error with this value.
 *         model.add(new ListBoxModel.Option(displayName, value, value.equals(theField)));
 *
 *         return model;
 *       }
 *
 *       public FormValidation doCheckTheField({@literal @}QueryParameter String theField) {
 *         // Decode in case the control form had an encoded error without
 *         // a selected value.
 *         return FormFillFailure.validateRequired(value);
 *       }
 *     }
 *   }
 * </pre>
 */
@SuppressWarnings("serial")
public class FormFillFailure extends IOException implements HttpResponse {
    // This class can be tested using: mvn -Djenkins.version=2.50

    /**
     * The {@code hudson.util.FormFillFailure.error(String)} method, or null if
     * the fallback implementation should be used.
     */
    private static final Method FORM_FILL_FAILURE_ERROR_METHOD = getFormFillFailureErrorMethod();

    private static Method getFormFillFailureErrorMethod() {
        try {
            Class<? extends HttpResponse> c = Class.forName("hudson.util.FormFillFailure").asSubclass(HttpResponse.class);
            return c.getMethod("error", String.class);
        } catch (ClassNotFoundException |NoSuchMethodException e) {
            return null;
        }
    }

    private static final String ERROR_VALUE_PREFIX = FormFillFailure.class.getName() + ".ERROR:";

    private static FormFillFailure errorWithValue(String message, Throwable cause, String value) {
        Objects.requireNonNull(value, "value");

        HttpResponse response = null;
        if (FORM_FILL_FAILURE_ERROR_METHOD != null) {
            try {
                response = (HttpResponse)FORM_FILL_FAILURE_ERROR_METHOD.invoke(null, message);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            // Use the fallback implementation, which is to create a single
            // option with a display name that shows an error, and an encoded
            // value that we can detect later.

            // If the form control value is already an encoded error,
            if (isError(value)) {
                throw new IllegalArgumentException();
            }

            ListBoxModel model = new ListBoxModel();
            model.add(Messages.FormFillFailure_error(message), ERROR_VALUE_PREFIX + value);
            response = model;
        }

        return new FormFillFailure(Util.escape(message), cause, response);
    }

    /**
     * Creates a new {@code FormFillFailure} with the specified message and with
     * the specified value.  The value will be encoded in the form control value
     * so that both the error status and original value can be determined.
     *
     * @param message the error message
     * @param value the value when the error occurred
     *
     * @return the {@code FormFillFailure} created
     *
     * @throws IllegalArgumentException if the value was already encoded by this
     * method
     * @see #getErrorValue
     */
    public static FormFillFailure errorWithValue(String message, String value) {
        return errorWithValue(message, null, value);
    }

    /**
     * Calls {@link #errorWithValue(String, String)} with the message from the
     * specified {@code FormValidation}.  The kind must be
     * {@code FormValidation.Kind.ERROR}.
     *
     * @param fv the cause of the failure
     * @param value the value when the error occurred
     *
     * @return the {@code FormFillFailure} created
     *
     * @throws IllegalArgumentException if the kind is not ERROR
     * @throws IllegalArgumentException if the value was already encoded by
     * {@link #errorWithValue(String, String)}
     * @see #getErrorValue
     */
    public static FormFillFailure errorWithValue(FormValidation fv, String value) {
        if (fv.kind != FormValidation.Kind.ERROR) {
            throw new IllegalArgumentException("kind " + fv.kind);
        }
        return errorWithValue(JenkinsUtil.unescape(fv.getMessage()), fv, value);
    }

    /**
     * True if the form control value was encoded by
     * {@link #errorWithValue(String, String)}.
     *
     * @param formControlValue form control value
     *
     * @return {@code true} if the form control value was encoded by
     * {@link #errorWithValue(String, String)}
     */
    public static boolean isError(String formControlValue) {
        return formControlValue != null && formControlValue.startsWith(ERROR_VALUE_PREFIX);
    }

    /**
     * Returns the value when the error occurred, or {@code null} if the form
     * control value was not encoded by {@link #errorWithValue(String, String)}.
     *
     * @param formControlValue form control value
     *
     * @return the value when the error occurred or {@code null} if the form
     * control value was not encoded by {@link #errorWithValue(String, String)}
     */
    public static String getErrorValue(String formControlValue) {
        return isError(formControlValue) ? formControlValue.substring(ERROR_VALUE_PREFIX.length()) : formControlValue;
    }

    /**
     * Extension of {@link JenkinsUtil#validateRequired} that always fails if
     * the form control value was encoded by
     * {@link #errorWithValue(String, String)}.
     *
     * @param formControlValue form control value
     * @return a {@link FormValidation} object created
     */
    public static FormValidation validateRequired(String formControlValue) {
        if (isError(formControlValue)) {
            return FormValidation.error(Messages.FormValidation_ValidateRequired());
        }
        return JenkinsUtil.validateRequired(formControlValue);
    }

    final HttpResponse response;

    private FormFillFailure(String message, Throwable cause, HttpResponse response) {
        super(message);
        this.response = response;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + response.getClass().getSimpleName() + ": " + response + ']';
    }

    @Override
    public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
        response.generateResponse(req, rsp, node);
    }
}
