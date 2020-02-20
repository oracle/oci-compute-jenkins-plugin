package com.oracle.cloud.baremetal.jenkins;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jvnet.localizer.LocaleProvider;
import org.jvnet.localizer.ResourceBundleHolder;

/**
 * An alternative to {@link ResourceBundleHolder} that does not require a
 * statically generated class with the same name as the properties file.
 */
public class DynamicResourceBundleHolder {
    /**
     * Returns a {@link ResourceBundleHolder} for the given class.
     *
     * @param owner the resource bundle owner
     * @param shortName the resource bundle name relative to the owner

     * @return a {@link ResourceBundleHolder} for the given class
     */
    public synchronized static DynamicResourceBundleHolder get(Class<?> owner, String shortName) {
        String name = owner.getName().replace('.', '/') + '/' + shortName;
        return new DynamicResourceBundleHolder(owner.getClassLoader(), name);
    }

    ClassLoader classLoader;
    private final String name;

    private DynamicResourceBundleHolder(ClassLoader classLoader, String name) {
        this.classLoader = classLoader;
        this.name = name;
    }

    private synchronized ResourceBundle get(Locale locale) {
        return ResourceBundle.getBundle(name, locale, classLoader);
    }

    /**
     * Formats a resource specified by the given key by using the default locale
     * @param key key
     * @param args object(s) to format
     * @return the formatted string
     */
    public String format(String key, Object... args) {
        return MessageFormat.format(get(LocaleProvider.getLocale()).getString(key),args);
    }
}
