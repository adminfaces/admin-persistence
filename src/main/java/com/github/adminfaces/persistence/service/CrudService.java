package com.github.adminfaces.persistence.service;

import com.github.adminfaces.persistence.model.Filter;
import com.github.adminfaces.persistence.model.PersistenceEntity;
import com.github.adminfaces.persistence.model.AdminSort;
import org.apache.deltaspike.data.api.criteria.Criteria;
import org.apache.deltaspike.data.api.criteria.CriteriaSupport;
import org.apache.deltaspike.data.impl.handler.CriteriaSupportHandler;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.metamodel.SingularAttribute;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author rmpestano
 *
 * Utility service for crud
 */
@Service
public class CrudService<T extends PersistenceEntity, PK extends Serializable> extends CriteriaSupportHandler<T> implements CriteriaSupport<T>, Serializable {

    protected Class<T> entityClass;

    protected Class<PK> entityKey;

    @Inject
    protected EntityManager entityManager;


    @Inject
    public void CrudService(InjectionPoint ip) {
        if (ip != null && ip.getType() != null) {
            try {
                //will work for @Inject @Service CrudService<Entity,Key>
                resolveEntity(ip);
            } catch (Exception e) {
            }
        }

        if (entityClass == null) {
            //will work for service inheritance MyService extends CrudService<Entity, Key>
            entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            entityKey = (Class<PK>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];

        }
    }

    private void resolveEntity(InjectionPoint ip) {
        ParameterizedType type = (ParameterizedType) ip.getType();
        Type[] typeArgs = type.getActualTypeArguments();
        entityClass = (Class<T>) typeArgs[0];
        entityKey = (Class<PK>) typeArgs[1];
    }

    public List<T> paginate(Filter<T> filter) {
        Criteria<T, T> criteria = configRestrictions(filter);

        String sortField = filter.getSortField();
        if (sortField != null) {
            SingularAttribute sortAttribute = entityManager.getMetamodel().entity(entityClass).getSingularAttribute(sortField);
            if (filter.getAdminSort().equals(AdminSort.UNSORTED)) {
                filter.setAdminSort(AdminSort.ASCENDING);
            }
            if (filter.getAdminSort().equals(AdminSort.ASCENDING)) {
                criteria.orderAsc(sortAttribute);
            } else {
                criteria.orderDesc(sortAttribute);
            }
        }

        return criteria.createQuery()
                .setFirstResult(filter.getFirst())
                .setMaxResults(filter.getPageSize())
                .getResultList();
    }

    /**
     * Called before pagination, should be overriden. By default there is no restrictions.
     * @param filter used to create restrictions
     * @return a criteria with configured restrictions
     */
    protected Criteria<T, T> configRestrictions(Filter<T> filter) {
        return criteria();
    }


    public void insert(T entity) {
        if (entity == null) {
            throw new RuntimeException("Record cannot be null");
        }

        if (entity.getId() != null) {
            throw new RuntimeException("Record must be transient");
        }
        beforeInsert(entity);
        entityManager.persist(entity);
        afterInsert(entity);
    }

    public void remove(T entity) {
        if (entity == null) {
            throw new RuntimeException("Record cannot be null");
        }

        if (entity.getId() == null) {
            throw new RuntimeException("Record cannot be transient");
        }
        beforeRemove(entity);
        entityManager.remove(entityManager.find(entityClass, entity.getId()));
        afterRemove(entity);
    }


    public void remove(List<T> entities) {
        if (entities == null) {
            throw new RuntimeException("Entities cannot be null");
        }
        for (T t : entities) {
            this.remove(t);
        }
    }

    public void update(T entity) {
        if (entity == null) {
            throw new RuntimeException("Record cannot be null");
        }

        if (entity.getId() == null) {
            throw new RuntimeException("Record cannot be transient");
        }
        beforeUpdate(entity);
        entityManager.merge(entity);
        afterUpdate(entity);
    }

    public long count(Filter<T> filter) {
        SingularAttribute<? super T, PK> id = entityManager.getMetamodel().entity(entityClass).getId(entityKey);
        return configRestrictions(filter)
                .select(Long.class, count(id))
                .getSingleResult();
    }

    public T findById(Serializable id) {
        T entity = entityManager.find(entityClass, id);
        if (entity == null) {
            throw new RuntimeException(String.format("Record with id %s not found.", id));
        }
        return entity;
    }

    public Class<PK> getEntityKey() {
        return entityKey;
    }

    @Override
    public Class<T> getEntityClass() {
        return entityClass;
    }

    @Override
    public EntityManager getEntityManager() {
        return entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void beforeInsert(T entity) {
    }

    public void afterInsert(T entity) {
    }

    public void beforeUpdate(T entity) {
    }

    public void afterUpdate(T entity) {
    }

    public void beforeRemove(T entity) {
    }

    public void afterRemove(T entity) {
    }

}
