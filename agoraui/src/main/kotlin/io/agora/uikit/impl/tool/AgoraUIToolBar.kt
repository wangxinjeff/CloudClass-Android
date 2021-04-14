package io.agora.uikit.impl.tool

import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.DecelerateInterpolator
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.uikit.impl.AbsComponent
import io.agora.uikit.R
import io.agora.uikit.interfaces.protocols.AgoraUIDrawingConfig
import io.agora.uikit.interfaces.listeners.IAgoraUIWhiteboardListener

class AgoraUIToolBar(context: Context,
                     parent: ViewGroup,
                     foldTop: Int = 0, foldLeft: Int = 0,
                     private var foldWidth: Int = 0,
                     private var foldHeight: Int = 0,
                     unfoldTop: Int = 0, unfoldLeft: Int = 0,
                     private var unfoldWidth: Int = 0,
                     private var unfoldHeight: Int = 0,
                     shadowWidth: Int = 0) : AbsComponent(), AgoraUIToolDialogListener {
    private val tag: String = "AgoraUIToolWindow"
    private val duration = 400L
    private val itemMargin = context.resources.getDimensionPixelSize(R.dimen.margin_small)
    private val itemHeightRatio = 5f / 6

    private val foldLayout: View = LayoutInflater.from(context).inflate(R.layout.agora_tool_fold_layout, parent, false)
    private val unfoldLayout: View = LayoutInflater.from(context).inflate(R.layout.agora_tool_unfold_layout, parent, false)

    private val toolAdapter: AgoraUIToolItemAdapter
    private val recycler: RecyclerView
    private val unfoldButton: AppCompatImageView
    private var whiteboardlistener: IAgoraUIWhiteboardListener? = null

    private val config = AgoraUIDrawingConfig()
    var optionSelected = 0

    private var toolBarType = AgoraUIToolType.Whiteboard
    private var toolBarItems = AgoraUIToolItemList.whiteboardList

    init {
        if (foldWidth <= 0) foldWidth = context.resources.getDimensionPixelSize(R.dimen.agora_tool_switch_layout_size)
        if (foldHeight <= 0) foldHeight = context.resources.getDimensionPixelSize(R.dimen.agora_tool_switch_layout_size)

        foldLayout.translationX = -(foldWidth + shadowWidth * 2).toFloat()
        foldLayout.clipToOutline = true
        foldLayout.elevation = shadowWidth.toFloat()
        parent.addView(foldLayout, foldWidth, foldHeight)

        foldLayout.viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (foldLayout.width > 0 && foldLayout.height > 0) {
                            foldLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            val params = foldLayout.layoutParams as ViewGroup.MarginLayoutParams
                            params.leftMargin = foldLeft
                            params.topMargin = foldTop
                            foldLayout.layoutParams = params
                        }
                    }
                }
        )

        unfoldButton = unfoldLayout.findViewById(R.id.agora_tool_switch)

        foldLayout.setOnClickListener {
            foldLayout.isClickable = false
            unfoldButton.isClickable = false
            foldLayout.postDelayed({
                foldLayout.isClickable = true
                unfoldButton.isClickable = true
            }, duration * 2)
            foldLayout.animate().setDuration(duration)
                    .setInterpolator(DecelerateInterpolator())
                    .xBy(-(foldLeft + foldWidth + shadowWidth * 2).toFloat())
                    .withEndAction {
                        unfoldLayout.animate().setDuration(duration)
                                .setInterpolator(DecelerateInterpolator())
                                .xBy((unfoldLeft + unfoldWidth + shadowWidth * 2).toFloat())
                    }
        }

        if (unfoldWidth <= 0) unfoldWidth = context.resources.getDimensionPixelSize(R.dimen.agora_tool_layout_default_width)
        if (unfoldHeight <= 0) unfoldHeight = context.resources.getDimensionPixelSize(R.dimen.agora_tool_layout_default_height)

        unfoldLayout.elevation = shadowWidth.toFloat()
        unfoldLayout.outlineProvider = AgoraUIToolUnfoldViewOutline()
        unfoldLayout.clipToOutline = true
        parent.addView(unfoldLayout, unfoldWidth, unfoldHeight)

        unfoldLayout.viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (foldLayout.width > 0 && foldLayout.height > 0) {
                            unfoldLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            val params = unfoldLayout.layoutParams as ViewGroup.MarginLayoutParams
                            params.leftMargin = unfoldLeft
                            params.topMargin = unfoldTop
                            unfoldLayout.layoutParams = params
                        }
                    }
                }
        )

        unfoldButton.setOnClickListener {
            foldLayout.isClickable = false
            unfoldButton.isClickable = false
            foldLayout.postDelayed({
                foldLayout.isClickable = true
                unfoldButton.isClickable = true
            }, duration * 2)
            unfoldLayout.animate().setDuration(duration)
                    .setInterpolator(DecelerateInterpolator())
                    .xBy(-(unfoldLeft + unfoldWidth + shadowWidth * 2).toFloat())
                    .withEndAction {
                        foldLayout.animate().setDuration(duration)
                                .setInterpolator(DecelerateInterpolator())
                                .xBy((foldLeft + foldWidth + shadowWidth * 2).toFloat())
                    }
        }

        recycler = unfoldLayout.findViewById(R.id.agora_tool_recycler)
        val recyclerParams = recycler.layoutParams as RelativeLayout.LayoutParams
        recyclerParams.height = recyclerHeight()
        recycler.layoutParams = recyclerParams

        toolAdapter = AgoraUIToolItemAdapter(this, recycler)
        recycler.adapter = this.toolAdapter
        recycler.layoutManager = LinearLayoutManager(context,
                LinearLayoutManager.VERTICAL, false)

        unfoldLayout.viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (unfoldButton.height > 0 && recycler.height > 0) {
                            unfoldLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            adjustToolHeight()
                        }
                    }
                }
        )
    }

    private fun recyclerHeight(): Int {
        // recycler item height versus width ratio is 5 / 6
        return (unfoldWidth * toolBarItems.size * itemHeightRatio).toInt()
    }

    private fun adjustToolHeight() {
        var params = unfoldButton.layoutParams as ViewGroup.MarginLayoutParams
        val marginVertical = (unfoldWidth - unfoldButton.width) / 2
        params.topMargin = marginVertical
        params.bottomMargin = marginVertical
        unfoldButton.layoutParams = params

        params = recycler.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = marginVertical

        // The height of recycler view if current list
        // items are all displayed
        val recyclerHeight = recyclerHeight()

        // The total height of tool bar if current
        // list items are all displayed
        var totalHeight = unfoldButton.height + marginVertical * 2 +
                if (recyclerHeight == 0) 0 else (recyclerHeight + marginVertical)

        if (totalHeight > unfoldHeight && recyclerHeight > 0) {
            totalHeight -= (totalHeight - unfoldHeight)
        }

        params.height = totalHeight
        recycler.layoutParams = params

        params = unfoldLayout.layoutParams as ViewGroup.MarginLayoutParams
        params.height = totalHeight
        unfoldLayout.layoutParams = params
    }

    fun setConfig(config: AgoraUIDrawingConfig) {
        this.config.set(config)
        toolAdapter.notifyDataSetChanged()
    }

    fun setWhiteboardFunctionEnabled(enabled: Boolean) {
        recycler.post {
            toolBarItems = if (!enabled) {
                if (toolBarType == AgoraUIToolType.All) {
                    AgoraUIToolItemList.RosterOnlyList
                } else {
                    setSelfVisibility(false)
                    recycler.visibility = View.GONE
                    AgoraUIToolItemList.emptyList
                }
            } else {
                setSelfVisibility(true)
                recycler.visibility = View.VISIBLE
                when (toolBarType) {
                    AgoraUIToolType.All -> AgoraUIToolItemList.AllItemList
                    AgoraUIToolType.Whiteboard -> AgoraUIToolItemList.whiteboardList
                }
            }

            toolAdapter.notifyDataSetChanged()
            adjustToolHeight()
        }
    }

    fun setToolbarType(type: AgoraUIToolType) {
        this.toolBarType = type
        recycler.post {
            toolBarItems = when (type) {
                AgoraUIToolType.All -> AgoraUIToolItemList.AllItemList
                AgoraUIToolType.Whiteboard -> AgoraUIToolItemList.whiteboardList
            }

            toolAdapter.notifyDataSetChanged()
            adjustToolHeight()
        }
    }

    fun setToolListener(listener: IAgoraUIWhiteboardListener) {
        this.whiteboardlistener = listener
    }

    private fun setSelfVisibility(visible: Boolean) {
        foldLayout.visibility = if (visible) VISIBLE else GONE
        unfoldLayout.visibility = if (visible) VISIBLE else GONE
    }

    private class AgoraUIToolUnfoldViewOutline : ViewOutlineProvider() {
        override fun getOutline(view: View?, outline: Outline?) {
            view?.let {
                outline?.setRoundRect(0, 0, it.width, it.height, it.width / 2f)
            }
        }
    }

    class AgoraUIToolItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: AppCompatImageView = itemView.findViewById(R.id.agora_tool_item_icon)
        val more: AppCompatImageView = itemView.findViewById(R.id.agora_tool_item_more_icon)
    }

    inner class AgoraUIToolItemAdapter(private val window: AgoraUIToolBar,
                                       private val recyclerView: RecyclerView)
        : RecyclerView.Adapter<AgoraUIToolItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgoraUIToolItemViewHolder {
            return AgoraUIToolItemViewHolder(LayoutInflater.from(parent.context).inflate(
                    R.layout.agora_tool_item_layout, parent, false))
        }

        override fun onBindViewHolder(holder: AgoraUIToolItemViewHolder, position: Int) {
            val layoutParam = holder.itemView.layoutParams
            layoutParam.width = recyclerView.width
            layoutParam.height = (layoutParam.width * 5f / 6).toInt()
            holder.itemView.layoutParams = layoutParam

            var params = holder.icon.layoutParams as RelativeLayout.LayoutParams
            params.width = (recyclerView.width / 3f * 2).toInt()
            params.height = params.width
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
            holder.icon.layoutParams = params

            params = holder.more.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.ALIGN_BOTTOM, holder.icon.id)
            params.addRule(RelativeLayout.ALIGN_END, holder.icon.id)
            params.addRule(RelativeLayout.ALIGN_RIGHT, holder.icon.id)
            params.bottomMargin = itemMargin
            holder.more.layoutParams = params

            val pos = holder.adapterPosition
            val item = toolBarItems[pos]
            holder.icon.setImageResource(item.iconRes)
            holder.more.visibility = if (toolBarItems[pos].hasPopup) View.VISIBLE else View.GONE

            holder.itemView.isActivated = (pos == optionSelected)
            holder.itemView.setOnClickListener {
                optionSelected = pos
                toolAdapter.notifyDataSetChanged()
                handleClick(holder.itemView, toolBarItems[pos])
            }
        }

        override fun getItemCount(): Int {
            return toolBarItems.size
        }

        private fun handleClick(view: View, item: AgoraUIToolItem) {
            when (item.type) {
                AgoraUIToolItemType.Select -> {
                    config.activeAppliance = AgoraUIApplianceType.Select
                    window.whiteboardlistener?.onApplianceSelected(config.activeAppliance)
                }

                AgoraUIToolItemType.Pen -> {
                    if (config.activeAppliance != AgoraUIApplianceType.Pen &&
                            config.activeAppliance != AgoraUIApplianceType.Rect &&
                            config.activeAppliance != AgoraUIApplianceType.Circle &&
                            config.activeAppliance != AgoraUIApplianceType.Line) {
                        config.activeAppliance = AgoraUIApplianceType.Pen
                        whiteboardlistener?.onApplianceSelected(config.activeAppliance)
                    }

                    AgoraUIToolDialog(view.context, item.type,
                            this@AgoraUIToolBar, config).show(view)
                }

                AgoraUIToolItemType.Text -> {
                    window.whiteboardlistener?.onApplianceSelected(AgoraUIApplianceType.Text)
                    AgoraUIToolDialog(view.context, item.type,
                            this@AgoraUIToolBar, config).show(view)
                }

                AgoraUIToolItemType.Eraser -> {
                    config.activeAppliance = AgoraUIApplianceType.Eraser
                    window.whiteboardlistener?.onApplianceSelected(config.activeAppliance)
                }

                AgoraUIToolItemType.Color -> {
                    AgoraUIToolDialog(view.context, item.type,
                            this@AgoraUIToolBar, config).show(view)
                }

                AgoraUIToolItemType.Roster -> {
                    window.whiteboardlistener?.onRosterSelected(view)
                }
            }
        }
    }

    override fun onFontSizeSelected(index: Int) {
        whiteboardlistener?.onFontSizeSelected(index)
    }

    override fun onColorSelected(color: Int) {
        whiteboardlistener?.onColorSelected(color)
    }

    override fun onThickSelected(thick: Int) {
        whiteboardlistener?.onThicknessSelected(thick)
    }

    override fun onApplianceSelected(appliance: AgoraUIApplianceType) {
        config.activeAppliance = appliance
        whiteboardlistener?.onApplianceSelected(appliance)
        toolAdapter.notifyDataSetChanged()
    }

    override fun setRect(rect: Rect) {

    }

    fun setVerticalPosition(top: Int, maxHeight: Int) {
        unfoldLayout.post {
            var params = unfoldLayout.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = top
            unfoldLayout.layoutParams = params

            params = foldLayout.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = top
            foldLayout.layoutParams = params

            unfoldHeight = maxHeight
            adjustToolHeight()
        }
    }
}