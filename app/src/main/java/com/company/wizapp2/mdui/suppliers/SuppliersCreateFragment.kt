package com.company.wizapp2.mdui.suppliers

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import com.company.wizapp2.R
import com.company.wizapp2.app.ErrorHandler
import com.company.wizapp2.app.ErrorMessage
import com.company.wizapp2.app.SAPWizardApplication
import com.company.wizapp2.databinding.SuppliersCreateUpdateBinding
import com.company.wizapp2.mdui.BundleKeys
import com.company.wizapp2.mdui.UIConstants
import com.company.wizapp2.repository.OperationResult
import com.company.wizapp2.viewmodel.supplier.SupplierViewModel
import com.sap.cloud.android.odata.espmcontainer.Supplier
import com.sap.cloud.android.odata.espmcontainer.ESPMContainerMetadata.EntityTypes
import com.sap.cloud.mobile.fiori.formcell.SimplePropertyFormCell
import com.sap.cloud.mobile.fiori.indicator.FioriProgressBar
import com.sap.cloud.mobile.odata.Property
import kotlinx.android.synthetic.main.activity_item_detail.*
import kotlinx.android.synthetic.main.suppliers_create_update.*
import org.slf4j.LoggerFactory

/**
 * A fragment that is used for both update and create for users to enter values for the properties. When used for
 * update, an instance of the entity is required. In the case of create, a new instance of the entity with defaults will
 * be created. The default values may not be acceptable for the OData service.
 * This fragment is either contained in a [SuppliersListActivity] in two-pane mode (on tablets) or a
 * [SuppliersDetailActivity] on handsets.
 *
 * Arguments: Operation: [OP_CREATE | OP_UPDATE]
 *            Supplier if Operation is update
 */
class SuppliersCreateFragment : Fragment() {
    /* supplier to be created or updated */
    private lateinit var supplier: Supplier

    /* DataBinding generated class */
    private lateinit var binding: SuppliersCreateUpdateBinding

    /* Indicate what operation to be performed */
    private var operation: String? = null

    /* supplier ViewModel */
    private lateinit var viewModel: SupplierViewModel

    /* Fiori Progressbar to display background process is running. */
    private var progressBar: FioriProgressBar? = null

    /* Application error handler to report error */
    private lateinit var errorHandler: ErrorHandler

    /* Used to set the toolbar title in tablet mode */
    private var activityTitle: String = ""


    private val isSupplierValid: Boolean
        get() {
            val linearLayout: LinearLayout = create_update_supplier
            var isValid = true
            for (i in 0 until linearLayout.childCount) {
                val viewItem = linearLayout.getChildAt(i)
                val simplePropertyFormCell = viewItem as SimplePropertyFormCell
                val propertyName = simplePropertyFormCell.tag as String
                val property = EntityTypes.supplier.getProperty(propertyName)
                val value = simplePropertyFormCell.value!!.toString()
                if (!isValidProperty(property, value)) {
                    simplePropertyFormCell.setTag(R.id.TAG_HAS_MANDATORY_ERROR, true)
                    val errorMessage = resources.getString(R.string.mandatory_warning)
                    simplePropertyFormCell.isErrorEnabled = true
                    simplePropertyFormCell.error = errorMessage
                    isValid = false
                } else {
                    if (simplePropertyFormCell.isErrorEnabled) {
                        val hasMandatoryError = simplePropertyFormCell.getTag(R.id.TAG_HAS_MANDATORY_ERROR) as Boolean
                        if (!hasMandatoryError) {
                            isValid = false
                        } else {
                            simplePropertyFormCell.isErrorEnabled = false
                        }
                    }
                    simplePropertyFormCell.setTag(R.id.TAG_HAS_MANDATORY_ERROR, false)
                }
            }
            return isValid
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        errorHandler = (activity!!.application as SAPWizardApplication).errorHandler
        setHasOptionsMenu(true)

        val bundle = arguments
        if (bundle != null) {
            operation = bundle.getString(BundleKeys.OPERATION)
            supplier = if (UIConstants.OP_CREATE == operation) {
                createSupplier()
            } else {
                bundle.getParcelable(BundleKeys.ENTITY_INSTANCE)!!
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Fragment detached, do nothing.
        if (activity == null) {
            return null
        }

        hideObjectHeader()
        val rootView = setupDataBinding(inflater, container)
        viewModel = ViewModelProviders.of(this).get(SupplierViewModel::class.java)
        observeOperationCompletionEvents()
        setActivityTitle()

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val secondaryToolbar: Toolbar? = activity?.findViewById(R.id.secondaryToolbar)
        if (this.activity is SuppliersListActivity) {
            secondaryToolbar?.let {
                it.menu.clear()
                it.inflateMenu(R.menu.itemlist_edit_options)
                it.title = activityTitle
                it.setOnMenuItemClickListener(this::onOptionsItemSelected)
            }
        } else {
            inflater.inflate(R.menu.itemlist_edit_options, menu)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save_item -> {
                if (!isSupplierValid) {
                    return false
                }

                if (progressBar == null) {
                    progressBar = activity!!.indeterminateBar
                }
                progressBar!!.visibility = View.VISIBLE

                if (operation == UIConstants.OP_CREATE) {
                    viewModel.create(supplier)
                } else {
                    viewModel.update(supplier)
                }

                return true
            }
            else ->
                // User didn't trigger any button listed above, let the superclass handle this action
                return super.onOptionsItemSelected(item)
        }
    }

    /**
     * Create a new Supplier instance and initialize properties to its default values
     * Nullable property will remain null
     * @return new Supplier instance
     */
    private fun createSupplier(): Supplier {
        val supplier = Supplier(true)
        return supplier
    }

    /* Callback function to complete processing when updateResult or createResult events fired */
    private fun onComplete(result: OperationResult<Supplier>) {
        progressBar!!.visibility = View.INVISIBLE
        if (result.error != null) {
            handleError(result)
        } else {
            handleSuccessfulCompletion(result)
        }
    }

    /* Simple validation: checks the presence of mandatory fields. */
    private fun isValidProperty(property: Property, value: String): Boolean {
        var isValid = true
        if (!property.isNullable && value.isEmpty()) {
            isValid = false
        }
        return isValid
    }

    /* Hide objectHeader in the layout file shared between display and update/create fragments */
    private fun hideObjectHeader() {
        this.activity?.objectHeader?.visibility = View.GONE
    }

    /**
     * Set up data binding for this view
     *
     * @param [inflater] layout inflater from onCreateView
     * @param [container] view group from onCreateView
     *
     * @return rootView from generated data binding code
     */
    private fun setupDataBinding(inflater: LayoutInflater, container: ViewGroup?): View {
        binding = SuppliersCreateUpdateBinding.inflate(inflater, container, false)
        val rootView = binding.root
        // Bind Suppliers data object to UI
        binding.setSupplier(supplier)
        return rootView
    }

    /* Set title for activity based on type of operation */
    private fun setActivityTitle() {
        val activity = this.activity
        activityTitle = when( operation ) {
            UIConstants.OP_CREATE -> resources.getString(R.string.title_create_fragment, EntityTypes.supplier.localName)
            else -> "${resources.getString(R.string.title_update_fragment)} ${EntityTypes.supplier.localName}"
        }
        if (activity is SuppliersListActivity) {
            activity.invalidateOptionsMenu()
        } else {
            activity?.title = activityTitle
        }
    }

    /*
     * Observe create-update-delete events by using view's lifecycle owner that will allow liveData, the life cycle
     * observer, to unsubscribe even when fragment is detached and reattached.
     */
    private fun observeOperationCompletionEvents() {
        viewModel.updateResult.observe(viewLifecycleOwner,
            Observer { result -> onComplete(result!!) })

        viewModel.createResult.observe(viewLifecycleOwner,
            Observer { result -> onComplete(result!!) })
    }

    /**
     * Notify user of error encountered while execution the operation
     *
     * @param [result] operation result with error
     */
    private fun handleError(result: OperationResult<Supplier>) {
        val errorMessage = when (result.operation) {
            OperationResult.Operation.UPDATE -> ErrorMessage(resources.getString(R.string.update_failed),
                    resources.getString(R.string.update_failed_detail), result.error, false)
            OperationResult.Operation.CREATE -> ErrorMessage(resources.getString(R.string.create_failed),
                    resources.getString(R.string.create_failed_detail), result.error, false)
            else -> throw AssertionError()
        }
        errorHandler.sendErrorMessage(errorMessage)
    }

    /**
     * Handles a successful operation execution
     *
     * @param [result] operation result completed successfully
     */
    private fun handleSuccessfulCompletion(result: OperationResult<Supplier>) {
        val activity = this.activity ?: return

        if (resources.getBoolean(R.bool.two_pane)) {
            val view = getActivity()!!.currentFocus
            if (view != null) {
                val inputMethodManager = activity
                        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            }
            if (result.operation == OperationResult.Operation.UPDATE) {
                restartDetailFragment(result.singleResult)
            } else if (result.operation == OperationResult.Operation.CREATE) {
                restartActivity()
            }
        } else {
            this.activity!!.finish()
        }
    }

    /**
     * Restarts SuppliersDetailFragment with the updated Supplier entity instance
     *
     * @param [supplier] updated supplier returned
     */
    private fun restartDetailFragment(supplier: Supplier?) {
        val arguments = Bundle()
        arguments.putParcelable(BundleKeys.ENTITY_INSTANCE, supplier)
        val fragment = SuppliersDetailFragment()
        fragment.arguments = arguments
        activity!!.supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_item_detail, fragment, UIConstants.DETAIL_FRAGMENT_TAG)
            .commit()
    }

    /* Restarts owning activity */
    private fun restartActivity() {
        val intent = activity!!.intent
        activity!!.finish()
        startActivity(intent)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SuppliersCreateFragment::class.java)
    }
}
