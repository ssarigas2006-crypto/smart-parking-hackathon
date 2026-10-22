
package oop.project;

import java.time.LocalDateTime;

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

    public abstract double calculateFee(
            int hours, LocalDateTime entryTime);
}
