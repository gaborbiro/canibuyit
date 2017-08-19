package com.gb.canibuythat.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gb.canibuythat.R;
import com.gb.canibuythat.util.DirReadException;
import com.gb.canibuythat.util.DirUtils;
import com.gb.canibuythat.util.PermissionVerifier;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class FileDialogActivity extends ListActivity {

    private static final int REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION = 1;

    public static final int SELECTION_MODE_CREATE = 1;
    public static final int SELECTION_MODE_OPEN = 2;

    private static final String ROOT = "/";
    private static final String PARENT = "../";

    public static final String EXTRA_START_PATH = "START_PATH";
    public static final String EXTRA_FORMAT_FILTER = "FORMAT_FILTER";
    public static final String EXTRA_RESULT_PATH = "RESULT_PATH";
    public static final String EXTRA_SELECTION_MODE = "SELECTION_MODE";
    public static final String EXTRA_CAN_SELECT_DIR = "CAN_SELECT_DIR";

    private int selectionMode = SELECTION_MODE_CREATE;
    private String[] formatFilters = null;
    private boolean canSelectDir = false;

    private TextView pathView;
    private EditText fileNameView;
    private Button selectButton;
    private LinearLayout selectionButtonsContainer;
    private LinearLayout creationButtonsContainer;
    private InputMethodManager inputManager;

    private FileListAdapter adapter;
    private File clickedFile;
    private String currentPath = ROOT;
    private String parentPath;
    private HashMap<String, Integer> lastPositions = new HashMap<>();

    private PermissionVerifier permissionVerifier;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_picker);
        String[] permissions;
        if (selectionMode == SELECTION_MODE_CREATE) {
            permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        } else {
            permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        permissionVerifier = new PermissionVerifier(this, permissions);
        permissionVerifier.verifyPermissions(true, REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION);

        setResult(RESULT_CANCELED, getIntent());

        selectionMode = getIntent().getIntExtra(EXTRA_SELECTION_MODE, SELECTION_MODE_CREATE);
        formatFilters = getIntent().getStringArrayExtra(EXTRA_FORMAT_FILTER);
        canSelectDir = getIntent().getBooleanExtra(EXTRA_CAN_SELECT_DIR, false);

        inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        pathView = (TextView) findViewById(R.id.path);
        fileNameView = (EditText) findViewById(R.id.file_name);
        selectButton = (Button) findViewById(R.id.select_btn);
        selectButton.setEnabled(false);
        selectButton.setOnClickListener(v -> {
            if (clickedFile != null) {
                getIntent().putExtra(EXTRA_RESULT_PATH, clickedFile.getPath());
                setResult(RESULT_OK, getIntent());
                finish();
            }
        });
        Button newButton = (Button) findViewById(R.id.new_btn);
        newButton.setOnClickListener(v -> {
            setCreateVisible(v);
            fileNameView.setText("");
            fileNameView.requestFocus();
        });
        if (selectionMode == SELECTION_MODE_OPEN) {
            newButton.setEnabled(false);
        }

        selectionButtonsContainer = (LinearLayout) findViewById(R.id.selection_buttons_container);
        creationButtonsContainer = (LinearLayout) findViewById(R.id.creation_buttons_container);
        creationButtonsContainer.setVisibility(View.GONE);
        Button cancelButton = (Button) findViewById(R.id.cancel_btn);
        cancelButton.setOnClickListener(this::setSelectVisible);
        Button createButton = (Button) findViewById(R.id.create_btn);
        createButton.setOnClickListener(v -> {
            if (fileNameView.getText().length() > 0) {
                getIntent().putExtra(EXTRA_RESULT_PATH, currentPath + "/" + fileNameView.getText());
                setResult(RESULT_OK, getIntent());
                finish();
            }
        });

        String startPath = getIntent().getStringExtra(EXTRA_START_PATH);
        startPath = startPath != null ? startPath : ROOT;
        if (canSelectDir) {
            clickedFile = new File(startPath);
            selectButton.setEnabled(true);
        }
        setData(startPath);
    }

    private void setData(String dirPath) {
        boolean useAutoSelection = dirPath.length() < currentPath.length();
        List<DirUtils.FileInfo> files = null;
        try {
            files = DirUtils.getDirInfo(dirPath, formatFilters);
        } catch (DirReadException e) {
            e.printStackTrace();
            Toast.makeText(FileDialogActivity.this, "Unable to read folder " + dirPath, Toast.LENGTH_SHORT).show();
            try {
                dirPath = ROOT;
                files = DirUtils.getDirInfo(dirPath, formatFilters);
            } catch (DirReadException e1) {
                e1.printStackTrace();
            }
        }
        parentPath = null;
        if (files != null) {
            if (!dirPath.equals(ROOT)) {
                files.add(0, new DirUtils.FileInfo(ROOT, true));
                files.add(1, new DirUtils.FileInfo(PARENT, true));
                parentPath = new File(dirPath).getParent();
            }

            currentPath = dirPath;
            adapter = new FileListAdapter(this, files);
            setListAdapter(adapter);

            Integer position = lastPositions.get(parentPath);
            if (position != null && useAutoSelection) {
                getListView().setSelection(position);
            }
            pathView.setText(getText(R.string.location) + ": " + currentPath);
        } else {
            setListAdapter(null);
            adapter = null;
            pathView.setText(null);
        }
    }

    private static class FileListAdapter extends ArrayAdapter<DirUtils.FileInfo> {

        FileListAdapter(Context context, List<DirUtils.FileInfo> items) {
            super(context, 0, items);
        }

        private class ViewHolder {
            ImageView mIcon;
            TextView mFilename;
        }

        @Override
        public
        @NonNull
        View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view;
            ViewHolder holder;

            if (convertView == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.list_item_file_picker, parent, false);
                holder = new ViewHolder();
                holder.mIcon = (ImageView) view.findViewById(R.id.icon);
                holder.mFilename = (TextView) view.findViewById(R.id.file_name);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            }
            DirUtils.FileInfo fileInfo = getItem(position);
            holder.mIcon.setImageResource(fileInfo.isFolder ? R.drawable.folder : R.drawable.file);
            holder.mFilename.setText(fileInfo.path);
            return view;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String path = adapter.getItem(position).path;
        File file;
        if (path.equals(ROOT)) {
            file = new File(path);
        } else {
            file = new File(currentPath, path);
        }
        setSelectVisible(v);

        if (file.isDirectory()) {
            selectButton.setEnabled(false);
            if (file.canRead()) {
                lastPositions.put(currentPath, position);
                setData(file.getAbsolutePath());

                if (canSelectDir) {
                    clickedFile = file;
                    v.setSelected(true);
                    selectButton.setEnabled(true);
                }
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Folder [" + file.getName() + "] " + getText(R.string.cant_read_folder))
                        .setPositiveButton("OK", null).show();
            }
        } else {
            clickedFile = file;
            v.setSelected(true);
            selectButton.setEnabled(true);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            selectButton.setEnabled(false);
            if (creationButtonsContainer.getVisibility() == View.VISIBLE) {
                creationButtonsContainer.setVisibility(View.GONE);
                selectionButtonsContainer.setVisibility(View.VISIBLE);
            } else {
                if (parentPath != null) {
                    setData(parentPath);
                } else {
                    return super.onKeyDown(keyCode, event);
                }
            }
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void setCreateVisible(View v) {
        creationButtonsContainer.setVisibility(View.VISIBLE);
        selectionButtonsContainer.setVisibility(View.GONE);
        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        selectButton.setEnabled(false);
    }

    private void setSelectVisible(View v) {
        creationButtonsContainer.setVisibility(View.GONE);
        selectionButtonsContainer.setVisibility(View.VISIBLE);
        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        selectButton.setEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!permissionVerifier.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            Toast.makeText(this, "Missing permissions!", Toast.LENGTH_SHORT).show();
        }
    }
}
