package com.company.wizapp2.viewmodel.stock

import android.app.Application
import android.os.Parcelable

import com.company.wizapp2.viewmodel.EntityViewModel
import com.sap.cloud.android.odata.espmcontainer.Stock
import com.sap.cloud.android.odata.espmcontainer.ESPMContainerMetadata.EntitySets

/*
 * Represents View model for Stock
 *
 * Having an entity view model for each <T> allows the ViewModelProvider to cache and return the view model of that
 * type. This is because the ViewModelStore of ViewModelProvider cannot not be able to tell the difference between
 * EntityViewModel<type1> and EntityViewModel<type2>.
 */
class StockViewModel(application: Application): EntityViewModel<Stock>(application, EntitySets.stock, Stock.lotSize) {
    /**
     * Constructor for a specific view model with navigation data.
     * @param [navigationPropertyName] - name of the navigation property
     * @param [entityData] - parent entity (starting point of the navigation)
     */
    constructor(application: Application, navigationPropertyName: String, entityData: Parcelable): this(application) {
        EntityViewModel<Stock>(application, EntitySets.stock, Stock.lotSize, navigationPropertyName, entityData)
    }
}
