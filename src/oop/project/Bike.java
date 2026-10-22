
package oop.project;

import java.time.LocalDateTime;

public class Bike extends Vehicle {

    public Bike(String vehicleNumber, String mobileNumber) {
        super(vehicleNumber, mobileNumber);
    }

    @Override
    public double calculateFee(int hours, LocalDateTime entryTime) {
        double total = 0;

        for (int i = 0; i < hours; i++) {
            int hourOfDay = entryTime.plusHours(i).getHour();

            if (hourOfDay >= 6 && hourOfDay < 12) {
                total += 15;       // Morning
            } else if (hourOfDay >= 12 && hourOfDay < 16) {
                total += 20;       // Noon
            } else if (hourOfDay >= 16 && hourOfDay < 20) {
                total += 25;       // Evening
            } else {
                total += 10;       // Night
            }
        }

        return total;
    }
}
