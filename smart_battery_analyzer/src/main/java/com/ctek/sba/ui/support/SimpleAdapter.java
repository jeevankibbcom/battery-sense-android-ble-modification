package com.ctek.sba.ui.support;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class SimpleAdapter extends BaseAdapter {

  private final LayoutInflater layoutInflater;

  public SimpleAdapter(Context context) {
    layoutInflater = LayoutInflater.from(context);
  }

  @Override
  public final View getView(int position, View convertView, ViewGroup parent) {
    View view = convertView;

    if (view == null) {
      view = createView(position, parent);
    }

    updateView(view, position);
    return view;
  }

  protected abstract View createView(int position, ViewGroup parent);

  protected abstract void updateView(View view, int position);

  protected LayoutInflater getLayoutInflater() {
    return layoutInflater;
  }

}
