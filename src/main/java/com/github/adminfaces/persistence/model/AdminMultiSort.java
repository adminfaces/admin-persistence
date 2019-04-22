/*
 * The MIT License
 *
 * Copyright 2019 rmpestano.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.adminfaces.persistence.model;

import java.util.Objects;

/**
 *
 * @author rmpestano
 */
public class AdminMultiSort {
    
    private final AdminSort adminSort;
    private final String sortField;

    public AdminMultiSort(AdminSort adminSort, String sortField) {
        this.adminSort = adminSort;
        this.sortField = sortField;
    }

    public AdminSort getAdminSort() {
        return adminSort;
    }

    public String getSortField() {
        return sortField;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AdminMultiSort other = (AdminMultiSort) obj;
        if (!Objects.equals(this.sortField, other.sortField)) {
            return false;
        }
        if (this.adminSort != other.adminSort) {
            return false;
        }
        return true;
    }
    
    
    
}
