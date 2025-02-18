package com.example.smarttrack;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Event implements Serializable {
    private String id; // Firestore document ID
    private String eventDate;
    private String title;
    private String description;
    private String location;
    private String startTime;
    private String endTime;
    private boolean notify;
    private boolean wholeDay;
    private String students;
    private String teacherId; // 🔥 ADD THIS FIELD

    private List<String> rooms;

    // Default constructor required for Firestore
    public Event() {}

    public Event(String id, String eventDate, String title, String description, String location,
                 String startTime, String endTime, boolean notify, boolean wholeDay,
                 String students, String teacherId, List<String> rooms) {
        this.id = id;
        this.eventDate = eventDate;
        this.title = title;
        this.description = description;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.notify = notify;
        this.wholeDay = wholeDay;
        this.students = students;
        this.teacherId = teacherId; // 🔥 Initialize teacherId
        this.rooms = rooms;
    }

    // ✅ Getter and Setter for teacherId
    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    // ✅ Existing Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public boolean isNotify() {
        return notify;
    }

    public void setNotify(boolean notify) {
        this.notify = notify;
    }

    public boolean isWholeDay() {
        return wholeDay;
    }

    public void setWholeDay(boolean wholeDay) {
        this.wholeDay = wholeDay;
    }

    public String getStudents() {
        return students;
    }

    public void setStudents(String students) {
        this.students = students;
    }

    public List<String> getRooms() {
        return rooms;
    }

    public void setRooms(List<String> rooms) {
        this.rooms = rooms;
    }

    public long getEventTimestamp() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date eventDate = sdf.parse(this.eventDate + " " + this.startTime);
            return eventDate != null ? eventDate.getTime() : 0;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
