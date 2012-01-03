package com.example.rssreader.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.view.View;
import com.example.rssreader.R;

import java.util.ArrayList;
import java.util.HashMap;

public class RssAdapter extends ArrayAdapter<HashMap> {

    public static final int STATE_NORMAL      = 1;
    public static final int STATE_EXPANDED    = 2;

    private final Context context;
    private ArrayList<HashMap> entities;

    public RssAdapter(Context context, ArrayList<HashMap> taskEntities, int resourceId) {
        super(context, resourceId, taskEntities);

        this.context = context;
        this.entities = taskEntities;

    }

    static class ViewHolder {
        public TextView descriptionView;
        public TextView dateView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        View rowView = convertView;
        if (rowView == null) {

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            rowView = inflater.inflate(R.layout.rss_item, parent, false);
            holder = new ViewHolder();
            holder.descriptionView = (TextView) rowView.findViewById(R.id.list_item_text);
            holder.dateView = (TextView) rowView.findViewById(R.id.list_item_date);

            rowView.setTag(holder);

        } else {
            holder = (ViewHolder) rowView.getTag();
        }

        holder.dateView.setText((String) entities.get(position).get("date"));
        if ((Integer) entities.get(position).get("state") == STATE_EXPANDED) {
            holder.descriptionView.setText((String) entities.get(position).get("description"));
        } else {
            holder.descriptionView.setText((String) entities.get(position).get("title"));
        }
        return rowView;
    }

    /**
     * Gets adapter elements count
     * @return
     */
    public int getCount() {
        return this.entities.size();
    }

    /**
     * Updates item
     * @param index
     * @return
     */
    public HashMap getItem(int index) {
        return this.entities.get(index);
    }

    /**
     * Updates item
     * @param index
     * @param newItem
     */
    public void updateItem(int index, HashMap newItem) {
        this.entities.set(index, newItem);
    }

    /**
     * Sets new elements
     * @param list
     */
    public void setAll(ArrayList<HashMap> list) {
        this.entities = list;
    }
}
