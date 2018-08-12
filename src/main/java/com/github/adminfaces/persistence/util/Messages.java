package com.github.adminfaces.persistence.util;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for resourceBundle and facesMessages
 */
public class Messages implements Serializable {

    private static final Logger log = Logger.getLogger(Messages.class.getName());

    private static ResourceBundle bundle;

    static {
        try {
            if (FacesContext.getCurrentInstance() != null) {
                bundle = ResourceBundle.getBundle("messages", FacesContext.getCurrentInstance().getViewRoot().getLocale());
            } else {
                bundle = ResourceBundle.getBundle("messages", Locale.getDefault());
            }
        } catch (MissingResourceException e) {
            log.log(Level.WARNING, "Application resource bundle named 'messages' not found.");
        }
    }

    public static String getMessage(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "??" + key + "??";
        }
    }

    public static String getMessage(String key, Object... params) {
        return MessageFormat.format(getMessage(key), params);
    }

    public static void addDetailMessage(String message) {
        addDetailMessage(message, null);
    }

    public static void addDetailMessage(String message, FacesMessage.Severity severity) {

        FacesMessage facesMessage = new FacesMessage("", message);
        if (severity != null && severity != FacesMessage.SEVERITY_INFO) {
            facesMessage.setSeverity(severity);
        }

        FacesContext.getCurrentInstance().addMessage(null, facesMessage);
    }
}
