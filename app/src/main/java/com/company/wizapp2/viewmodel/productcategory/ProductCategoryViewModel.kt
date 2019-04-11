package com.company.wizapp2.viewmodel.productcategory

import android.app.Application
import android.os.Parcelable

import com.company.wizapp2.viewmodel.EntityViewModel
import com.sap.cloud.android.odata.espmcontainer.ProductCategory
import com.sap.cloud.android.odata.espmcontainer.ESPMContainerMetadata.EntitySets

/*
 * Represents View model for ProductCategory
 *
 * Having an entity view model for each <T> allows the ViewModelProvider to cache and return the view model of that
 * type. This is because the ViewModelStore of ViewModelProvider cannot not be able to tell the difference between
 * EntityViewModel<type1> and EntityViewModel<type2>.
 */
class ProductCategoryViewModel(application: Application): EntityViewModel<ProductCategory>(application, EntitySets.productCategories, ProductCategory.categoryName) {
    /**
     * Constructor for a specific view model with navigation data.
     * @param [navigationPropertyName] - name of the navigation property
     * @param [entityData] - parent entity (starting point of the navigation)
     */
    constructor(application: Application, navigationPropertyName: String, entityData: Parcelable): this(application) {
        EntityViewModel<ProductCategory>(application, EntitySets.productCategories, ProductCategory.categoryName, navigationPropertyName, entityData)
    }
}
