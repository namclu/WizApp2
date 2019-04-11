package com.company.wizapp2.repository

import com.company.wizapp2.service.SAPServiceManager

import com.sap.cloud.android.odata.espmcontainer.ESPMContainerMetadata.EntitySets
import com.sap.cloud.android.odata.espmcontainer.Customer
import com.sap.cloud.android.odata.espmcontainer.ProductCategory
import com.sap.cloud.android.odata.espmcontainer.ProductText
import com.sap.cloud.android.odata.espmcontainer.Product
import com.sap.cloud.android.odata.espmcontainer.PurchaseOrderHeader
import com.sap.cloud.android.odata.espmcontainer.PurchaseOrderItem
import com.sap.cloud.android.odata.espmcontainer.SalesOrderHeader
import com.sap.cloud.android.odata.espmcontainer.SalesOrderItem
import com.sap.cloud.android.odata.espmcontainer.Stock
import com.sap.cloud.android.odata.espmcontainer.Supplier

import com.sap.cloud.mobile.odata.EntitySet
import com.sap.cloud.mobile.odata.EntityValue
import com.sap.cloud.mobile.odata.Property

import java.util.WeakHashMap

/*
 * Repository factory to construct repository for an entity set
 */
class RepositoryFactory
/**
 * Construct a RepositoryFactory instance. There should only be one repository factory and used
 * throughout the life of the application to avoid caching entities multiple times.
 * @param sapServiceManager - Service manager for interaction with OData service
 */
(private val sapServiceManager: SAPServiceManager) {
    private val repositories: WeakHashMap<String, Repository<out EntityValue>> = WeakHashMap()

    /**
     * Construct or return an existing repository for the specified entity set
     * @param entitySet - entity set for which the repository is to be returned
     * @param orderByProperty - if specified, collection will be sorted ascending with this property
     * @return a repository for the entity set
     */
    fun getRepository(entitySet: EntitySet, orderByProperty: Property?): Repository<out EntityValue> {
        val eSPMContainer = sapServiceManager.eSPMContainer
        val key = entitySet.localName
        var repository: Repository<out EntityValue>? = repositories[key]
        if (repository == null) {
            repository = when (key) {
                EntitySets.customers.localName -> Repository<Customer>(eSPMContainer!!, EntitySets.customers, orderByProperty)
                EntitySets.productCategories.localName -> Repository<ProductCategory>(eSPMContainer!!, EntitySets.productCategories, orderByProperty)
                EntitySets.productTexts.localName -> Repository<ProductText>(eSPMContainer!!, EntitySets.productTexts, orderByProperty)
                EntitySets.products.localName -> Repository<Product>(eSPMContainer!!, EntitySets.products, orderByProperty)
                EntitySets.purchaseOrderHeaders.localName -> Repository<PurchaseOrderHeader>(eSPMContainer!!, EntitySets.purchaseOrderHeaders, orderByProperty)
                EntitySets.purchaseOrderItems.localName -> Repository<PurchaseOrderItem>(eSPMContainer!!, EntitySets.purchaseOrderItems, orderByProperty)
                EntitySets.salesOrderHeaders.localName -> Repository<SalesOrderHeader>(eSPMContainer!!, EntitySets.salesOrderHeaders, orderByProperty)
                EntitySets.salesOrderItems.localName -> Repository<SalesOrderItem>(eSPMContainer!!, EntitySets.salesOrderItems, orderByProperty)
                EntitySets.stock.localName -> Repository<Stock>(eSPMContainer!!, EntitySets.stock, orderByProperty)
                EntitySets.suppliers.localName -> Repository<Supplier>(eSPMContainer!!, EntitySets.suppliers, orderByProperty)
                else -> throw AssertionError("Fatal error, entity set[$key] missing in generated code")
            }
            repositories[key] = repository
        }
        return repository
    }

    /**
     * Get rid of all cached repositories
     */
    fun reset() {
        repositories.clear()
    }
}
