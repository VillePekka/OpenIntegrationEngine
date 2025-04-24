package com.mirth.connect.server.launcher;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Kiran Ayyagari (kiran@sereen.io)
 */
public class ParentFirstClassLoader extends URLClassLoader {

    public ParentFirstClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if(name.equals("com.mirth.connect.donkey.model.event.Event")) {
            System.out.println("loadClass " + name + " " + this);
        }
        synchronized (getClassLoadingLock(name)) {
            Class<?> loadedClass = findLoadedClass(name);
            if(loadedClass != null) {
                return loadedClass;
            }
            try {
                if (getParent() != null) {
                    return getParent().loadClass(name);
                }
            } catch (ClassNotFoundException e) {
                // Parent could not find the class, proceed to load locally
            }

            // If parent couldn't load, try to find the class in this classloader's URLs
            try {
                loadedClass = findClass(name);
            } catch (ClassNotFoundException e) {
                // If not found locally, throw ClassNotFoundException
                throw new ClassNotFoundException(name);
            }

            // Resolve the class if requested
            if (resolve) {
                resolveClass(loadedClass);
            }

            return loadedClass;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Delegate to URLClassLoader's findClass to load from URLs
        if(name.equals("com.mirth.connect.donkey.model.event.Event")) {
            System.out.println("findClass " + name + " " + this);
        }

        return super.findClass(name);
    }

    @Override
    public URL getResource(String name) {
        System.out.println("getting resource " + name);
        if(name.endsWith("mirth.properties")) {
            System.out.println(name);
        }
        return super.getResource(name);
    }

    @Override
    public URL findResource(String name) {
        if(name.endsWith("mirth.properties")) {
            System.out.println(name);
        }
        return super.findResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        if(name.endsWith("mirth.properties")) {
            System.out.println(name);
        }
        return super.getResourceAsStream(name);
    }
}
