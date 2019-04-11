package com.company.wizapp2.mdui

import android.os.Bundle
import android.view.LayoutInflater
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem

import java.util.ArrayList
import java.util.HashMap
import com.company.wizapp2.mdui.customers.CustomersListActivity
import com.company.wizapp2.mdui.productcategories.ProductCategoriesListActivity
import com.company.wizapp2.mdui.producttexts.ProductTextsListActivity
import com.company.wizapp2.mdui.products.ProductsListActivity
import com.company.wizapp2.mdui.purchaseorderheaders.PurchaseOrderHeadersListActivity
import com.company.wizapp2.mdui.purchaseorderitems.PurchaseOrderItemsListActivity
import com.company.wizapp2.mdui.salesorderheaders.SalesOrderHeadersListActivity
import com.company.wizapp2.mdui.salesorderitems.SalesOrderItemsListActivity
import com.company.wizapp2.mdui.stock.StockListActivity
import com.company.wizapp2.mdui.suppliers.SuppliersListActivity
import org.slf4j.LoggerFactory
import com.company.wizapp2.R

import kotlinx.android.synthetic.main.activity_entity_list.*
import kotlinx.android.synthetic.main.entity_list_element.view.*

/*
 * An activity to display the list of all entity types from the OData service
 */
class EntitySetListActivity : AppCompatActivity() {
    private val entitySetNames = ArrayList<String>()
    private val entitySetNameMap = HashMap<String, EntitySetName>()


    enum class EntitySetName constructor(val entitySetName: String, val titleId: Int, val iconId: Int) {
        Customers("Customers", R.string.eset_customers,
            BLUE_ANDROID_ICON),
        ProductCategories("ProductCategories", R.string.eset_productcategories,
            WHITE_ANDROID_ICON),
        ProductTexts("ProductTexts", R.string.eset_producttexts,
            BLUE_ANDROID_ICON),
        Products("Products", R.string.eset_products,
            WHITE_ANDROID_ICON),
        PurchaseOrderHeaders("PurchaseOrderHeaders", R.string.eset_purchaseorderheaders,
            BLUE_ANDROID_ICON),
        PurchaseOrderItems("PurchaseOrderItems", R.string.eset_purchaseorderitems,
            WHITE_ANDROID_ICON),
        SalesOrderHeaders("SalesOrderHeaders", R.string.eset_salesorderheaders,
            BLUE_ANDROID_ICON),
        SalesOrderItems("SalesOrderItems", R.string.eset_salesorderitems,
            WHITE_ANDROID_ICON),
        Stock("Stock", R.string.eset_stock,
            BLUE_ANDROID_ICON),
        Suppliers("Suppliers", R.string.eset_suppliers,
            WHITE_ANDROID_ICON)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entity_list)
        val toolbar = findViewById<Toolbar>(R.id.toolbar) // to avoid ambiguity
        setSupportActionBar(toolbar)

        entitySetNames.clear()
        entitySetNameMap.clear()
        for (entitySet in EntitySetName.values()) {
            val entitySetTitle = resources.getString(entitySet.titleId)
            entitySetNames.add(entitySetTitle)
            entitySetNameMap[entitySetTitle] = entitySet
        }

        val listView = entity_list
        val adapter = EntitySetListAdapter(this, R.layout.entity_list_element, entitySetNames)

        listView.adapter = adapter

        listView.setOnItemClickListener listView@{ _, _, position, _ ->
            val entitySetName = entitySetNameMap[adapter.getItem(position)!!]
            val context = this@EntitySetListActivity
            val intent: Intent = when (entitySetName) {
                EntitySetListActivity.EntitySetName.Customers -> Intent(context, CustomersListActivity::class.java)
                EntitySetListActivity.EntitySetName.ProductCategories -> Intent(context, ProductCategoriesListActivity::class.java)
                EntitySetListActivity.EntitySetName.ProductTexts -> Intent(context, ProductTextsListActivity::class.java)
                EntitySetListActivity.EntitySetName.Products -> Intent(context, ProductsListActivity::class.java)
                EntitySetListActivity.EntitySetName.PurchaseOrderHeaders -> Intent(context, PurchaseOrderHeadersListActivity::class.java)
                EntitySetListActivity.EntitySetName.PurchaseOrderItems -> Intent(context, PurchaseOrderItemsListActivity::class.java)
                EntitySetListActivity.EntitySetName.SalesOrderHeaders -> Intent(context, SalesOrderHeadersListActivity::class.java)
                EntitySetListActivity.EntitySetName.SalesOrderItems -> Intent(context, SalesOrderItemsListActivity::class.java)
                EntitySetListActivity.EntitySetName.Stock -> Intent(context, StockListActivity::class.java)
                EntitySetListActivity.EntitySetName.Suppliers -> Intent(context, SuppliersListActivity::class.java)
                else -> return@listView
            }
            context.startActivity(intent)
        }
    }

    inner class EntitySetListAdapter internal constructor(context: Context, resource: Int, entitySetNames: List<String>)
                    : ArrayAdapter<String>(context, resource, entitySetNames) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView
            val entitySetName = entitySetNameMap[getItem(position)!!]
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.entity_list_element, parent, false)
            }
            val entitySetCell = view!!.entity_set_name
            entitySetCell.headline = entitySetName!!.entitySetName
            entitySetCell.setDetailImage(entitySetName.iconId)
            return view
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, SETTINGS_SCREEN_ITEM, 0, R.string.menu_item_settings)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        LOGGER.debug("onOptionsItemSelected: " + item.title)
        return when (item.itemId) {
            SETTINGS_SCREEN_ITEM -> {
                LOGGER.debug("settings screen menu item selected.")
                val intent = Intent(this, SettingsActivity::class.java)
                this.startActivityForResult(intent, SETTINGS_SCREEN_ITEM)
                true
            }
            else -> false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        LOGGER.debug("EntitySetListActivity::onActivityResult, request code: $requestCode result code: $resultCode")
        if (requestCode == SETTINGS_SCREEN_ITEM) {
            LOGGER.debug("Calling AppState to retrieve settings after settings screen is closed.")
        }
    }

    companion object {
        private const val SETTINGS_SCREEN_ITEM = 200
        private val LOGGER = LoggerFactory.getLogger(EntitySetListActivity::class.java)
        private const val BLUE_ANDROID_ICON = R.drawable.ic_android_blue
        private const val WHITE_ANDROID_ICON = R.drawable.ic_android_white
    }
}
