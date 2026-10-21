/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


package oop.project;

public class Car extends Vehicle {

    public Car(String vehicleNumber, String mobileNumber) {
        super(vehicleNumber, mobileNumber);
    }

    @Override
    public double calculateFee(int hours) {
        return Math.max(0, hours) * 50.0;
    }
}


