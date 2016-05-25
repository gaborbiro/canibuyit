package com.gb.canibuythat.ui.dragndroplist;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;

public class DragNDropCursorAdapter extends SimpleCursorAdapter implements DragNDropAdapter {
    private int mPositionMapping[];
    private final int mDragNDropHandleViewId;

    public DragNDropCursorAdapter(Context context, int layout, Cursor cursor, String[] from,
                                  int[] to, int dragNDropHandleViewId) {

        super(context, layout, cursor, from, to, 0);
        mDragNDropHandleViewId = dragNDropHandleViewId;
        setup();
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        Cursor cursor = super.swapCursor(c);
        mPositionMapping = null;
        setup();
        return cursor;
    }

    private void setup() {
        Cursor c = getCursor();

        if (c == null || !c.moveToFirst()) {
            return;
        }

        mPositionMapping = new int[c.getCount()];

        for (int i = 0; i < mPositionMapping.length; ++i) {
            mPositionMapping[i] = i;
        }
    }

    @Override
    public View getDropDownView(int position, View view, ViewGroup group) {
        return super.getDropDownView(mPositionMapping[position], view, group);
    }

    @Override
    public Object getItem(int position) {
        return super.getItem(mPositionMapping[position]);
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(mPositionMapping[position]);
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(mPositionMapping[position]);
    }

    @Override
    public View getView(int position, View view, ViewGroup group) {
        return super.getView(mPositionMapping[position], view, group);
    }

    @Override
    public boolean isEnabled(int position) {
        return super.isEnabled(mPositionMapping[position]);
    }

    @Override
    public void onItemDragStart(DragNDropListView parent, View view, int position, long id) {

    }

    @Override
    public void onItemDrop(DragNDropListView parent, View view, int startIndex, int endIndex, long id) {
        int position = mPositionMapping[startIndex];

        if (startIndex < endIndex) {
            System.arraycopy(mPositionMapping, startIndex + 1, mPositionMapping, startIndex, endIndex - startIndex);
        } else if (endIndex < startIndex) {
            System.arraycopy(mPositionMapping, endIndex, mPositionMapping, endIndex + 1, startIndex - endIndex);
        }

        mPositionMapping[endIndex] = position;
    }

    @Override
    public int getDragNDropHandleViewId() {
        return mDragNDropHandleViewId;
    }
}
