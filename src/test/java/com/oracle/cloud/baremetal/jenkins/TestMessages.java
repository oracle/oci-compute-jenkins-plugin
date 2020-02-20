package com.oracle.cloud.baremetal.jenkins;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.jvnet.localizer.ResourceBundleHolder;

/**
 * Allow the {@link Messages} class to be used even if src/main/resources is not
 * present on the Eclipse project Build Path.
 */
public class TestMessages {
    private static final ResourceBundleHolder holder;

    private static class ResourceClassLoader extends ClassLoader {
        private final File resourcesDir = new File("src/main/resources");

        @Override
        public URL getResource(String name) {
            File file = new File(resourcesDir, name);
            if (file.isFile()) {
                try {
                    return file.toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new Error(e);
                }
            }
            return super.getResource(name);
        }

        Class<?> defineClass(Class<?> c) throws Exception {
            // Redefine the class in this class loader so that when
            // ResourceBundleHolder calls Class.getResource, it will
            // ultimately call our overridden getResource.
            String name = c.getName();
            try (InputStream in = c.getResourceAsStream(name.substring(name.lastIndexOf('.') + 1).replace('.', '/') + ".class")) {
                byte[] buf = IOUtils.toByteArray(in);
                return defineClass(name, buf, 0, buf.length);
            }
        }
    }

    static {
        final Class<?> messagesClass = Messages.class;
        holder = ResourceBundleHolder.get(messagesClass);

        try {
            ResourceClassLoader resourceClassLoader = new ResourceClassLoader();

            Field resourceBundleHolderOwnerField = ResourceBundleHolder.class.getField("owner");
            resourceBundleHolderOwnerField.setAccessible(true);
            resourceBundleHolderOwnerField.set(holder, resourceClassLoader.defineClass(messagesClass));

//            BaremetalCloudAgentTemplate.ConfigMessages.holder.classLoader = resourceClassLoader;
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static void init() {}
}
