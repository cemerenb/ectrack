package com.app.ectrack;

import com.google.firebase.Timestamp;

public class PatientMedicine {
    private String id;
    private String name;
    private String dosage; // e.g., "1 tablet"
    private String frequency; // e.g., "Twice a day"
    private Timestamp startDate;
    private Timestamp prescribedDate;
    private int durationDays;
    private String duration; // e.g., "7 gün"
    private String usageInstructions;
    private int totalPills; // Optional: to track remaining
    private int remainingPills;

    public PatientMedicine() {

    }

    public PatientMedicine(String id, String name, String dosage, String frequency, Timestamp startDate,
            int durationDays, int totalPills) {
        this.id = id;
        this.name = name;
        this.dosage = dosage;
        this.frequency = frequency;
        this.startDate = startDate;
        this.durationDays = durationDays;
        this.totalPills = totalPills;
        this.remainingPills = totalPills;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public int getTotalPills() {
        return totalPills;
    }

    public void setTotalPills(int totalPills) {
        this.totalPills = totalPills;
    }

    public int getRemainingPills() {
        return remainingPills;
    }

    public void setRemainingPills(int remainingPills) {
        this.remainingPills = remainingPills;
    }

    public Timestamp getPrescribedDate() {
        return prescribedDate;
    }

    public void setPrescribedDate(Timestamp prescribedDate) {
        this.prescribedDate = prescribedDate;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getUsageInstructions() {
        return usageInstructions;
    }

    public void setUsageInstructions(String usageInstructions) {
        this.usageInstructions = usageInstructions;
    }
}

