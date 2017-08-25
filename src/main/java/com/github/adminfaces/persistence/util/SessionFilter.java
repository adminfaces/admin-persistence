package com.github.adminfaces.persistence.util;

import com.github.adminfaces.persistence.bean.CrudMB;
import com.github.adminfaces.persistence.model.Filter;
import com.github.adminfaces.persistence.model.PersistenceEntity;

import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@SessionScoped
public class SessionFilter implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<Class<? extends CrudMB>, Filter<? extends PersistenceEntity>> sessionMap = new HashMap<>();

    public void add(Class<? extends CrudMB> key, Filter<? extends PersistenceEntity> value) {
        sessionMap.put(key, value);
    }

    public void clear(Class<? extends CrudMB> key) {
        if (sessionMap.containsKey(key)) {
            sessionMap.put(key, null);
        }
    }

    public Filter<? extends PersistenceEntity> get(Class<? extends CrudMB> key) {
        return sessionMap.get(key);
    }

}
