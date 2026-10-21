package oop.project;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author SARIGA S
 */


public abstract class Vehicle {

    private String vehicleNumber;
    private String mobileNumber;

    public Vehicle(String vehicleNumber, String mobileNumber) {
        this.vehicleNumber = vehicleNumber;
        this.mobileNumber = mobileNumber;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public abstract double calculateFee(int hours);
}



