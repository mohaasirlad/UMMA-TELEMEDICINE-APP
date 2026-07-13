package com.example.ummatelemedicineapp.models;

public class Transaction {
    private String title;
    private String date;
    private String amount;
    private int iconRes;

    public Transaction(String title, String date, String amount, int iconRes) {
        this.title = title;
        this.date = date;
        this.amount = amount;
        this.iconRes = iconRes;
    }

    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getAmount() { return amount; }
    public int getIconRes() { return iconRes; }
}