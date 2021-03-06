package com.github.adminfaces.persistence.bean;

import com.github.adminfaces.persistence.model.Filter;
import com.github.adminfaces.persistence.model.PersistenceEntity;
import com.github.adminfaces.persistence.service.CrudService;
import com.github.adminfaces.persistence.util.AdminDataModel;
import com.github.adminfaces.persistence.util.Messages;
import com.github.adminfaces.persistence.util.SessionFilter;

import javax.annotation.PostConstruct;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.adminfaces.persistence.util.Messages.addDetailMessage;

/**
 * @author <a href="http://github.com/rmpestano">Rafael Pestano</a>
 *
 * Template (JSF) bean for CRUD operations over a JPA entity
 * @param <T> the entity type
 *
 */
public abstract class CrudMB<T extends PersistenceEntity> implements Serializable {

    protected final Logger log = Logger.getLogger(getClass().getName());

    protected CrudService<T, ? extends Serializable> crudService;

    protected T entity; //entity to crud

    protected Serializable id; //used as view param in GET based navigation

    protected Filter<T> filter; //used for search parameters

    protected AdminDataModel<T> list; //datatable pagination

    protected List<T> selectionList; //holds selected rows in datatable with multiple selection (checkbox column)

    protected T selection; //holds single selection

    protected List<T> filteredValue;// datatable filteredValue attribute (column filters)

    @Inject
    protected SessionFilter sessionFilter; //save filters in session

    @Inject
    protected BeanManager beanManager;

    private String createMessage;

    private String removeMessage;

    private String updateMessage;

    @PostConstruct
    public void initCrudMB() {

        if (getCrudService() == null) {
            initServiceViaAnnotation();
            if (crudService == null) {
                log.log(Level.SEVERE, "You need to initialize CrudService on your Managed Bean and call setCrudService(yourService) or override getCrudService()");
                throw new RuntimeException("You need to initialize CrudService on your Managed Bean and call setCrudService(yourService) or override getCrudService()");
            }
        }

        entity = initEntity();

        filter = initFilter();

        list = new AdminDataModel<>(crudService, filter);
    }

    private void initServiceViaAnnotation() {
        if (getClass().isAnnotationPresent(BeanService.class)) {
            BeanService beanService = getClass().getAnnotation(BeanService.class);
            Class<? extends CrudService> serviceClass = beanService.value();
            crudService = getBean(serviceClass);
        }
    }

    //called via preRenderView or viewAction
    public void init() {
        if (FacesContext.getCurrentInstance().getPartialViewContext().isAjaxRequest()) {
            return;
        }

        if (id != null && !"".equals(id)) {
            entity = crudService.findById(id);
            if (entity == null) {
                log.info(String.format("Entity not found with id %s, a new one will be initialized.", id));
                id = null;
                entity = initEntity();
            }
        }
    }

    private ParameterizedType getParameterizedType() {
        ParameterizedType parameterizedType;
        if(getClass().getGenericSuperclass() instanceof ParameterizedType) {
            parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
        } else if (getClass().getSuperclass().getGenericSuperclass() instanceof ParameterizedType) {
            parameterizedType = (ParameterizedType) getClass().getSuperclass().getGenericSuperclass();
        } else {
            parameterizedType = (ParameterizedType) getClass().getSuperclass().getSuperclass().getGenericSuperclass();
        }
        return parameterizedType;
    }

    protected  <T> T getBean(Class<T> clazz) {
        Bean<T> bean = (Bean<T>) beanManager.getBeans(clazz).iterator().next();
        CreationalContext<T> ctx = beanManager.createCreationalContext(bean);
        T object = (T) beanManager.getReference(bean, clazz, ctx);
        return object;
    }

    private Filter<T> initFilter() {
        Filter<T> filter;
        if (keepFiltersInSession()) {
            String sessionFilterKey = getClass().getName();
            filter = (Filter<T>) sessionFilter.get(sessionFilterKey);
            if (filter == null) {
                filter = createFilters();
                sessionFilter.add(sessionFilterKey, filter);
            }
        } else {
            filter = createFilters();
        }

        return filter;
    }

    private T initEntity() {
        return createEntity();
    }

    public boolean isNew() {
        return entity == null || entity.getId() == null;
    }

    public T createEntity() {
        try {
            ParameterizedType parameterizedType = getParameterizedType();
            return ((Class<T>) parameterizedType.getActualTypeArguments()[0]).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.log(Level.SEVERE, String.format("Could not create entity class for bean %s", getClass().getName()), e);
            throw new RuntimeException(e);
        }
    }

    public Filter<T> createFilters() {
        return new Filter<>(createEntity());
    }

    public boolean keepFiltersInSession() {
        return true;
    }

    public void setCrudService(CrudService<T, ?> crudService) {
        this.crudService = crudService;
    }

    public AdminDataModel<T> getList() {
        return list;
    }

    public void setList(AdminDataModel<T> list) {
        this.list = list;
    }

    public List<T> getSelectionList() {
        return selectionList;
    }

    public void setSelectionList(List<T> selectionList) {
        this.selectionList = selectionList;
    }

    public T getSelection() {
        return selection;
    }

    public void setSelection(T selection) {
        this.selection = selection;
    }

    public List<T> getFilteredValue() {
        return filteredValue;
    }

    public void setFilteredValue(List<T> filteredValue) {
        this.filteredValue = filteredValue;
    }

    public T getEntity() {
        return entity;
    }

    public void setEntity(T entity) {
        this.entity = entity;
    }

    public Filter<T> getFilter() {
        return filter;
    }

    public void setFilter(Filter<T> filter) {
        this.filter = filter;
    }

    public CrudService<T, ?> getCrudService() {
        return crudService;
    }

    public Serializable getId() {
        return id;
    }

    public void setId(Serializable id) {
        this.id = id;
    }

    public String getCreateMessage() {
        if (createMessage == null) {
            createMessage = Messages.getMessage("entity.create-message", entity.getClass().getSimpleName());
            if (createMessage.startsWith("??")) {
                createMessage = "Record created successfully";
            }
        }
        return createMessage;
    }

    public String getRemoveMessage() {
        if (removeMessage == null) {
            removeMessage = Messages.getMessage("entity.remove-message", entity.getClass().getSimpleName());
            if (removeMessage.startsWith("??")) {
                removeMessage = "Record removed successfully";
            }
        }
        return removeMessage;
    }

    public String getUpdateMessage() {
        if (updateMessage == null) {
            updateMessage = Messages.getMessage("entity.update-message", entity.getClass().getSimpleName());
            if (updateMessage.startsWith("??")) {
                updateMessage = "Record updated successfully";
            }
        }
        return updateMessage;
    }

    // actions
    public T save() {
        beforeAll();
        if (isNew()) {
            beforeInsert();
            crudService.insert(entity);
            afterInsert();
        } else {
            beforeUpdate();
            entity = crudService.update(entity);
            afterUpdate();
        }
        afterAll();
        return entity;
    }

    public void remove() {
        beforeAll();
        beforeRemove();
        crudService.remove(entity);
        afterRemove();
        afterAll();
    }

    public void clear() {
        if (keepFiltersInSession()) {
            sessionFilter.clear(getClass().getName());
        }
        filter = initFilter();
        list.setFilter(filter);
        entity = initEntity();
        id = null;
    }
    
    public void beforeAll() {
    }

    public void beforeRemove() {
    }

    public void afterRemove() {
        addDetailMsg(getRemoveMessage());
    }

    public void beforeInsert() {
    }

    public void afterInsert() {
        addDetailMsg(getCreateMessage());
    }

    public void beforeUpdate() {
    }

    public void afterUpdate() {
        addDetailMsg(getUpdateMessage());
    }
    
    public void afterAll() {
    }

    public static void addDetailMsg(String message) {
        addDetailMessage(message);
    }

    public static void addDetailMsg(String message, FacesMessage.Severity severity) {
        addDetailMessage(message, severity);
    }

    /**
     * @deprecated use createFilters
     */
    @Deprecated
    public Filter<T> createDefaultFilters() {
        return createFilters();
    }

    /**
     * @deprecated use createEntity
     */
    @Deprecated
    public T createDefaultEntity() {
        return createEntity();
    }
}
