package com.github.adminfaces.persistence.util;

import com.github.adminfaces.persistence.model.AdminMultiSort;
import com.github.adminfaces.persistence.model.AdminSort;
import com.github.adminfaces.persistence.model.Filter;
import com.github.adminfaces.persistence.model.PersistenceEntity;
import com.github.adminfaces.persistence.service.CrudService;
import java.util.ArrayList;

import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.primefaces.model.SortMeta;
import static org.primefaces.model.SortOrder.ASCENDING;

public class AdminDataModel<T extends PersistenceEntity> extends LazyDataModel<T> {

    private CrudService<T, ?> crudService;
    private Filter<T> filter;
    private boolean keepFiltersInSession;

    public AdminDataModel(CrudService<T, ?> crudService, Filter<T> filter) {
        this(crudService, filter, true);
    }

    public AdminDataModel(CrudService<T, ?> crudService, Filter<T> filter, boolean keepFiltersInSession) {
        this.crudService = crudService;
        this.filter = filter;
        this.keepFiltersInSession = keepFiltersInSession;
    }

    @Override
    public List<T> load(int first, int pageSize, String sortField, SortOrder sortOrder,
        Map<String, FilterMeta> filters) {
        Map<String, SortMeta> multiSortMeta = new HashMap<>();
        multiSortMeta.put(sortField, new SortMeta(null, sortField, sortOrder, null));
        return load(first, pageSize, multiSortMeta, filters);
    }

    @Override
    public List<T> load(int first, int pageSize, Map<String, SortMeta> multiSortMeta, Map<String, FilterMeta> filters) {
        List<AdminMultiSort> adminMultiSort = new ArrayList<>();
        if (multiSortMeta != null && !multiSortMeta.isEmpty()) {
            for (SortMeta sortMeta : multiSortMeta.values()) {
                AdminSort adminSort = AdminSort.UNSORTED;
                if (ASCENDING.equals(sortMeta.getSortOrder())) {
                    adminSort = AdminSort.ASCENDING;
                } else if (SortOrder.DESCENDING.equals(sortMeta.getSortOrder())) {
                    adminSort = AdminSort.DESCENDING;
                }
                adminMultiSort.add(new AdminMultiSort(adminSort, sortMeta.getSortField()));
            }
        }
        if ((filters == null || filters.isEmpty()) && keepFiltersInSession) {
            filters = filter.getPrimeFilterParams();
        }

        filter.setFirst(first).setPageSize(pageSize)
            .setMultiSort(adminMultiSort)
            .setParams(toObjectMap(filters));
        List<T> list = crudService.paginate(filter);
        setRowCount(crudService.count(filter).intValue());
        return list;

    }

    private Map<String, Object> toObjectMap(Map<String, FilterMeta> filters) {
        final Map<String, Object> objectMap = new HashMap<>();
        for (Map.Entry<String, FilterMeta> entry : filters.entrySet()) {
            objectMap.put(entry.getKey(), entry.getValue().getFilterValue());
        }
        return objectMap;
    }

    @Override
    public int getRowCount() {
        return super.getRowCount();
    }

    @Override
    public T getRowData(String key) {
        List<T> list = (List<T>) this.getWrappedData();
        if (list != null && !list.isEmpty()) {
            for (T t : list) {
                if (key.equals(t.getId().toString())) {
                    return t;
                }
            }
        }
        return null;
    }

    public void setFilter(Filter<T> filter) {
        this.filter = filter;
    }
}
