package me.typical.lixiplugin.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify that a config file should be placed in a subfolder.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Folder {
    /**
     * @return The subfolder name within the plugin's data folder
     */
    String value();
}
