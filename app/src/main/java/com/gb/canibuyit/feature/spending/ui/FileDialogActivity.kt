package com.gb.canibuyit.feature.spending.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ListActivity
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.gb.canibuyit.R
import com.gb.canibuyit.error.DirReadException
import com.gb.canibuyit.util.DirUtils
import com.gb.canibuyit.util.PermissionVerifier
import com.gb.canibuyit.util.hideKeyboard
import com.gb.canibuyit.util.inflate
import kotlinx.android.synthetic.main.activity_file_picker.*
import java.io.File
import java.util.HashMap

class FileDialogActivity : ListActivity() {

    private var selectionMode = SELECTION_MODE_CREATE
    private var formatFilters: Array<String>? = null
    private var canSelectDir = false

    private var adapter: FileListAdapter? = null
    private var clickedFile: File? = null
    private var currentPath = ROOT
    private var parentPath: String? = null
    private val lastPositions = HashMap<String, Int>()

    private lateinit var permissionVerifier: PermissionVerifier

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_picker)
        val permissions: Array<String> = if (selectionMode == SELECTION_MODE_CREATE) {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionVerifier = PermissionVerifier(this, permissions)
        permissionVerifier.verifyPermissions(true, REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION)

        setResult(Activity.RESULT_CANCELED, intent)

        selectionMode = intent.getIntExtra(EXTRA_SELECTION_MODE, SELECTION_MODE_CREATE)
        formatFilters = intent.getStringArrayExtra(EXTRA_FORMAT_FILTER)
        canSelectDir = intent.getBooleanExtra(EXTRA_CAN_SELECT_DIR, false)

        this.select_btn.isEnabled = false
        this.select_btn.setOnClickListener {
            clickedFile?.let {
                intent.putExtra(EXTRA_RESULT_PATH, it.path)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
        new_btn.setOnClickListener { v ->
            setCreateVisible(v)
            this.file_name_input.setText("")
            this.file_name_input.requestFocus()
        }
        if (selectionMode == SELECTION_MODE_OPEN) {
            new_btn.isEnabled = false
        }

        this.selection_buttons_container.isVisible = false
        this.cancel_btn.setOnClickListener(this::setSelectVisible)
        this.create_btn.setOnClickListener {
            if (this.file_name_input.text.isNotEmpty()) {
                intent.putExtra(EXTRA_RESULT_PATH, currentPath + "/" + this.file_name_input.text)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }

        var startPath: String? = intent.getStringExtra(EXTRA_START_PATH)
        startPath = startPath ?: ROOT
        if (canSelectDir) {
            clickedFile = File(startPath)
            this.select_btn.isEnabled = true
        }
        setData(startPath)
    }

    private fun setData(path: String) {
        var path = path
        val useAutoSelection = path.length < currentPath.length
        var files: MutableList<DirUtils.FileInfo>? = null
        try {
            files = DirUtils.getDirInfo(path, formatFilters)
        } catch (e: DirReadException) {
            // maybe it's a file
            val parent = File(path).parent

            try {
                files = DirUtils.getDirInfo(parent, formatFilters)
                setCreateVisible(null)
                this.file_name_input.setText(File(path).name)
                this.file_name_input.requestFocus()
                path = parent
            } catch (e2: DirReadException) {
                e.printStackTrace()
                Toast.makeText(this@FileDialogActivity, "Unable to read folder $path",
                        Toast.LENGTH_SHORT)
                        .show()
                try {
                    path = ROOT
                    files = DirUtils.getDirInfo(path, formatFilters)
                } catch (e1: DirReadException) {
                    e1.printStackTrace()
                }
            }
        }

        parentPath = null
        files?.let {
            if (path != ROOT) {
                it.add(0, DirUtils.FileInfo(ROOT, true))
                it.add(1, DirUtils.FileInfo(PARENT, true))
                parentPath = File(path).parent
            }

            currentPath = path
            adapter = FileListAdapter(this, it)
            listAdapter = adapter

            val position = lastPositions[parentPath]
            if (position != null && useAutoSelection) {
                listView.setSelection(position)
            }
            this.path_lbl.text = "${getText(R.string.location)}: $currentPath"
        } ?: let {
            listAdapter = null
            adapter = null
            this.path_lbl.text = null
        }
    }

    private class FileListAdapter internal constructor(context: Context,
                                                       items: List<DirUtils.FileInfo>) :
            ArrayAdapter<DirUtils.FileInfo>(context, 0, items) {
        private inner class ViewHolder {
            lateinit var icon: ImageView
            lateinit var filename: TextView
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View
            val holder: ViewHolder

            if (convertView == null) {
                view = parent.inflate(R.layout.list_item_file_picker)
                holder = ViewHolder()
                holder.icon = view.findViewById(R.id.icon) as ImageView
                holder.filename = view.findViewById(R.id.file_name_input) as TextView
                view.tag = holder
            } else {
                view = convertView
                holder = view.tag as ViewHolder
            }
            val fileInfo = getItem(position)
            holder.icon.setImageResource(
                    if (fileInfo!!.isFolder) R.drawable.folder else R.drawable.file)
            holder.filename.text = fileInfo.path
            return view
        }
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val path = adapter!!.getItem(position)!!.path
        val file: File
        file = if (path == ROOT) {
            File(path)
        } else {
            File(currentPath, path)
        }
        setSelectVisible(v)

        if (file.isDirectory) {
            this.select_btn.isEnabled = false
            if (file.canRead()) {
                lastPositions[currentPath] = position
                setData(file.absolutePath)

                if (canSelectDir) {
                    clickedFile = file
                    v.isSelected = true
                    this.select_btn.isEnabled = true
                }
            } else {
                AlertDialog.Builder(this)
                        .setTitle(
                                "Folder [" + file.name + "] " + getText(R.string.cant_read_folder))
                        .setPositiveButton("OK", null).show()
            }
        } else {
            clickedFile = file
            v.isSelected = true
            this.select_btn.isEnabled = true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            this.select_btn.isEnabled = false
            if (this.creation_buttons_container.visibility == View.VISIBLE) {
                this.creation_buttons_container.isVisible = false
                this.selection_buttons_container.isVisible = true
            } else {
                parentPath?.let {
                    setData(it)
                } ?: let {
                    return super.onKeyDown(keyCode, event)
                }
            }
            return true
        } else {
            return super.onKeyDown(keyCode, event)
        }
    }

    private fun setCreateVisible(view: View?) {
        this.creation_buttons_container.isVisible = true
        this.selection_buttons_container.isVisible = false
        view?.apply {
            hideKeyboard()
        }
        this.select_btn.isEnabled = false
    }

    private fun setSelectVisible(view: View?) {
        this.creation_buttons_container.isVisible = false
        this.selection_buttons_container.isVisible = true
        view?.apply {
            hideKeyboard()
        }
        this.select_btn.isEnabled = false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (!permissionVerifier.onRequestPermissionsResult(requestCode, permissions,
                        grantResults)) {
            Toast.makeText(this, "Missing permissions!", Toast.LENGTH_SHORT).show()
        }
    }
}

private const val REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION = 1

const val SELECTION_MODE_CREATE = 1
const val SELECTION_MODE_OPEN = 2

private const val ROOT = "/"
private const val PARENT = "../"

const val EXTRA_START_PATH = "START_PATH"
const val EXTRA_FORMAT_FILTER = "FORMAT_FILTER"
const val EXTRA_RESULT_PATH = "RESULT_PATH"
const val EXTRA_SELECTION_MODE = "SELECTION_MODE"
const val EXTRA_CAN_SELECT_DIR = "CAN_SELECT_DIR"