package com.oracle.cloud.baremetal.jenkins;

import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;

public class JenkinsUtil {

    //Generic type-safe wrapper for {@link Jenkins#getDescriptorOrDie}.
    public static <T extends Describable<T>> Descriptor<T> getDescriptorOrDie(Class<? extends T> type) {
        @SuppressWarnings("unchecked")
        Descriptor<T> desc = JenkinsUtil.getJenkinsInstance().getDescriptorOrDie(type);
        if (!desc.isSubTypeOf(type)) {
            throw new IllegalStateException(type.toString());
        }
        return desc;
    }

    // Generic type-safe wrapper for {@link Jenkins#getDescriptorOrDie} to be
    // used when a concrete descriptor class is used.
    public static <S extends Describable<S>, T extends S, D extends Descriptor<S>> D getDescriptorOrDie(Class<T> type, Class<D> descriptorType) {
        return descriptorType.cast(JenkinsUtil.getJenkinsInstance().getDescriptorOrDie(type));
    }

    /**
     * Similar to {@link FormValidation#validateRequired}, but uses the same
     * error message as {@code <f:textbox clazz="required"/>}.
     * @param value, the validation value
     * @return FormValidation
     */
    public static FormValidation validateRequired(String value) {
        if (Util.fixEmptyAndTrim(value) == null) {
            return FormValidation.error(Messages.FormValidation_ValidateRequired());
        }
        return FormValidation.ok();
    }

    private static class Unescaper {
        private final String string;
        private int pos;
        private final StringBuilder builder;

        Unescaper(String string) {
            this.string = string;
            this.builder = new StringBuilder(string.length());
        }

        String unescape() {
            while (pos < string.length()) {
                if (!unescape("<br>", '\n') &&
                        !unescape("&lt;", '<') &&
                        !unescape("&gt;", '>') &&
                        !unescape("&amp;", '&') &&
                        !unescape("&quot;", '"') &&
                        !unescape("&#039;", '\'') &&
                        !unescape("&nbsp;", ' ')) {
                    builder.append(string.charAt(pos));
                    pos++;
                }
            }
            return builder.toString();
        }

        private boolean unescape(String from, char to) {
            if (pos + from.length() <= string.length() && string.regionMatches(pos, from, 0, from.length())) {
                builder.append(to);
                pos += from.length();
                return true;
            }
            return false;
        }
    }

    //Unescapes a string escaped by {@link Util#escape}.
    public static String unescape(String s) {
        return new Unescaper(s).unescape();
    }

    public static Jenkins getJenkinsInstance() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Fail to get Jenkins instance, which means it has not been started, or was already shut down");
        }
        return jenkins;
    }
}

