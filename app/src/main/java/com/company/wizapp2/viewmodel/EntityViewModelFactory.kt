package com.company.wizapp2.viewmodel

import android.app.Application
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.os.Parcelable

import com.company.wizapp2.viewmodel.customer.CustomerViewModel
import com.company.wizapp2.viewmodel.productcategory.ProductCategoryViewModel
import com.company.wizapp2.viewmodel.producttext.ProductTextViewModel
import com.company.wizapp2.viewmodel.product.ProductViewModel
import com.company.wizapp2.viewmodel.purchaseorderheader.PurchaseOrderHeaderViewModel
import com.company.wizapp2.viewmodel.purchaseorderitem.PurchaseOrderItemViewModel
import com.company.wizapp2.viewmodel.salesorderheader.SalesOrderHeaderViewModel
import com.company.wizapp2.viewmodel.salesorderitem.SalesOrderItemViewModel
import com.company.wizapp2.viewmodel.stock.StockViewModel
import com.company.wizapp2.viewmodel.supplier.SupplierViewModel

/**
 * Custom factory class, which can create view models for entity subsets, which are
 * reached from a parent entity through a navigation property.
 *
 * @param application parent application
 * @param navigationPropertyName name of the navigation link
 * @param entityData parent entity
 */
class EntityViewModelFactory (
        val application: Application, // name of the navigation property
        val navigationPropertyName: String, // parent entity
        val entityData: Parcelable) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass.simpleName) {
			"CustomerViewModel" -> CustomerViewModel(application, navigationPropertyName, entityData) as T
            			"ProductCategoryViewModel" -> ProductCategoryViewModel(application, navigationPropertyName, entityData) as T
            			"ProductTextViewModel" -> ProductTextViewModel(application, navigationPropertyName, entityData) as T
            			"ProductViewModel" -> ProductViewModel(application, navigationPropertyName, entityData) as T
            			"PurchaseOrderHeaderViewModel" -> PurchaseOrderHeaderViewModel(application, navigationPropertyName, entityData) as T
            			"PurchaseOrderItemViewModel" -> PurchaseOrderItemViewModel(application, navigationPropertyName, entityData) as T
            			"SalesOrderHeaderViewModel" -> SalesOrderHeaderViewModel(application, navigationPropertyName, entityData) as T
            			"SalesOrderItemViewModel" -> SalesOrderItemViewModel(application, navigationPropertyName, entityData) as T
            			"StockViewModel" -> StockViewModel(application, navigationPropertyName, entityData) as T
             else -> SupplierViewModel(application, navigationPropertyName, entityData) as T
        }
    }
}
