package com.company.wizapp2.mdui.purchaseorderheaders

import android.content.Intent
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.company.wizapp2.service.SAPServiceManager
import com.company.wizapp2.mediaresource.EntityMediaResource
import com.company.wizapp2.R
import com.company.wizapp2.app.ErrorHandler
import com.company.wizapp2.app.ErrorMessage
import com.company.wizapp2.app.SAPWizardApplication
import com.company.wizapp2.databinding.PurchaseorderheadersDetailBinding
import com.company.wizapp2.mdui.BundleKeys
import com.company.wizapp2.mdui.EntityDeleteDialog
import com.company.wizapp2.mdui.UIConstants
import com.company.wizapp2.mdui.EntityKeyUtil
import com.company.wizapp2.repository.OperationResult
import com.company.wizapp2.viewmodel.purchaseorderheader.PurchaseOrderHeaderViewModel
import com.sap.cloud.android.odata.espmcontainer.PurchaseOrderHeader
import com.sap.cloud.mobile.fiori.indicator.FioriProgressBar
import com.sap.cloud.mobile.fiori.`object`.ObjectHeader
import kotlinx.android.synthetic.main.activity_item_detail.*
import kotlinx.android.synthetic.main.activity_item_detail.view.*

import com.company.wizapp2.mdui.purchaseorderitems.PurchaseOrderItemsListActivity
import com.company.wizapp2.mdui.suppliers.SuppliersListActivity

/**
 * A fragment representing a single PurchaseOrderHeader detail screen. This fragment is either contained in an
 * PurchaseOrderHeadersListActivity in two-pane mode (on tablets) or an PurchaseOrderHeadersDetailActivity (on phone).
 */
class PurchaseOrderHeadersDetailFragment : Fragment() {
    /* Generated data binding class based on layout file */
    private lateinit var binding: PurchaseorderheadersDetailBinding

    /* PurchaseOrderHeader entity to be displayed */
    private lateinit var purchaseOrderHeader: PurchaseOrderHeader

    /* Fiori ObjectHeader component used when entity is to be displayed on phone */
    private var objectHeader: ObjectHeader? = null

    /* View model of the entity type that the displayed entity belongs to */
    private lateinit var viewModel: PurchaseOrderHeaderViewModel

    /* Fiori progress bar for busy indication if either update or delete action is clicked upon */
    private var progressBar: FioriProgressBar? = null

    /* Error handler to display message should error occurs */
    private lateinit var errorHandler: ErrorHandler

    /*
     * Service manager to provide root URL of OData Service for Glide to load images if there are media resources
     * associated with the entity type
     */
    private lateinit var sapServiceManager: SAPServiceManager

    /* Used to set the toolbar title in tablet mode */
    private var activityTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sapServiceManager = (activity!!.application as SAPWizardApplication).sapServiceManager
        errorHandler = (activity!!.application as SAPWizardApplication).errorHandler

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val activity = activity ?: return null
        viewModel = ViewModelProviders.of(this).get(PurchaseOrderHeaderViewModel::class.java)

        val bundle = arguments
        if (bundle != null && bundle.containsKey(BundleKeys.ENTITY_INSTANCE)) {
            purchaseOrderHeader = bundle.getParcelable(BundleKeys.ENTITY_INSTANCE)!!
            if (this.activity is PurchaseOrderHeadersListActivity) {
                activityTitle = purchaseOrderHeader.entityType.localName
                activity.invalidateOptionsMenu()
            } else {
                activity.title = purchaseOrderHeader.entityType.localName
            }
            setupObjectHeader(purchaseOrderHeader)
        }

        // In two pane mode, PurchaseOrderHeadersListActivity is already receiving delete completion callback
        if (!getActivity()!!.resources.getBoolean(R.bool.two_pane)) {
            viewModel.deleteResult.observe(viewLifecycleOwner,
                Observer { result -> onDeleteComplete(result!!) })
        }

        return setupDataBinding(inflater, container)
    }

    override fun onDestroy() {
        super.onDestroy()
        val secondaryToolbar: Toolbar? = activity?.findViewById(R.id.secondaryToolbar)
        if (secondaryToolbar != null) {
            secondaryToolbar.title = ""
            secondaryToolbar.menu.clear()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val secondaryToolbar: Toolbar? = activity?.findViewById(R.id.secondaryToolbar)
        if (this.activity is PurchaseOrderHeadersListActivity) {
            secondaryToolbar?.let {
                it.menu.clear()
                it.inflateMenu(R.menu.itemlist_view_options)
                it.title = activityTitle
                it.setOnMenuItemClickListener { menuItem -> onOptionsItemSelected(menuItem) }
            }
        } else {
            inflater.inflate(R.menu.itemlist_view_options, menu)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.update_item -> {
                showCreateFragmentForUpdate()
                return true
            }

            R.id.delete_item -> {
                viewModel.addSelected(purchaseOrderHeader)
                val dDialog = EntityDeleteDialog(activity!!, viewModel)
                dDialog.confirmDelete()
                return true
            }

            else ->
                // User didn't trigger any button listed above, let the superclass handle this action
                return super.onOptionsItemSelected(item)
        }
    }
    
    @Suppress("UNUSED_PARAMETER") 
    fun onNavigationClickedToPurchaseOrderItems_Items(view: View) {
        val intent = Intent(this.activity, PurchaseOrderItemsListActivity::class.java)
        intent.putExtra("parent",  purchaseOrderHeader)
        intent.putExtra("navigation", "Items")
        startActivity(intent)
    }

    @Suppress("UNUSED_PARAMETER") 
    fun onNavigationClickedToSuppliers_SupplierDetails(view: View) {
        val intent = Intent(this.activity, SuppliersListActivity::class.java)
        intent.putExtra("parent",  purchaseOrderHeader)
        intent.putExtra("navigation", "SupplierDetails")
        startActivity(intent)
    }


    /* Completion callback for delete operation */
    private fun onDeleteComplete(result: OperationResult<PurchaseOrderHeader>) {
        val activity = activity ?: return

        if (progressBar == null) {
            progressBar = activity.window.decorView.indeterminateBar
        }
        progressBar!!.visibility = View.INVISIBLE
        val ex = result.error
        if (ex != null) {
            handleError(ex)
            return
        }
        if (!resources.getBoolean(R.bool.two_pane)) {
            getActivity()!!.finish()
        } else {
            // Remove this as the entity is already deleted
            getActivity()!!.supportFragmentManager.beginTransaction().remove(this).commit()
        }
    }

    /**
     * Setup ObjectHeader with an instance of PurchaseOrderHeader
     *
     * @param [purchaseOrderHeader] to be displayed in ObjectHeader
     */
    private fun setupObjectHeader(purchaseOrderHeader: PurchaseOrderHeader) {
        // Object Header is not available in tablet mode
        val activity = activity
        objectHeader = activity!!.objectHeader
        val dataValue = purchaseOrderHeader.getDataValue(PurchaseOrderHeader.currencyCode)
        objectHeader?.headline = dataValue?.toString()

        // EntityKey in string format: '{"key":value,"key2":value2}'
        objectHeader?.subheadline = EntityKeyUtil.getOptionalEntityKey(purchaseOrderHeader)

        objectHeader?.setTag("#tag1", 0)
        objectHeader?.setTag("#tag3", 2)
        objectHeader?.setTag("#tag2", 1)

        objectHeader?.body = "You can set the header body text here."
        objectHeader?.footnote = "You can set the header footnote here."
        objectHeader?.description = "You can add a detailed item description here."

        // Glide offers caching in addition to fetching the images
        objectHeader?.prepareDetailImageView()?.scaleType = ImageView.ScaleType.FIT_CENTER

        activity.let { _activity ->
            objectHeader?.let { objectHeader ->
            Glide.with(_activity)
                .load(EntityMediaResource.getMediaResourceUrl(purchaseOrderHeader, sapServiceManager.serviceRoot))
                .apply(RequestOptions().fitCenter())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(objectHeader.prepareDetailImageView())

            }
        }
    }

    /**
     * Set up databinding for this view
     *
     * @param [inflater] layout inflater from onCreateView
     * @param [container] view group from onCreateView
     * @return [View] rootView from generated databinding code
     */
    private fun setupDataBinding(inflater: LayoutInflater, container: ViewGroup?): View {
        binding = PurchaseorderheadersDetailBinding.inflate(inflater, container, false)
        val rootView = binding.root
        binding.setPurchaseOrderHeader(purchaseOrderHeader)
        binding.handler = this
        return rootView
    }

    /**
     * Notify user of error encountered during operation execution
     *
     * @param [exception] - exception encountered
     */
    private fun handleError(exception: Exception) {
        val errorMessage = ErrorMessage(resources.getString(R.string.delete_failed),
                resources.getString(R.string.delete_failed_detail), exception, false)
        errorHandler.sendErrorMessage(errorMessage)
    }

    private fun showCreateFragmentForUpdate() {
        val arguments = Bundle()
        arguments.putSerializable(BundleKeys.OPERATION, UIConstants.OP_UPDATE)
        arguments.putParcelable(BundleKeys.ENTITY_INSTANCE, purchaseOrderHeader)

        val fragment = PurchaseOrderHeadersCreateFragment()
        fragment.arguments = arguments
        val fragmentId = if (activity!!.item_detail_container != null) {
            R.id.item_detail_container
        } else {
            R.id.fragment_item_detail
        }

        activity!!.supportFragmentManager.beginTransaction()
            .replace(fragmentId, fragment, UIConstants.MODIFY_FRAGMENT_TAG)
            .commit()
    }
}
