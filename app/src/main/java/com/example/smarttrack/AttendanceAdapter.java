package com.example.smarttrack;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class AttendanceAdapter extends ArrayAdapter<String> {
    private Context context;
    private List<String> attendanceRecords;

    public AttendanceAdapter(Context context, List<String> attendanceRecords) {
        super(context, android.R.layout.simple_list_item_1, attendanceRecords);
        this.context = context;
        this.attendanceRecords = attendanceRecords;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Create the TextView dynamically
        TextView textView = new TextView(context);
        textView.setPadding(24, 24, 24, 24); // Padding inside the "card"
        textView.setTextSize(16); // Font size for the content
        textView.setTextColor(Color.BLACK);
        textView.setGravity(Gravity.START);

        // Set text content
        textView.setText(attendanceRecords.get(position));

        // Create a card-like background with rounded corners
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE); // Background color
        background.setCornerRadius(16); // Rounded corners
        background.setStroke(2, Color.LTGRAY); // Optional border color
        textView.setBackground(background);

        // Add elevation-like shadow effect (for versions supporting it)
        textView.setElevation(4);

        return textView;
    }
}
