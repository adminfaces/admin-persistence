package com.github.adminfaces.persistence;

import com.github.adminfaces.persistence.util.Messages;
import org.junit.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class MessagesIt {

    @Test
    public void shouldGetMessageFromProperties() throws ClassNotFoundException {
        Messages.changeLocale(Locale.forLanguageTag("en"));
        Class.forName(Messages.class.getName(), true, MessagesIt.class.getClassLoader()); //forces static block initialization
        assertThat(Messages.getMessage("hello"))
                .isNotNull()
                .isEqualTo("world");
    }

    @Test
    public void shouldGetMessageWithParamFromProperties() throws ClassNotFoundException {
        Messages.changeLocale(Locale.forLanguageTag("en"));
        assertThat(Messages.getMessage("hello.param", "AdminFaces"))
                .isNotNull()
                .isEqualTo("Hello AdminFaces");
    }

    @Test
    public void shouldGetMessageFromPT_BRProperties() throws ClassNotFoundException {
        Messages.changeLocale(Locale.forLanguageTag("pt"));
        assertThat(Messages.getMessage("hello"))
                .isNotNull()
                .isEqualTo("mundo");
    }

    @Test
    public void shouldGetMessageWithParamFromPT_BRProperties() throws ClassNotFoundException {
        Messages.changeLocale(Locale.forLanguageTag("pt"));
        assertThat(Messages.getMessage("hello.param", "AdminFaces"))
                .isNotNull()
                .isEqualTo("Olá AdminFaces");
    }
}
