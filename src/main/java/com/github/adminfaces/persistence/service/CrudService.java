package com.github.adminfaces.persistence.service;

import com.github.adminfaces.persistence.model.Filter;
import com.github.adminfaces.persistence.model.PersistenceEntity;
import com.github.adminfaces.persistence.model.AdminSort;
import org.apache.deltaspike.data.api.criteria.Criteria;
import org.apache.deltaspike.data.api.criteria.CriteriaSupport;
import org.apache.deltaspike.data.impl.criteria.QueryCriteria;
import org.apache.deltaspike.data.impl.handler.CriteriaSupportHandler;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.JoinType;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.ListAttribute;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author rmpestano
 * Utility service for crud operations
 */
@Service
public class CrudService<T extends PersistenceEntity, PK extends Serializable> extends CriteriaSupportHandler<T> implements CriteriaSupport<T>, Serializable {

    private static final Logger LOG = Logger.getLogger(CrudService.class.getName());

    protected Class<T> entityClass;

    protected Class<PK> entityKey;

    @Inject
    protected EntityManager entityManager;


    @Inject
    protected void CrudService(InjectionPoint ip) {
        if (ip != null && ip.getType() != null) {
            try {
                //Used for generic service injection, e.g: @Inject @Service CrudService<Entity,Key>
                resolveEntity(ip);
            } catch (Exception e) {
                LOG.warning(String.format("Could not resolve entity type and entity key via injection point [%s]. Now trying to resolve via generic superclass of [%s].",ip.getMember().getName(),getClass().getName()));
            }
        }

        if (entityClass == null) {
            //Used on service inheritance, e.g: MyService extends CrudService<Entity, Key>
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
     *
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

    public T update(T entity) {
        if (entity == null) {
            throw new RuntimeException("Record cannot be null");
        }

        if (entity.getId() == null) {
            throw new RuntimeException("Record cannot be transient");
        }

        beforeUpdate(entity);
        entity = entityManager.merge(entity);
        entityManager.flush();
        afterUpdate(entity);
        return entity;
    }

    public T saveOrUpdate(T entity) {
        if (entity == null) {
            throw new RuntimeException("Record cannot be null");
        }

        if (entity.getId() == null) {
            insert(entity);
        } else {
            entity = update(entity);
        }

        return entity;
    }

    /**
     * Count all
     */
    public Long count() {
        SingularAttribute<? super T, PK> id = entityManager.getMetamodel().entity(entityClass).getId(entityKey);
        return criteria()
                .select(Long.class, count(id))
                .getSingleResult();
    }

    /**
     * Count by filter using configRestrictions to count
     *
     * @param filter
     * @return
     */
    public Long count(Filter<T> filter) {
        SingularAttribute<? super T, PK> id = entityManager.getMetamodel().entity(entityClass).getId(entityKey);
        return configRestrictions(filter)
                .select(Long.class, count(id))
                .getSingleResult();
    }

    /**
     * Count using a pre populated criteria
     *
     * @param criteria
     * @return
     */
    public Long count(Criteria<T, T> criteria) {
        SingularAttribute<? super T, PK> id = getEntityManager().getMetamodel().entity(entityClass).getId(entityKey);
        return criteria.select(Long.class, count(id))
                .getSingleResult();
    }

    public T findById(Serializable id) {
        T entity = entityManager.find(entityClass, id);
        if (entity == null) {
            throw new RuntimeException(String.format("Record with id %s not found.", id));
        }
        return entity;
    }


    /**
     * @param example An entity whose attribute's value will be used for creating a criteria
     * @param usingAttributes attributes from example entity to consider. If no attribute is provided then a no restriction will be added.
     * @return A criteria restricted by example using 'eq' for comparing attributes
     */
    public Criteria example(T example, SingularAttribute<T, ?>... usingAttributes) {
        return example(criteria(),example,usingAttributes);
    }

    /**
     * @param criteria a criteria to add restrictions based on the example entity.
     * @param example An entity whose attribute's value will be used for creating a criteria
     * @param usingAttributes attributes from example entity to consider. If no attribute is provided then a no restriction will be added.
     *
     * @return A criteria restricted by example using 'eq' for comparing attributes
     */
    public Criteria example(Criteria criteria, T example, SingularAttribute<T, ?>... usingAttributes) {
        if(criteria == null) {
            criteria = criteria();
        }
        for (SingularAttribute<T, ?> attribute : usingAttributes) {
            if (attribute.getJavaMember() instanceof Field) {
                Field field = (Field) attribute.getJavaMember();
                field.setAccessible(true);
                try {

                    Object value = field.get(example);
                    if (value != null) {
                        LOG.fine(String.format("Adding restriction by example on attribute %s using value %s.", attribute.getName(), value));
                        criteria.eq(attribute, value);
                    }
                } catch (IllegalAccessException e) {
                    LOG.warning(String.format("Could not get value from field %s of entity %s.", field.getName(), example.getClass().getName()));
                }
            }
        }
        return criteria;
    }


    /**
     * @param example An entity whose attribute's value will be used for creating a criteria
     * @param usingAttributes attributes from example entity to consider. If no attribute is provided then a no restriction will be added.
     *
     * @return A criteria restricted by example using 'likeIgnoreCase' for comparing attributes
     */
    public Criteria exampleLike(T example, SingularAttribute<T, String>... usingAttributes) {
       return exampleLike(criteria(),example,usingAttributes);
    }

    /**
     * @param criteria a pre populated criteria
     * @param example An entity whose attribute's value will be used for creating a criteria
     * @param usingAttributes attributes from example entity to consider. If no attribute is provided then a no restriction will be added.
     *
     * @return A criteria restricted by example using 'likeIgnoreCase' for comparing attributes
     */
    public Criteria exampleLike(Criteria criteria, T example, SingularAttribute<T, String>... usingAttributes) {

        for (SingularAttribute<T, ?> attribute : usingAttributes) {
            if (attribute.getJavaMember() instanceof Field) {
                Field field = (Field) attribute.getJavaMember();
                field.setAccessible(true);
                try {

                    Object value = field.get(example);
                    if (value != null) {
                        LOG.fine(String.format("Adding restriction by example on attribute %s using value %s.", attribute.getName(), value));
                        criteria.likeIgnoreCase(attribute, value.toString());
                    }
                } catch (IllegalAccessException e) {
                    LOG.warning(String.format("Could not get value from field %s of entity %s.", field.getName(), example.getClass().getName()));
                }
            }
        }
        return criteria;
    }


    /**
     * @param example An entity whose attribute's value will be used for creating a criteria
     * @param usingAttributes attributes from example entity to consider. If no attribute is provided then a no restriction will be added.
     *
     * @return A criteria restricted by example using 'in' for comparing each element of list attributes
     */
    public Criteria example(T example, ListAttribute<T, ?>... usingAttributes) {
       return example(criteria(),example,usingAttributes);
    }

    /**
     * @param criteria a criteria to add restrictions based on the example entity.
     * @param example An entity whose attribute's value will be used for creating a criteria
     * @param usingAttributes attributes from example entity to consider. If no attribute is provided then a no restriction will be added.
     *
     * @return A criteria restricted by example using 'in'for comparing each element of list attributes
     */
    public  Criteria example(Criteria<T,?> criteria, T example, ListAttribute<T, ?>... usingAttributes) {

         if(usingAttributes == null || usingAttributes.length == 0) {
                throw new RuntimeException("Please provide attributes to example criteria.");
            }

            if(criteria == null){
                criteria = criteria();
            }

            for (ListAttribute<T, ?> attribute : usingAttributes) {
                Class joinClass = attribute.getElementType().getJavaType();
                Criteria joinCriteria = where(joinClass, JoinType.LEFT);
                criteria.join(attribute, joinCriteria);
            if (attribute.getJavaMember() instanceof Field) {
                Field field = (Field) attribute.getJavaMember();
                field.setAccessible(true);
                try {
                    Object value = field.get(example);
                    if (value != null) {
                        LOG.fine(String.format("Adding restriction by example on attribute %s using value %s.", attribute.getName(), value));
                        List<PersistenceEntity> association = (List<PersistenceEntity>) value;
                        SingularAttribute id = getEntityManager().getMetamodel().entity(attribute.getElementType().getJavaType()).getId(association.get(0).getId().getClass());
                        List<Serializable> ids = new ArrayList<>();
                        for (PersistenceEntity persistenceEntity : association) {
                             ids.add(persistenceEntity.getId());
                        }

                       joinCriteria.in(id, ids);
                    }
                } catch (IllegalAccessException e) {
                    LOG.warning(String.format("Could not get value from field %s of entity %s.", field.getName(), example.getClass().getName()));
                }
            }
        }
        return criteria;
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
