package com.company.wizapp2.mdui.producttexts

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.WorkerThread
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.NavUtils
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.company.wizapp2.service.SAPServiceManager
import com.company.wizapp2.R
import com.company.wizapp2.viewmodel.EntityViewModelFactory
import com.company.wizapp2.viewmodel.producttext.ProductTextViewModel
import com.company.wizapp2.repository.OperationResult
import com.company.wizapp2.app.ErrorHandler
import com.company.wizapp2.app.ErrorMessage
import com.company.wizapp2.app.SAPWizardApplication
import com.company.wizapp2.mdui.BundleKeys
import com.company.wizapp2.mdui.UIConstants
import com.company.wizapp2.mdui.EntityKeyUtil
import com.company.wizapp2.mdui.EntityDeleteDialog
import com.company.wizapp2.mdui.EntitySetListActivity.EntitySetName
import com.company.wizapp2.mediaresource.EntityMediaResource
import com.sap.cloud.android.odata.espmcontainer.ESPMContainerMetadata.EntitySets
import com.sap.cloud.android.odata.espmcontainer.ProductText
import com.sap.cloud.mobile.fiori.indicator.FioriProgressBar
import com.sap.cloud.mobile.fiori.`object`.ObjectCell
import com.sap.cloud.mobile.odata.EntityValue
import kotlinx.android.synthetic.main.activity_item_list.*
import kotlinx.android.synthetic.main.collection_list_element.view.*
import org.slf4j.LoggerFactory

/**
 * An activity representing a list of ProductText. This activity has different presentations for handset and tablet-size
 * devices. On handsets, the activity presents a list of items, which when touched, lead to a view representing
 * ProductText details. On tablets, the activity presents the list of ProductText and ProductText details side-by-side using two
 * vertical panes.
 */
class ProductTextsListActivity : AppCompatActivity() {
    /*
     * Service manager to provide root-URL of OData Service for Glide to load images if there are media resources
     * associated with the entity type
     */
    private lateinit var sapServiceManager: SAPServiceManager

    /* Error handler to display message should error occurs */
    private lateinit var errorHandler: ErrorHandler

    /* List adapter to be used with RecyclerView containing all instances of productTexts */
    private lateinit var adapter: ProductTextListAdapter

    /*
     * Data refresh can be triggered by swiping down the list.
     */
    private lateinit var refreshLayout: SwipeRefreshLayout

    /* View model of the entity type */
    private lateinit var viewModel: ProductTextViewModel

    /* Fiori progress bar for busy indication if either update or delete action is clicked */
    private var progressBar: FioriProgressBar? = null
    
    /*
     * Navigation parameters: name of the link and the starting entity. Both of them are null, if
     * entity list was opened from the main list, i.e. without following a navigation link.
     */
    private var navigationPropertyName: String? = null
    private var parentEntityData: Parcelable? = null

    /*
     * RecyclerView associated with this activity
     * Required for tablet mode to manage view state
     */
    private lateinit var recyclerView: RecyclerView

    override fun onResume() {
        super.onResume()
        adapter.actionMode?.let {
            adapter.actionMode!!.finish()
            adapter.actionMode = null
        }

        observeDataAndEvents()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.itemlist_menu, menu)
        return true
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /**
     * Listen for option item selections so that we receive a notification when the user requests a refresh by selecting
     * the refresh action bar item.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
         return when (item.itemId) {
            // Check if user triggered a refresh:
            R.id.menu_refresh -> {
                LOGGER.info("ProductText list starting to refresh")
                // Signal SwipeRefreshLayout to start the progress indicator
                refreshLayout.isRefreshing = true

                refreshListData()
                setListTitle()
                true
            }
            R.id.menu_home -> {
                LOGGER.info("Go to home")
                NavUtils.navigateUpFromSameTask(this)
                true
            }
            else ->
                // User didn't trigger any button listed above, let the superclass handle this action
                super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sapWizardApplication = application as SAPWizardApplication
        sapServiceManager = sapWizardApplication.sapServiceManager
        errorHandler = sapWizardApplication.errorHandler

        setContentView(R.layout.activity_item_list)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        showUpButton()

        // read the intent parameters, which might hold navigation data
        navigationPropertyName = intent.getStringExtra("navigation")
        parentEntityData = intent.getParcelableExtra("parent")
        
        if (navigationPropertyName != null && parentEntityData != null) {
            val fab: FloatingActionButton = findViewById(R.id.fab)
            fab.hide()
        } else {
            createFloatingButton()
        }

        setupRefreshLayout()
        setupRecyclerView()
        setListTitle()
    }
    
    /**
     * List view title is set different, if the complete list or a navigation
     * data is shown.
     */
    private fun setListTitle() {
        navigationPropertyName?.let { _navigationPropertyName ->
            parentEntityData?.let { _parentEntityData ->
                val parentEntity = _parentEntityData as EntityValue
                val titleBuilder = StringBuilder()
                    .append(parentEntity.entityType.localName)
                    .append(EntityKeyUtil.getOptionalEntityKey(parentEntity))
                    .append("/")
                    .append(_navigationPropertyName)
                title = titleBuilder.toString()
            }
        } ?: run {
                setTitle(EntitySetName.ProductTexts.titleId)
        }
    }
    
     /**
     * If list is shown as a navigation endpoint, then refresh should just update the current list, the
     * whole collection shouldn't be downloaded again.
     */
    private fun refreshListData() {
        navigationPropertyName?.let { _navigationPropertyName ->
            parentEntityData?.let { _parentEntityData ->
                viewModel.refresh(_parentEntityData as EntityValue, _navigationPropertyName)
            }
        } ?: run {
                viewModel.refresh()
        }
    }

    private fun setupRefreshLayout() {
        refreshLayout = swiperefresh!!
        refreshLayout.setColorSchemeColors(FIORI_STANDARD_THEME_GLOBAL_DARK_BASE)
        refreshLayout.setProgressBackgroundColorSchemeColor(FIORI_STANDARD_THEME_BACKGROUND)

        refreshLayout.setOnRefreshListener {
            refreshListData()
        }
    }

    /* Completion callback for delete operation */
    private fun onDeleteComplete(result: OperationResult<ProductText>) {
        hideProgressBar()

        val exception = result.error
        if (exception != null) {
            handleDeleteError(exception)
            return
        }

        if (adapter.actionMode != null) {
            adapter.actionMode!!.finish()
        }

        if (resources.getBoolean(R.bool.two_pane)) {
            removeDetailFragment()
        }
    }

    private fun removeDetailFragment() {
        val fragment = supportFragmentManager.findFragmentByTag(UIConstants.DETAIL_FRAGMENT_TAG)
        if (fragment != null) {
            supportFragmentManager.beginTransaction().remove(fragment).commit()
        }
    }

    private fun hideProgressBar() {
        if (progressBar == null) {
            progressBar = indeterminateBar
        }
        progressBar!!.visibility = View.INVISIBLE
    }

    private fun handleDeleteError(exception: Exception) {
        val errorMessage = ErrorMessage(resources.getString(R.string.delete_failed),
                resources.getString(R.string.delete_failed_detail), exception, false)
        errorHandler.sendErrorMessage(errorMessage)
    }

    private fun showUpButton() {
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun createFloatingButton() {
        fab.setOnClickListener { view ->
            if (resources.getBoolean(R.bool.two_pane)) {
                showCreateFragment(UIConstants.OP_CREATE, null)
            } else {
                showDetailActivity(view.context, UIConstants.OP_CREATE, null)
            }
        }
    }

    /*
     * Starts entity create fragment to handle create or update operation.
     * For update, the productText to update is provided as a parameter.
     */
    private fun showCreateFragment(operation: String, productText: ProductText?) {
        val arguments = Bundle()
        arguments.putSerializable(BundleKeys.OPERATION, operation)
        if (operation == UIConstants.OP_UPDATE) {
            arguments.putParcelable(BundleKeys.ENTITY_INSTANCE, productText)
        }
        val fragment = ProductTextsCreateFragment()
        fragment.arguments = arguments
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_item_detail, fragment, UIConstants.MODIFY_FRAGMENT_TAG)
            .commit()
    }

    /*
     * Starts entity detail activity to handle create or update operation.
     * For update, the productText to update is provided as a parameter.
     */
    private fun showDetailActivity(context: Context, operation: String, productText: ProductText?) {
        val intent = Intent(context, ProductTextsDetailActivity::class.java)
        intent.putExtra(BundleKeys.OPERATION, operation)
        if (operation == UIConstants.OP_UPDATE || operation == UIConstants.OP_READ) {
            intent.putExtra(BundleKeys.ENTITY_INSTANCE, productText)
        }
        context.startActivity(intent)
    }

    /**
     * Observe entity collection, delete completion event and refresh completion event
     */
    private fun observeDataAndEvents() {

        navigationPropertyName?.let {_navigationPropertyName ->
            parentEntityData?.let { _parentEntityData ->
                viewModel = ViewModelProviders.of(this, EntityViewModelFactory(application, _navigationPropertyName, _parentEntityData)).get(ProductTextViewModel::class.java)
            }
        } ?: run {
                viewModel = ViewModelProviders.of(this).get(ProductTextViewModel::class.java)
                viewModel.initialRead()
        }

        viewModel.observableItems.observe(this, Observer<List<ProductText>> { productTexts ->
            if (productTexts != null) {
                adapter.setItems(productTexts)
                
                // in two-pane mode the first element is shown automatically on the detail section
                if (this.resources.getBoolean(R.bool.two_pane) && productTexts.size > 0) {
                    if (viewModel.inFocusId == 0L) {
                        adapter.showDetailFragment(productTexts.get(0))
                        viewModel.inFocusId = adapter.getItemIdForProductText(productTexts.get(0))
                    }
                }
            }
        })

        viewModel.deleteResult.observe(this, Observer { onDeleteComplete(it!!) })

        viewModel.readResult.observe(this, Observer {
            if (refreshLayout.isRefreshing) {
                refreshLayout.isRefreshing = false
            }
        })
    }

    private fun setupRecyclerView() {
        recyclerView = item_list ?: throw AssertionError()
        adapter = ProductTextListAdapter(this, this.resources.getBoolean(R.bool.two_pane), recyclerView)
        recyclerView.adapter = adapter
    }

    /**
     * List adapter to be used with RecyclerView. It contains the set of productTexts.
     *
     * @param [context]
     * @param [hasTwoPane] true, when device large enough for two pane mode
     * @param [recyclerView] RecyclerView this adapter is associate with
     */
    inner class ProductTextListAdapter(
        private val context: Context,
        private val hasTwoPane: Boolean,
        private val recyclerView: RecyclerView
    ) : RecyclerView.Adapter<ProductTextListAdapter.ViewHolder>() {

        /* Entire list of ProductText collection */
        private var productTexts: MutableList<ProductText>? = null

        /*
         * Flag to indicate whether we have checked retained selected productTexts
         */
        private var checkForSelectedOnCreate = false

        internal var actionMode: ActionMode? = null
        private val actionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                val inflater = mode.menuInflater
                inflater.inflate(R.menu.itemlist_view_options, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val retVal: Boolean
                when (item.itemId) {
                    R.id.update_item -> {
                        val productText = viewModel.getSelected(0)
                        if (viewModel.numberOfSelected() == 1 && productText != null) {
                            if (hasTwoPane) {
                                showCreateFragment(UIConstants.OP_UPDATE, productText)
                                actionMode?.finish()
                                actionMode = null
                            } else {
                                showDetailActivity(context, UIConstants.OP_UPDATE, productText)
                            }
                        }
                        retVal = true
                    }
                    R.id.delete_item -> {
                        val dDialog: EntityDeleteDialog<ProductText> = EntityDeleteDialog(context, viewModel)
                        dDialog.confirmDelete()
                        retVal = true
                    }
                    else -> retVal = false
                }
                return retVal
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                actionMode = null
                viewModel.removeAllSelected()
                notifyDataSetChanged()
            }
        }

        init {
            setHasStableIds(true)
        }

        /**
         * Use DiffUtil to calculate the difference and dispatch them to the adapter.
         * Note: Please use background thread for calculation if the list is large to avoid blocking main thread.
         */
        @WorkerThread
        fun setItems(currentProductTexts: List<ProductText>) {
            if (productTexts == null) {
                productTexts = ArrayList(currentProductTexts)
                notifyItemRangeInserted(0, currentProductTexts.size)
            } else {
                val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun getOldListSize(): Int {
                        return productTexts!!.size
                    }

                    override fun getNewListSize(): Int {
                        return currentProductTexts.size
                    }

                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return productTexts!![oldItemPosition].entityKey.toString() == currentProductTexts[newItemPosition].entityKey.toString()
                    }

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        val productText = productTexts!![oldItemPosition]
                        return !productText.isUpdated && currentProductTexts[newItemPosition] == productText
                    }
                })
                productTexts!!.clear()
                productTexts!!.addAll(currentProductTexts)
                result.dispatchUpdatesTo(this)
            }
        }

        override fun getItemId(position: Int): Long {
            return getItemIdForProductText(productTexts!![position])
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.collection_list_element, parent, false)
            return ViewHolder(view)
        }

        @Suppress("UNUSED_PARAMETER")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            checkForRetainedSelection()
            holder.checkBox.setOnCheckedChangeListener(null)
            holder.checkBox.isChecked = false
            holder.checkBox.visibility = View.INVISIBLE

            val productText = productTexts!![holder.adapterPosition]
            populateObjectCell(holder.objectCell, productText)

            val isActive = getItemIdForProductText(productText) == viewModel.inFocusId
            val isProductTextSelected = viewModel.selectedContains(productText)
            setViewBackground(holder.view, isProductTextSelected, isActive)

            setOnClickListener(holder, productText)

            setOnLongClickListener(holder)

            setOnCheckedChangeListener(holder, productText)

            if (isProductTextSelected) {
                setCheckBox(holder.checkBox, true)
            }
        }

        /*
         * Check to see if there are an retained selected productText on start.
         * This situation occurs when a rotation with selected productTexts is triggered by user.
         */
        private fun checkForRetainedSelection() {
            if (!checkForSelectedOnCreate) {
                checkForSelectedOnCreate = true
                if (viewModel.numberOfSelected() > 0) {
                    manageActionModeOnCheckedTransition()
                }
            }
        }

        /**
         * Set ViewHolder's view onClickListener
         *
         * @param [holder]
         * @param [productText] associated with this ViewHolder
         */
        private fun setOnClickListener(holder: ViewHolder, productText: ProductText) {
            holder.view.setOnClickListener { view ->
                if (hasTwoPane) {
                    showDetailFragment(productText)
                    resetSelected()
                    processClickAction(view, productText)
                } else {
                    showDetailActivity(view.context, UIConstants.OP_READ, productText)
                }
            }
        }

        /*
         * If there are selected productTexts via long press, clear them as click and long press are mutually exclusive
         * In addition, since we are clearing all selected productTexts via long press, finish the action mode.
         */
        private fun resetSelected() {
            if (viewModel.numberOfSelected() > 0) {
                viewModel.removeAllSelected()
                if (actionMode != null) {
                    actionMode!!.finish()
                    actionMode = null
                }
            }
        }

        /*
         * Attempt to locate previously clicked view and reset its background
         * Reset view model's inFocusId
         */
        private fun resetPreviouslyClicked() {
            val inFocusId = viewModel.inFocusId
            val viewHolder = recyclerView.findViewHolderForItemId(inFocusId) as? ProductTextListAdapter.ViewHolder
            if (viewHolder != null) {
                setViewBackground(viewHolder.view, false, false)
            }
            viewModel.inFocusId = 0
        }

        /**
         * This function will first locate previously clicked view. If found, it will reset its background.
         * Next, it set the current view's background to active state
         * Finally, it save the itemId of the clicked view
         * @param view on which Click action occurs
         * @param productText associated with the clicked view
         */
        private fun processClickAction(view: View, productText: ProductText) {
            resetPreviouslyClicked()
            setViewBackground(view, false, true)
            viewModel.inFocusId = getItemIdForProductText(productText)
        }

        /**
         * Set ViewHolder's view onLongClickListener
         *
         * @param [holder]
         */
        private fun setOnLongClickListener(holder: ViewHolder) {
            holder.view.setOnLongClickListener listener@ {
                if (hasTwoPane) {
                    // If we already have a create fragment (update or create). We will discard the fragment to avoid
                    // confusion
                    val fragment = supportFragmentManager.findFragmentByTag(UIConstants.MODIFY_FRAGMENT_TAG)
                    if (fragment != null) {
                        supportFragmentManager.beginTransaction().remove(fragment).commit()
                        return@listener true
                    }
                }
                if (actionMode == null) {
                    actionMode = this@ProductTextsListActivity.startActionMode(actionModeCallback)
                }

                val box = holder.checkBox
                if (box.isChecked) {
                    box.isChecked = false
                    box.visibility = View.INVISIBLE
                } else {
                    box.isChecked = true
                    box.visibility = View.VISIBLE
                }
                true
            }
        }

        /**
         * Set ViewHolder's CheckBox onCheckedChangeListener
         *
         * @param [holder]
         * @param [productText] associated with this ViewHolder
         */
        private fun setOnCheckedChangeListener(holder: ViewHolder, productText: ProductText) {
            holder.checkBox.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    holder.checkBox.visibility = View.VISIBLE
                    viewModel.addSelected(productText)
                    manageActionModeOnCheckedTransition()
                    resetPreviouslyClicked()
                } else {
                    holder.checkBox.visibility = View.INVISIBLE
                    viewModel.removeSelected(productText)
                    manageActionModeOnUncheckedTransition()
                }
                setViewBackground(holder.view, viewModel.selectedContains(productText), false)
            }
        }

        /*
         * Start Action Mode if it has not been started
         * This is only called when long press action results in a selection. Hence action mode may
         * not have been started. Along with starting action mode, title will be set.
         * If this is an additional selection, adjust title appropriately.
         */
        private fun manageActionModeOnCheckedTransition() {
            if (actionMode == null) {
                actionMode = this@ProductTextsListActivity.startActionMode(actionModeCallback)
            } else {
                if (viewModel.numberOfSelected() > 1) {
                    actionMode!!.menu.findItem(R.id.update_item).isVisible = false
                }
            }
            actionMode!!.title = viewModel.numberOfSelected().toString()
        }

        /*
         * This is called when one of the selected productTexts has been de-selected
         * On this event, we will determine if update action needs to be made visible or
         * action mode should be terminated (no more selected)
         */
        private fun manageActionModeOnUncheckedTransition() {
            when (viewModel.numberOfSelected()) {
                1 -> actionMode!!.menu.findItem(R.id.update_item).isVisible = true

                0 -> {
                    actionMode!!.finish()
                    actionMode = null
                    return
                }
            }
            actionMode!!.title = viewModel.numberOfSelected().toString()
        }

        /**
         * Use detail fragment to show productText which is used in twp-pane mode
         *
         * @param [productText] to show
         */
        public fun showDetailFragment(productText: ProductText) {
            val arguments = Bundle()
            arguments.putParcelable(BundleKeys.ENTITY_INSTANCE, productText)
            val fragment = ProductTextsDetailFragment()
            fragment.arguments = arguments
            (context as ProductTextsListActivity).supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_item_detail, fragment, UIConstants.DETAIL_FRAGMENT_TAG)
                .commit()
        }

        private fun populateObjectCell(objectCell: ObjectCell, productText: ProductText) {
            val dataValue = productText.getDataValue(ProductText.language)
            var masterPropertyValue: String? = null

            if (dataValue != null) {
                masterPropertyValue = dataValue.toString()
            }

            objectCell.headline = masterPropertyValue
            objectCell.detailImage = null

            if (masterPropertyValue == null) {
                objectCell.detailImageCharacter = "?"
            } else {
                objectCell.detailImageCharacter = masterPropertyValue.substring(0, 1)
            }

            if (EntityMediaResource.hasMediaResources(EntitySets.productTexts)) {
                objectCell.prepareDetailImageView().scaleType = ImageView.ScaleType.FIT_CENTER
                Glide.with(context)
                    .load(EntityMediaResource.getMediaResourceUrl(productText, sapServiceManager.serviceRoot))
                    .apply(RequestOptions().fitCenter())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(objectCell.prepareDetailImageView())
            }

            objectCell.subheadline = "Subheadline goes here"
            objectCell.footnote = "Footnote goes here"
            if (masterPropertyValue == null) {
                objectCell.setIcon("?", 0)
            } else {
                objectCell.setIcon(masterPropertyValue.substring(0, 1), 0)
            }
            objectCell.setIcon(R.drawable.default_dot, 1, R.string.attachment_item_content_desc)
            objectCell.setIcon("!", 2)
        }

        /**
         * Set background of view to indicate productText selection status. Background set depends whether it is selected by
         * long press or click
         *
         * @param [view]
         * @param [isProductTextSelected] true if productText is selected via long press action
         * @param [isActive] true if productText is selected via click action
         */
        private fun setViewBackground(view: View, isProductTextSelected: Boolean, isActive: Boolean) {
            when {
                isProductTextSelected -> view.background = ContextCompat.getDrawable(context, R.drawable.list_item_selected)
                (isActive && hasTwoPane) -> view.background = ContextCompat.getDrawable(context, R.drawable.list_item_active)
                else -> view.background = ContextCompat.getDrawable(context, R.drawable.list_item_default)
            }
        }

        /**
         * Set up checkbox value and visibility based on productText selection status
         *
         * @param [checkBox]
         * @param [isProductTextSelected] true if productText is selected via long press action
         */
        private fun setCheckBox(checkBox: CheckBox, isProductTextSelected: Boolean) {
            if (isProductTextSelected) {
                checkBox.isChecked = true
                checkBox.visibility = View.VISIBLE
            } else {
                checkBox.isChecked = false
                checkBox.visibility = View.INVISIBLE
            }
        }

        override fun getItemCount(): Int {
            return if (productTexts == null) {
                0
            } else {
                productTexts!!.size
            }
        }

        /**
         * Computes a stable ID for each ProductText object for use to locate the ViewHolder
         * @param productText
         * @return an ID based on the primary key of ProductText
         */
        public fun getItemIdForProductText(productText: ProductText): Long {
            return productText.entityKey.toString().hashCode().toLong()
        }

        /**
         * ViewHolder for RecyclerView.
         * Each view has a Fiori ObjectCell and a checkbox (used by long press).
         */
        inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            /*
             * Fiori ObjectCell to display productText in list
             */
            val objectCell: ObjectCell = view.content

            /*
             * Checkbox for long press selection
             */
            val checkBox: CheckBox = view.cbx

            override fun toString(): String {
                return super.toString() + " '" + objectCell.description + "'"
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ProductTextsListActivity::class.java)

        /* SAP Fiori Standard Theme Primary Color: 'Global Dark Base' */
        private val FIORI_STANDARD_THEME_GLOBAL_DARK_BASE = Color.rgb(63, 81, 96)

        /* SAP Fiori Standard Theme Primary Color: 'Background' */
        private val FIORI_STANDARD_THEME_BACKGROUND = Color.rgb(250, 250, 250)
    }
}
