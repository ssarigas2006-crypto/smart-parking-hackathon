
package oop.project;

import java.time.LocalDateTime;

public class ParkingFeeCalculator {

    public static final int OVERTIME_LIMIT_HOURS = 6;

    public static class FeeBreakdown {
        public final int billableHours;
        public final int overtimeHours;
        public final double baseFee;
        public final double overtimeCharge;
        public final double totalFee;

        public FeeBreakdown(
                int billableHours,
                int overtimeHours,
                double baseFee,
                double overtimeCharge) {

            this.billableHours = billableHours;
            this.overtimeHours = overtimeHours;
            this.baseFee = baseFee;
            this.overtimeCharge = overtimeCharge;
            this.totalFee = baseFee + overtimeCharge;
        }
    }

    public static FeeBreakdown calculate(
            Vehicle vehicle,
            LocalDateTime entryTime,
            long parkingMinutes) {

        int hours = (int) Math.max(
                1, (parkingMinutes + 59) / 60);

        double baseFee = vehicle.calculateFee(hours, entryTime);

        int overtimeHours = Math.max(
                0, hours - OVERTIME_LIMIT_HOURS);

        double overtimeCharge = 0;

        if (parkingMinutes > OVERTIME_LIMIT_HOURS * 60L) {
            if (vehicle instanceof Car) {
                overtimeCharge = 100.0;
            } else if (vehicle instanceof Bike) {
                overtimeCharge = 50.0;
            }
        }

        return new FeeBreakdown(
                hours,
                overtimeHours,
                baseFee,
                overtimeCharge);
    }
}
