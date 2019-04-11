package com.company.wizapp2.mdui.stock

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
import com.company.wizapp2.viewmodel.stock.StockViewModel
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
import com.sap.cloud.android.odata.espmcontainer.Stock
import com.sap.cloud.mobile.fiori.indicator.FioriProgressBar
import com.sap.cloud.mobile.fiori.`object`.ObjectCell
import com.sap.cloud.mobile.odata.EntityValue
import kotlinx.android.synthetic.main.activity_item_list.*
import kotlinx.android.synthetic.main.collection_list_element.view.*
import org.slf4j.LoggerFactory

/**
 * An activity representing a list of Stock. This activity has different presentations for handset and tablet-size
 * devices. On handsets, the activity presents a list of items, which when touched, lead to a view representing
 * Stock details. On tablets, the activity presents the list of Stock and Stock details side-by-side using two
 * vertical panes.
 */
class StockListActivity : AppCompatActivity() {
    /*
     * Service manager to provide root-URL of OData Service for Glide to load images if there are media resources
     * associated with the entity type
     */
    private lateinit var sapServiceManager: SAPServiceManager

    /* Error handler to display message should error occurs */
    private lateinit var errorHandler: ErrorHandler

    /* List adapter to be used with RecyclerView containing all instances of stocks */
    private lateinit var adapter: StockListAdapter

    /*
     * Data refresh can be triggered by swiping down the list.
     */
    private lateinit var refreshLayout: SwipeRefreshLayout

    /* View model of the entity type */
    private lateinit var viewModel: StockViewModel

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
                LOGGER.info("Stock list starting to refresh")
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
                setTitle(EntitySetName.Stock.titleId)
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
    private fun onDeleteComplete(result: OperationResult<Stock>) {
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
     * For update, the stock to update is provided as a parameter.
     */
    private fun showCreateFragment(operation: String, stock: Stock?) {
        val arguments = Bundle()
        arguments.putSerializable(BundleKeys.OPERATION, operation)
        if (operation == UIConstants.OP_UPDATE) {
            arguments.putParcelable(BundleKeys.ENTITY_INSTANCE, stock)
        }
        val fragment = StockCreateFragment()
        fragment.arguments = arguments
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_item_detail, fragment, UIConstants.MODIFY_FRAGMENT_TAG)
            .commit()
    }

    /*
     * Starts entity detail activity to handle create or update operation.
     * For update, the stock to update is provided as a parameter.
     */
    private fun showDetailActivity(context: Context, operation: String, stock: Stock?) {
        val intent = Intent(context, StockDetailActivity::class.java)
        intent.putExtra(BundleKeys.OPERATION, operation)
        if (operation == UIConstants.OP_UPDATE || operation == UIConstants.OP_READ) {
            intent.putExtra(BundleKeys.ENTITY_INSTANCE, stock)
        }
        context.startActivity(intent)
    }

    /**
     * Observe entity collection, delete completion event and refresh completion event
     */
    private fun observeDataAndEvents() {

        navigationPropertyName?.let {_navigationPropertyName ->
            parentEntityData?.let { _parentEntityData ->
                viewModel = ViewModelProviders.of(this, EntityViewModelFactory(application, _navigationPropertyName, _parentEntityData)).get(StockViewModel::class.java)
            }
        } ?: run {
                viewModel = ViewModelProviders.of(this).get(StockViewModel::class.java)
                viewModel.initialRead()
        }

        viewModel.observableItems.observe(this, Observer<List<Stock>> { stocks ->
            if (stocks != null) {
                adapter.setItems(stocks)
                
                // in two-pane mode the first element is shown automatically on the detail section
                if (this.resources.getBoolean(R.bool.two_pane) && stocks.size > 0) {
                    if (viewModel.inFocusId == 0L) {
                        adapter.showDetailFragment(stocks.get(0))
                        viewModel.inFocusId = adapter.getItemIdForStock(stocks.get(0))
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
        adapter = StockListAdapter(this, this.resources.getBoolean(R.bool.two_pane), recyclerView)
        recyclerView.adapter = adapter
    }

    /**
     * List adapter to be used with RecyclerView. It contains the set of stocks.
     *
     * @param [context]
     * @param [hasTwoPane] true, when device large enough for two pane mode
     * @param [recyclerView] RecyclerView this adapter is associate with
     */
    inner class StockListAdapter(
        private val context: Context,
        private val hasTwoPane: Boolean,
        private val recyclerView: RecyclerView
    ) : RecyclerView.Adapter<StockListAdapter.ViewHolder>() {

        /* Entire list of Stock collection */
        private var stocks: MutableList<Stock>? = null

        /*
         * Flag to indicate whether we have checked retained selected stock
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
                        val stock = viewModel.getSelected(0)
                        if (viewModel.numberOfSelected() == 1 && stock != null) {
                            if (hasTwoPane) {
                                showCreateFragment(UIConstants.OP_UPDATE, stock)
                                actionMode?.finish()
                                actionMode = null
                            } else {
                                showDetailActivity(context, UIConstants.OP_UPDATE, stock)
                            }
                        }
                        retVal = true
                    }
                    R.id.delete_item -> {
                        val dDialog: EntityDeleteDialog<Stock> = EntityDeleteDialog(context, viewModel)
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
        fun setItems(currentStock: List<Stock>) {
            if (stocks == null) {
                stocks = ArrayList(currentStock)
                notifyItemRangeInserted(0, currentStock.size)
            } else {
                val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun getOldListSize(): Int {
                        return stocks!!.size
                    }

                    override fun getNewListSize(): Int {
                        return currentStock.size
                    }

                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return stocks!![oldItemPosition].entityKey.toString() == currentStock[newItemPosition].entityKey.toString()
                    }

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        val stock = stocks!![oldItemPosition]
                        return !stock.isUpdated && currentStock[newItemPosition] == stock
                    }
                })
                stocks!!.clear()
                stocks!!.addAll(currentStock)
                result.dispatchUpdatesTo(this)
            }
        }

        override fun getItemId(position: Int): Long {
            return getItemIdForStock(stocks!![position])
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

            val stock = stocks!![holder.adapterPosition]
            populateObjectCell(holder.objectCell, stock)

            val isActive = getItemIdForStock(stock) == viewModel.inFocusId
            val isStockSelected = viewModel.selectedContains(stock)
            setViewBackground(holder.view, isStockSelected, isActive)

            setOnClickListener(holder, stock)

            setOnLongClickListener(holder)

            setOnCheckedChangeListener(holder, stock)

            if (isStockSelected) {
                setCheckBox(holder.checkBox, true)
            }
        }

        /*
         * Check to see if there are an retained selected stock on start.
         * This situation occurs when a rotation with selected stocks is triggered by user.
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
         * @param [stock] associated with this ViewHolder
         */
        private fun setOnClickListener(holder: ViewHolder, stock: Stock) {
            holder.view.setOnClickListener { view ->
                if (hasTwoPane) {
                    showDetailFragment(stock)
                    resetSelected()
                    processClickAction(view, stock)
                } else {
                    showDetailActivity(view.context, UIConstants.OP_READ, stock)
                }
            }
        }

        /*
         * If there are selected stocks via long press, clear them as click and long press are mutually exclusive
         * In addition, since we are clearing all selected stocks via long press, finish the action mode.
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
            val viewHolder = recyclerView.findViewHolderForItemId(inFocusId) as? StockListAdapter.ViewHolder
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
         * @param stock associated with the clicked view
         */
        private fun processClickAction(view: View, stock: Stock) {
            resetPreviouslyClicked()
            setViewBackground(view, false, true)
            viewModel.inFocusId = getItemIdForStock(stock)
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
                    actionMode = this@StockListActivity.startActionMode(actionModeCallback)
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
         * @param [stock] associated with this ViewHolder
         */
        private fun setOnCheckedChangeListener(holder: ViewHolder, stock: Stock) {
            holder.checkBox.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    holder.checkBox.visibility = View.VISIBLE
                    viewModel.addSelected(stock)
                    manageActionModeOnCheckedTransition()
                    resetPreviouslyClicked()
                } else {
                    holder.checkBox.visibility = View.INVISIBLE
                    viewModel.removeSelected(stock)
                    manageActionModeOnUncheckedTransition()
                }
                setViewBackground(holder.view, viewModel.selectedContains(stock), false)
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
                actionMode = this@StockListActivity.startActionMode(actionModeCallback)
            } else {
                if (viewModel.numberOfSelected() > 1) {
                    actionMode!!.menu.findItem(R.id.update_item).isVisible = false
                }
            }
            actionMode!!.title = viewModel.numberOfSelected().toString()
        }

        /*
         * This is called when one of the selected stocks has been de-selected
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
         * Use detail fragment to show stock which is used in twp-pane mode
         *
         * @param [stock] to show
         */
        public fun showDetailFragment(stock: Stock) {
            val arguments = Bundle()
            arguments.putParcelable(BundleKeys.ENTITY_INSTANCE, stock)
            val fragment = StockDetailFragment()
            fragment.arguments = arguments
            (context as StockListActivity).supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_item_detail, fragment, UIConstants.DETAIL_FRAGMENT_TAG)
                .commit()
        }

        private fun populateObjectCell(objectCell: ObjectCell, stock: Stock) {
            val dataValue = stock.getDataValue(Stock.lotSize)
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

            if (EntityMediaResource.hasMediaResources(EntitySets.stock)) {
                objectCell.prepareDetailImageView().scaleType = ImageView.ScaleType.FIT_CENTER
                Glide.with(context)
                    .load(EntityMediaResource.getMediaResourceUrl(stock, sapServiceManager.serviceRoot))
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
         * Set background of view to indicate stock selection status. Background set depends whether it is selected by
         * long press or click
         *
         * @param [view]
         * @param [isStockSelected] true if stock is selected via long press action
         * @param [isActive] true if stock is selected via click action
         */
        private fun setViewBackground(view: View, isStockSelected: Boolean, isActive: Boolean) {
            when {
                isStockSelected -> view.background = ContextCompat.getDrawable(context, R.drawable.list_item_selected)
                (isActive && hasTwoPane) -> view.background = ContextCompat.getDrawable(context, R.drawable.list_item_active)
                else -> view.background = ContextCompat.getDrawable(context, R.drawable.list_item_default)
            }
        }

        /**
         * Set up checkbox value and visibility based on stock selection status
         *
         * @param [checkBox]
         * @param [isStockSelected] true if stock is selected via long press action
         */
        private fun setCheckBox(checkBox: CheckBox, isStockSelected: Boolean) {
            if (isStockSelected) {
                checkBox.isChecked = true
                checkBox.visibility = View.VISIBLE
            } else {
                checkBox.isChecked = false
                checkBox.visibility = View.INVISIBLE
            }
        }

        override fun getItemCount(): Int {
            return if (stocks == null) {
                0
            } else {
                stocks!!.size
            }
        }

        /**
         * Computes a stable ID for each Stock object for use to locate the ViewHolder
         * @param stock
         * @return an ID based on the primary key of Stock
         */
        public fun getItemIdForStock(stock: Stock): Long {
            return stock.entityKey.toString().hashCode().toLong()
        }

        /**
         * ViewHolder for RecyclerView.
         * Each view has a Fiori ObjectCell and a checkbox (used by long press).
         */
        inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            /*
             * Fiori ObjectCell to display stock in list
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
        private val LOGGER = LoggerFactory.getLogger(StockListActivity::class.java)

        /* SAP Fiori Standard Theme Primary Color: 'Global Dark Base' */
        private val FIORI_STANDARD_THEME_GLOBAL_DARK_BASE = Color.rgb(63, 81, 96)

        /* SAP Fiori Standard Theme Primary Color: 'Background' */
        private val FIORI_STANDARD_THEME_BACKGROUND = Color.rgb(250, 250, 250)
    }
}
