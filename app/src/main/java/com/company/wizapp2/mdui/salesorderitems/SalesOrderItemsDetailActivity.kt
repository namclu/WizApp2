package com.company.wizapp2.mdui.salesorderitems

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.company.wizapp2.R
import com.company.wizapp2.mdui.UIConstants
import com.company.wizapp2.mdui.BundleKeys
import com.sap.cloud.android.odata.espmcontainer.SalesOrderItem
import kotlinx.android.synthetic.main.activity_item_detail.*


/**
 * An activity that presents a single SalesOrderItem detail screen. This activity is only used with narrow width devices.
 * On tablet-size devices, SalesOrderItem detail is presented side-by-side with the list of SalesOrderItems in
 * a SalesOrderItemsListActivity.
 *
 * This activity can be started to handle read/display, update or create of an instance of SalesOrderItem
 * Arguments: Operation: [OP_READ | OP_UPDATE | OP_CREATE].
 *            For Read, the SalesOrderItem to display
 *            For Update, the SalesOrderItem to update
 *            For Create, a new instance of SalesOrderItem with defaults will be created
 */
class SalesOrderItemsDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_item_detail)
        setSupportActionBar(detail_toolbar)
        showUpButton()

        if (savedInstanceState == null) {
            val operation = intent.getStringExtra(BundleKeys.OPERATION)
            if (operation == null || operation == UIConstants.OP_READ) {
                //NAVIGATE DOWN
                val salesOrderItem = intent.getParcelableExtra<SalesOrderItem>(BundleKeys.ENTITY_INSTANCE)
                createFragmentForDisplay(salesOrderItem)
            } else {
                if (operation == UIConstants.OP_CREATE || operation == UIConstants.OP_UPDATE) {
                    createFragmentForCreateUpdate(operation)
                } else {
                    throw AssertionError()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                navigateUpTo(Intent(this, SalesOrderItemsListActivity::class.java))
                true
            }
            else -> false
        }
    }

    /** Show the Up button in the action bar */
    private fun showUpButton() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * Create SalesOrderItemsDetailFragment to show the details of selected SalesOrderItem
     * @param [salesOrderItem] SalesOrderItem to be displayed
     */
    private fun createFragmentForDisplay(salesOrderItem: SalesOrderItem) {
        val arguments = Bundle()
        arguments.putParcelable(BundleKeys.ENTITY_INSTANCE, salesOrderItem)
        val fragment = SalesOrderItemsDetailFragment()
        fragment.arguments = arguments
        supportFragmentManager.beginTransaction()
            .add(R.id.item_detail_container, fragment)
            .commit()
    }

    /**
     * Create SalesOrderItemsCreateFragment to create or update
     * @param [operation] indicate create or update
     */
    private fun createFragmentForCreateUpdate(operation: String) {
        val arguments = Bundle()
        arguments.putSerializable(BundleKeys.OPERATION, operation)
        if (operation == UIConstants.OP_UPDATE) {
            val salesOrderItem = intent.getParcelableExtra<SalesOrderItem>(BundleKeys.ENTITY_INSTANCE)
            arguments.putParcelable(BundleKeys.ENTITY_INSTANCE, salesOrderItem)
        }
        val fragment = SalesOrderItemsCreateFragment()
        fragment.arguments = arguments
        supportFragmentManager.beginTransaction()
            .add(R.id.item_detail_container, fragment)
            .commit()
    }
}
