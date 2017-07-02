package com.gb.canibuythat.ui.dragndroplist;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;

public class DragNDropCursorAdapter extends SimpleCursorAdapter implements DragNDropAdapter {
    private int positionMapping[];
    private final int dragNDropHandleViewId;

    public DragNDropCursorAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to, int dragNDropHandleViewId) {
        super(context, layout, cursor, from, to, 0);
        this.dragNDropHandleViewId = dragNDropHandleViewId;
        setup();
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        Cursor cursor = super.swapCursor(c);
        positionMapping = null;
        setup();
        return cursor;
    }

    private void setup() {
        Cursor c = getCursor();
        if (c == null || !c.moveToFirst()) {
            return;
        }
        positionMapping = new int[c.getCount()];
        for (int i = 0; i < positionMapping.length; ++i) {
            positionMapping[i] = i;
        }
    }

    @Override
    public View getDropDownView(int position, View view, ViewGroup group) {
        return super.getDropDownView(positionMapping[position], view, group);
    }

    @Override
    public Object getItem(int position) {
        return super.getItem(positionMapping[position]);
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(positionMapping[position]);
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(positionMapping[position]);
    }

    @Override
    public View getView(int position, View view, ViewGroup group) {
        return super.getView(positionMapping[position], view, group);
    }

    @Override
    public boolean isEnabled(int position) {
        return super.isEnabled(positionMapping[position]);
    }

    @Override
    public void onItemDragStart(DragNDropListView parent, View view, int position, long id) {
        // nothing to do
    }

    @Override
    public void onItemDrop(DragNDropListView parent, View view, int startIndex, int endIndex, long id) {
        int position = positionMapping[startIndex];
        if (startIndex < endIndex) {
            System.arraycopy(positionMapping, startIndex + 1, positionMapping, startIndex, endIndex - startIndex);
        } else if (endIndex < startIndex) {
            System.arraycopy(positionMapping, endIndex, positionMapping, endIndex + 1, startIndex - endIndex);
        }
        positionMapping[endIndex] = position;
    }

    @Override
    public int getDragNDropHandleViewId() {
        return dragNDropHandleViewId;
    }
}
