package com.company.wizapp2.mdui.producttexts

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.company.wizapp2.R
import com.company.wizapp2.mdui.UIConstants
import com.company.wizapp2.mdui.BundleKeys
import com.sap.cloud.android.odata.espmcontainer.ProductText
import kotlinx.android.synthetic.main.activity_item_detail.*


/**
 * An activity that presents a single ProductText detail screen. This activity is only used with narrow width devices.
 * On tablet-size devices, ProductText detail is presented side-by-side with the list of ProductTexts in
 * a ProductTextsListActivity.
 *
 * This activity can be started to handle read/display, update or create of an instance of ProductText
 * Arguments: Operation: [OP_READ | OP_UPDATE | OP_CREATE].
 *            For Read, the ProductText to display
 *            For Update, the ProductText to update
 *            For Create, a new instance of ProductText with defaults will be created
 */
class ProductTextsDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_item_detail)
        setSupportActionBar(detail_toolbar)
        showUpButton()

        if (savedInstanceState == null) {
            val operation = intent.getStringExtra(BundleKeys.OPERATION)
            if (operation == null || operation == UIConstants.OP_READ) {
                //NAVIGATE DOWN
                val productText = intent.getParcelableExtra<ProductText>(BundleKeys.ENTITY_INSTANCE)
                createFragmentForDisplay(productText)
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
                navigateUpTo(Intent(this, ProductTextsListActivity::class.java))
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
     * Create ProductTextsDetailFragment to show the details of selected ProductText
     * @param [productText] ProductText to be displayed
     */
    private fun createFragmentForDisplay(productText: ProductText) {
        val arguments = Bundle()
        arguments.putParcelable(BundleKeys.ENTITY_INSTANCE, productText)
        val fragment = ProductTextsDetailFragment()
        fragment.arguments = arguments
        supportFragmentManager.beginTransaction()
            .add(R.id.item_detail_container, fragment)
            .commit()
    }

    /**
     * Create ProductTextsCreateFragment to create or update
     * @param [operation] indicate create or update
     */
    private fun createFragmentForCreateUpdate(operation: String) {
        val arguments = Bundle()
        arguments.putSerializable(BundleKeys.OPERATION, operation)
        if (operation == UIConstants.OP_UPDATE) {
            val productText = intent.getParcelableExtra<ProductText>(BundleKeys.ENTITY_INSTANCE)
            arguments.putParcelable(BundleKeys.ENTITY_INSTANCE, productText)
        }
        val fragment = ProductTextsCreateFragment()
        fragment.arguments = arguments
        supportFragmentManager.beginTransaction()
            .add(R.id.item_detail_container, fragment)
            .commit()
    }
}
