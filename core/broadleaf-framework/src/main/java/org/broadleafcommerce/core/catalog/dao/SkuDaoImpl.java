/*
 * #%L
 * BroadleafCommerce Framework
 * %%
 * Copyright (C) 2009 - 2013 Broadleaf Commerce
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.broadleafcommerce.core.catalog.dao;

import org.broadleafcommerce.common.persistence.EntityConfiguration;
import org.broadleafcommerce.common.time.SystemTime;
import org.broadleafcommerce.common.util.DateUtil;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.domain.SkuFee;
import org.broadleafcommerce.core.catalog.domain.SkuImpl;
import org.hibernate.ejb.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * {@inheritDoc}
 *
 * @author Jeff Fischer
 */
@Repository("blSkuDao")
public class SkuDaoImpl implements SkuDao {

    @PersistenceContext(unitName="blPU")
    protected EntityManager em;

    @Resource(name="blEntityConfiguration")
    protected EntityConfiguration entityConfiguration;

    protected Long currentDateResolution = 10000L;
    protected Date cachedDate = SystemTime.asDate();

    @Override
    public Sku save(Sku sku) {
        return em.merge(sku);
    }
    
    @Override
    public SkuFee saveSkuFee(SkuFee fee) {
        return em.merge(fee);
    }

    @Override
    public Sku readSkuById(Long skuId) {
        return (Sku) em.find(SkuImpl.class, skuId);
    }

    @Override
    public Sku readFirstSku() {
        TypedQuery<Sku> query = em.createNamedQuery("BC_READ_FIRST_SKU", Sku.class);
        return query.getSingleResult();
    }

    @Override
    public List<Sku> readAllSkus() {
        TypedQuery<Sku> query = em.createNamedQuery("BC_READ_ALL_SKUS", Sku.class);
        return query.getResultList();
    }

    @Override
    public List<Sku> readSkusByIds(List<Long> skuIds) {
        if (skuIds == null || skuIds.size() == 0) {
            return null;
        }
        TypedQuery<Sku> query = em.createNamedQuery("BC_READ_SKUS_BY_IDS", Sku.class);
        query.setParameter("skuIds", skuIds);
        return query.getResultList();
    }

    @Override
    public void delete(Sku sku){
        if (!em.contains(sku)) {
            sku = readSkuById(sku.getId());
        }
        em.remove(sku);     
    }

    @Override
    public Sku create() {
        return (Sku) entityConfiguration.createEntityInstance(Sku.class.getName());
    }

    @Override
    public Long readCountAllActiveSkus() {
        Date currentDate = DateUtil.getCurrentDateAfterFactoringInDateResolution(cachedDate, currentDateResolution);
        return readCountAllActiveSkusInternal(currentDate);
    }

    protected Long readCountAllActiveSkusInternal(Date currentDate) {
        // Set up the criteria query that specifies we want to return a Long
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);

        // The root of our search is sku
        Root<SkuImpl> sku = criteria.from(SkuImpl.class);

        // We want the count of products
        criteria.select(builder.count(sku));

        // Ensure the sku is currently active
        List<Predicate> restrictions = new ArrayList<Predicate>();

        // Add the active start/end date restrictions
        restrictions.add(builder.lessThan(sku.get("activeStartDate").as(Date.class), currentDate));
        restrictions.add(builder.or(
                builder.isNull(sku.get("activeEndDate")),
                builder.greaterThan(sku.get("activeEndDate").as(Date.class), currentDate)));

        // Add the restrictions to the criteria query
        criteria.where(restrictions.toArray(new Predicate[restrictions.size()]));

        TypedQuery<Long> query = em.createQuery(criteria);
        query.setHint(QueryHints.HINT_CACHEABLE, true);
        query.setHint(QueryHints.HINT_CACHE_REGION, "query.Catalog");

        return query.getSingleResult();
    }

    @Override
    public List<Sku> readAllActiveSkus(int page, int pageSize) {
        Date currentDate = DateUtil.getCurrentDateAfterFactoringInDateResolution(cachedDate, currentDateResolution);
        return readAllActiveSkusInternal(page, pageSize, currentDate);
    }

    protected List<Sku> readAllActiveSkusInternal(int page, int pageSize, Date currentDate) {
        // Set up the criteria query that specifies we want to return Products
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Sku> criteria = builder.createQuery(Sku.class);

        // The root of our search is Product
        Root<SkuImpl> sku = criteria.from(SkuImpl.class);

        // Product objects are what we want back
        criteria.select(sku);

        // Ensure the product is currently active
        List<Predicate> restrictions = new ArrayList<Predicate>();

        // Add the active start/end date restrictions
        restrictions.add(builder.lessThan(sku.get("activeStartDate").as(Date.class), currentDate));
        restrictions.add(builder.or(
                builder.isNull(sku.get("activeEndDate")),
                builder.greaterThan(sku.get("activeEndDate").as(Date.class), currentDate)));

        // Add the restrictions to the criteria query
        criteria.where(restrictions.toArray(new Predicate[restrictions.size()]));

        int firstResult = page * pageSize;
        TypedQuery<Sku> query = em.createQuery(criteria);
        query.setHint(QueryHints.HINT_CACHEABLE, true);
        query.setHint(QueryHints.HINT_CACHE_REGION, "query.Catalog");

        return query.setFirstResult(firstResult).setMaxResults(pageSize).getResultList();
    }

    @Override
    public Long getCurrentDateResolution() {
        return currentDateResolution;
    }

    @Override
    public void setCurrentDateResolution(Long currentDateResolution) {
        this.currentDateResolution = currentDateResolution;
    }

}
