package com.github.adminfaces.persistence.util;

import org.omnifaces.util.Faces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.application.FacesMessage;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

@ApplicationScoped
public class Messages {

    private static final Logger LOG = LoggerFactory.getLogger(Messages.class.getName());

    private ResourceBundle bundle;

    @PostConstruct
    public void init() {
        try {
            bundle = ResourceBundle.getBundle("messages", Faces.getLocale());
        }catch (MissingResourceException e) {
            LOG.warn("Application resource bundle named 'messages' not found.");
        }
    }

    public String getMessage(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "??" + key + "??";
        }
    }

    public String getMessage(String key, Object... params) {
        return MessageFormat.format(getMessage(key), params);
    }


    public static void addDetailMessage(String message) {
        addDetailMessage(message, null);
    }

    public static void addDetailMessage(String message, FacesMessage.Severity severity) {

        FacesMessage facesMessage = org.omnifaces.util.Messages.create("").detail(message).get();
        if (severity != null && severity != FacesMessage.SEVERITY_INFO) {
            facesMessage.setSeverity(severity);
        } else {
            org.omnifaces.util.Messages.add(null, facesMessage);
        }
    }
}
