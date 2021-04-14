package io.agora.uikit.impl.users

import android.app.Dialog
import android.graphics.Color
import android.graphics.Rect
import android.view.*
import android.widget.CheckedTextView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.*
import io.agora.uikit.R
import io.agora.uikit.impl.container.AgoraUIConfig.clickInterval
import io.agora.uikit.interfaces.listeners.IAgoraUIUserListListener
import kotlin.math.min

class AgoraUIRoster(anchor: View) : Dialog(anchor.context, R.style.agora_dialog) {
    private lateinit var rvUserList: RecyclerView
    private lateinit var userListAdapter: UserListAdapter
    private lateinit var tvTeacherName: TextView

    var userListListener: IAgoraUIUserListListener? = null

    init {
        init(anchor)
    }

    private fun init(anchor: View?) {
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        setContentView(R.layout.agora_userlist_dialog_layout)
        rvUserList = findViewById(R.id.recycler_view)
        tvTeacherName = findViewById(R.id.tv_teacher_name)
        findViewById<View>(R.id.iv_close).setOnClickListener { dismiss() }

        userListAdapter = UserListAdapter(object : UserItemClickListener {
            override fun onCameraCheckChanged(item: AgoraUIUserDetailInfo, checked: Boolean) {
                userListListener?.onMuteVideo(!checked)
            }

            override fun onMicCheckChanged(item: AgoraUIUserDetailInfo, checked: Boolean) {
                userListListener?.onMuteAudio(!checked)
            }

            override fun onRendererContainer(item: AgoraUIUserDetailInfo, viewGroup: ViewGroup?) {
                userListListener?.onRendererContainer(viewGroup, item.streamUuid)
            }
        })

        rvUserList.addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                    setDrawable(ContextCompat.getDrawable(context, R.drawable.agora_userlist_divider)!!)
                })

        rvUserList.addItemDecoration(object : RecyclerView.ItemDecoration() {
            val itemHeight = context.resources.getDimensionPixelSize(R.dimen.agora_userlist_row_height)
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val layoutParams = view.layoutParams
                layoutParams.width = parent.measuredWidth
                layoutParams.height = itemHeight
                view.layoutParams = layoutParams
                super.getItemOffsets(outRect, view, parent, state)
            }
        })

        // remove the animator when refresh item
        rvUserList.itemAnimator?.addDuration = 0
        rvUserList.itemAnimator?.changeDuration = 0
        rvUserList.itemAnimator?.moveDuration = 0
        rvUserList.itemAnimator?.removeDuration = 0
        (rvUserList.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        rvUserList.adapter = userListAdapter

        anchor?.let {
            adjustPosition(anchor,
                    context.resources.getDimensionPixelSize(R.dimen.agora_userlist_dialog_width),
                    context.resources.getDimensionPixelSize(R.dimen.agora_userlist_dialog_height)
            )
        }
    }

    fun adjustPosition(anchor: View) {
        adjustPosition(anchor,
                context.resources.getDimensionPixelSize(R.dimen.agora_userlist_dialog_width),
                context.resources.getDimensionPixelSize(R.dimen.agora_userlist_dialog_height))
    }

    private fun adjustPosition(anchor: View, width: Int, height: Int) {
        val window = window
        val params = window!!.attributes
        hideStatusBar(window)

        params.width = width
        params.height = height
        params.gravity = Gravity.TOP or Gravity.START

        val locationsOnScreen = IntArray(2)
        anchor.getLocationOnScreen(locationsOnScreen)
        params.x = locationsOnScreen[0] + anchor.width
        params.y = locationsOnScreen[1] + anchor.height / 2 - height / 2
        window.attributes = params
    }

    private fun hideStatusBar(window: Window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        val flag = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.decorView.systemUiVisibility = (flag or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    fun updateUserList(list: MutableList<AgoraUIUserDetailInfo>) {
        filterTeacher(list)
        userListAdapter.submitList(ArrayList(list))
    }

    private fun filterTeacher(list: MutableList<AgoraUIUserDetailInfo>) {
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.user.role == AgoraUIUserRole.Teacher) {
                // deal with teacher info
                iterator.remove()
                updateTeacher(item)
                break
            }
        }
    }

    private fun findIndex(info: AgoraUIUserDetailInfo): Int {
        var index = 0
        var foundIndex = -1;
        for (item in userListAdapter.currentList) {
            if (item.user.userUuid == info.user.userUuid) {
                foundIndex = index
                break
            }
            index++
        }
        return foundIndex
    }

    private fun updateTeacher(info: AgoraUIUserDetailInfo) {
        tvTeacherName.post { tvTeacherName.text = info.user.userName }
    }

    private fun updateStudent(info: AgoraUIUserDetailInfo) {
        val index = findIndex(info)
        if (index >= 0) {
            userListAdapter.currentList[index] = info
            userListAdapter.notifyItemChanged(index)
        }
    }
}


internal class UserListDiff : DiffUtil.ItemCallback<AgoraUIUserDetailInfo>() {
    override fun areItemsTheSame(oldItem: AgoraUIUserDetailInfo, newItem: AgoraUIUserDetailInfo): Boolean {
        return oldItem == newItem && oldItem.user.userUuid == newItem.user.userUuid
    }

    override fun areContentsTheSame(oldItem: AgoraUIUserDetailInfo, newItem: AgoraUIUserDetailInfo): Boolean {
        return oldItem.user.userName == newItem.user.userName
                && oldItem.onLine == newItem.onLine
                && oldItem.coHost == newItem.coHost
                && oldItem.boardGranted == newItem.boardGranted
                && oldItem.cameraState == newItem.cameraState
                && oldItem.microState == newItem.microState
                && oldItem.enableAudio == newItem.enableAudio
                && oldItem.enableVideo == newItem.enableVideo
                && oldItem.rewardCount == newItem.rewardCount
    }

}

internal interface UserItemClickListener {
    fun onCameraCheckChanged(item: AgoraUIUserDetailInfo, checked: Boolean)
    fun onMicCheckChanged(item: AgoraUIUserDetailInfo, checked: Boolean)
    fun onRendererContainer(item: AgoraUIUserDetailInfo, viewGroup: ViewGroup?)
}

internal class UserHolder(val view: View, val listener: UserItemClickListener) : RecyclerView.ViewHolder(view) {
    private val tvName = view.findViewById<TextView>(R.id.tv_user_name)
    private val ctvDesktop = view.findViewById<CheckedTextView>(R.id.ctv_desktop)
    private val ctvAccess = view.findViewById<CheckedTextView>(R.id.ctv_access)
    private val ctvCamera = view.findViewById<CheckedTextView>(R.id.ctv_camera)
    private val ctvMic = view.findViewById<CheckedTextView>(R.id.ctv_mic)
    private val ctvStar = view.findViewById<CheckedTextView>(R.id.ctv_star)

    fun bind(item: AgoraUIUserDetailInfo) {
        tvName.text = item.user.userName

        ctvDesktop.isEnabled = item.onLine
        ctvAccess.isEnabled = item.boardGranted

        if (item.coHost) {
            ctvCamera.isEnabled = item.isSelf
        } else {
            ctvCamera.isEnabled = false
        }
        ctvCamera.isChecked = item.enableVideo
        ctvCamera.setOnClickListener {
            ctvCamera.isClickable = false
            ctvCamera.isChecked = !ctvCamera.isChecked
            listener.onCameraCheckChanged(item, ctvCamera.isChecked)
            ctvCamera.postDelayed({ ctvCamera.isClickable = true }, clickInterval)
        }

        if (item.coHost) {
            ctvMic.isEnabled = item.isSelf
        } else {
            ctvMic.isEnabled = false
        }
        ctvMic.isChecked = item.enableAudio
        ctvMic.setOnClickListener {
            ctvMic.isChecked = !ctvMic.isChecked
            listener.onMicCheckChanged(item, ctvMic.isChecked)
            ctvMic.postDelayed({ ctvCamera.isClickable = true }, clickInterval)
        }

        val tmp = min(item.rewardCount, 99)
        ctvStar.text = view.resources.getString(R.string.agora_video_reward, tmp)

//        listener.onRendererContainer(item, null)
    }
}

internal class UserListAdapter(val listener: UserItemClickListener) : ListAdapter<AgoraUIUserDetailInfo, UserHolder>(UserListDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            UserHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.agora_userlist_dialog_list_item, null), listener)

    override fun onBindViewHolder(holder: UserHolder, position: Int) {
        holder.bind(getItem(position))
    }

}