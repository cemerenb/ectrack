package com.app.ectrack;

import com.google.firebase.Timestamp;

public class Medicine {
    private String id;
    private String name;
    private String barcode;
    private String description;
    private int stock;
    private double price;
    private Timestamp expiryDate; // Using Firestore Timestamp

    private int piecesPerBox;
    private String medicineType;

    public Medicine() {

    }

    public Medicine(String id, String name, String barcode, String description, int stock, double price,
            Timestamp expiryDate, int piecesPerBox, String medicineType) {
        this.id = id;
        this.name = name;
        this.barcode = barcode;
        this.description = description;
        this.stock = stock;
        this.price = price;
        this.expiryDate = expiryDate;
        this.piecesPerBox = piecesPerBox;
        this.medicineType = medicineType;
    }

    public int getPiecesPerBox() {
        return piecesPerBox;
    }

    public void setPiecesPerBox(int piecesPerBox) {
        this.piecesPerBox = piecesPerBox;
    }

    public String getMedicineType() {
        return medicineType;
    }

    public void setMedicineType(String medicineType) {
        this.medicineType = medicineType;
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

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Timestamp getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Timestamp expiryDate) {
        this.expiryDate = expiryDate;
    }
}

