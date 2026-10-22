
package oop.project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ParkingManager {

    private final ArrayList<ParkingSlot> slots = new ArrayList<>();
    private final ArrayList<ParkingRecord> history = new ArrayList<>();

    private final Map<String, LocalDateTime> entryTimes = new HashMap<>();
    private final Map<String, Double> pendingPayments = new HashMap<>();
    private final Map<String, ParkingRecord> pendingRecords = new HashMap<>();

    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public ParkingManager() {
        for (int i = 1; i <= 5; i++) {
            slots.add(new ParkingSlot(i, "C"));
        }

        for (int i = 1; i <= 10; i++) {
            slots.add(new ParkingSlot(i, "B"));
        }

        try {
            DatabaseManager.initializeDatabase();
            loadParkingHistory();
            loadActiveVehicles();
        } catch (SQLException e) {
            System.err.println("Database loading failed: " + e.getMessage());
        }
    }

    private void loadParkingHistory() throws SQLException {
        String sql = "SELECT vehicle_number, mobile_number, vehicle_type, "
                + "slot_number, entry_time, exit_time, parking_minutes, "
                + "parking_hours, fee, payment_status, payment_time, "
                + "base_fee, overtime_hours, overtime_charge "
                + "FROM parking_history WHERE payment_status = 'PAID' "
                + "ORDER BY id";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            history.clear();

            while (rs.next()) {
                Timestamp entryTs = rs.getTimestamp("entry_time");
                Timestamp exitTs = rs.getTimestamp("exit_time");
                Timestamp paymentTs = rs.getTimestamp("payment_time");

                if (entryTs == null || exitTs == null) {
                    continue;
                }

                LocalDateTime entry = entryTs.toLocalDateTime();
                LocalDateTime exit = exitTs.toLocalDateTime();

                long minutes = rs.getLong("parking_minutes");
                int hours = rs.getInt("parking_hours");
                double fee = rs.getDouble("fee");
                double baseFee = rs.getDouble("base_fee");
                int overtimeHours = rs.getInt("overtime_hours");
                double overtimeCharge = rs.getDouble("overtime_charge");

                ParkingRecord record = new ParkingRecord(
                        rs.getString("vehicle_number"),
                        rs.getString("mobile_number"),
                        rs.getString("vehicle_type"),
                        rs.getString("slot_number"),
                        entry,
                        exit,
                        minutes,
                        hours,
                        baseFee,
                        overtimeHours,
                        overtimeCharge,
                        fee
                );

                record.paid = true;

                if (paymentTs != null) {
                    record.paymentTime = paymentTs.toLocalDateTime();
                }

                history.add(record);
            }
        }
    }

    private void loadActiveVehicles() throws SQLException {
        String sql = "SELECT vehicle_number, mobile_number, vehicle_type, "
                + "slot_number, entry_time FROM active_vehicles";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String number = rs.getString("vehicle_number");
                String mobile = rs.getString("mobile_number");
                String type = rs.getString("vehicle_type");
                String slotLabel = rs.getString("slot_number");
                Timestamp entryTs = rs.getTimestamp("entry_time");

                if (entryTs == null) {
                    continue;
                }

                LocalDateTime entry = entryTs.toLocalDateTime();
                ParkingSlot matchingSlot = null;

                for (ParkingSlot slot : slots) {
                    if (slot.getSlotLabel().equalsIgnoreCase(slotLabel)) {
                        matchingSlot = slot;
                        break;
                    }
                }

                if (matchingSlot == null) {
                    System.err.println("Unknown parking slot: " + slotLabel);
                    continue;
                }

                if (matchingSlot.isOccupied()) {
                    System.err.println("Duplicate occupied slot: " + slotLabel);
                    continue;
                }

                Vehicle vehicle;

                if (type.equalsIgnoreCase("Car")) {
                    vehicle = new Car(number, mobile);
                } else if (type.equalsIgnoreCase("Bike")) {
                    vehicle = new Bike(number, mobile);
                } else {
                    System.err.println("Unknown vehicle type: " + type);
                    continue;
                }

                matchingSlot.parkVehicle(vehicle);
                entryTimes.put(number, entry);
            }
        }
    }

    public String parkVehicle(String number, String mobile, String type) {
        if (number == null || mobile == null || type == null) {
            return "Please enter all vehicle details.";
        }

        number = number.trim().toUpperCase();
        mobile = mobile.trim();
        type = type.trim();

        if (number.isEmpty() || mobile.isEmpty()) {
            return "Please enter both vehicle number and mobile number.";
        }

        if (!mobile.matches("\\d{10}")) {
            return "Enter a valid 10-digit mobile number.";
        }

        if (!type.equalsIgnoreCase("Car")
                && !type.equalsIgnoreCase("Bike")) {
            return "Please select Car or Bike.";
        }

        for (ParkingSlot slot : slots) {
            Vehicle parked = slot.getVehicle();

            if (parked != null
                    && parked.getVehicleNumber().equalsIgnoreCase(number)) {
                return "This vehicle is already parked.";
            }
        }

        for (ParkingRecord record : pendingRecords.values()) {
            if (record.vehicleNumber.equalsIgnoreCase(number)) {
                return "This vehicle has an unpaid exit. "
                        + "Complete its payment first.";
            }
        }

        Vehicle vehicle;
        String requiredType;

        if (type.equalsIgnoreCase("Car")) {
            vehicle = new Car(number, mobile);
            type = "Car";
            requiredType = "C";
        } else {
            vehicle = new Bike(number, mobile);
            type = "Bike";
            requiredType = "B";
        }

        for (ParkingSlot slot : slots) {
            if (slot.getSlotType().equals(requiredType)
                    && !slot.isOccupied()) {

                LocalDateTime entryTime = LocalDateTime.now();

                try {
                    DatabaseManager.saveActiveVehicle(
                            number,
                            mobile,
                            type,
                            slot.getSlotLabel(),
                            entryTime
                    );
                } catch (SQLException e) {
                    return "Database error: vehicle was not saved.\n"
                            + e.getMessage();
                }

                slot.parkVehicle(vehicle);
                entryTimes.put(number, entryTime);

                return "Vehicle parked successfully!\n"
                        + "Vehicle Number: " + number + "\n"
                        + "Vehicle Type: " + type + "\n"
                        + "Parking Slot: " + slot.getSlotLabel() + "\n"
                        + "Entry Time: " + entryTime.format(formatter);
            }
        }

        return type + " parking is full!";
    }

    public ParkingSlot findParkedSlot(String number, String mobile) {
        if (number == null || mobile == null) {
            return null;
        }

        number = number.trim();
        mobile = mobile.trim();

        for (ParkingSlot slot : slots) {
            Vehicle vehicle = slot.getVehicle();

            if (vehicle != null
                    && vehicle.getVehicleNumber().equalsIgnoreCase(number)
                    && vehicle.getMobileNumber().equals(mobile)) {
                return slot;
            }
        }

        return null;
    }

    public String findVehicle(String number, String mobile) {
        ParkingSlot slot = findParkedSlot(number, mobile);

        if (slot == null) {
            return "Vehicle not found. Check the vehicle number "
                    + "and mobile number.";
        }

        Vehicle vehicle = slot.getVehicle();
        LocalDateTime entry = entryTimes.get(vehicle.getVehicleNumber());

        return "VEHICLE FOUND\n\n"
                + "Vehicle Number: " + vehicle.getVehicleNumber() + "\n"
                + "Mobile Number: " + vehicle.getMobileNumber() + "\n"
                + "Vehicle Type: "
                + (slot.getSlotType().equals("C") ? "Car" : "Bike") + "\n"
                + "Parking Slot: " + slot.getSlotLabel() + "\n"
                + "Entry Time: "
                + (entry == null ? "Unknown" : entry.format(formatter));
    }

    public String showParkedVehicles() {
        StringBuilder result = new StringBuilder();
        result.append("CURRENTLY PARKED VEHICLES\n\n");

        boolean found = false;

        for (ParkingSlot slot : slots) {
            Vehicle vehicle = slot.getVehicle();

            if (vehicle != null) {
                found = true;

                result.append("Slot: ")
                        .append(slot.getSlotLabel()).append("\n");
                result.append("Vehicle: ")
                        .append(vehicle.getVehicleNumber()).append("\n");
                result.append("Type: ")
                        .append(slot.getSlotType().equals("C") ? "Car" : "Bike")
                        .append("\n");
                result.append("Mobile: ")
                        .append(vehicle.getMobileNumber()).append("\n");

                LocalDateTime entry = entryTimes.get(
                        vehicle.getVehicleNumber());

                result.append("Entry: ")
                        .append(entry == null
                                ? "Unknown" : entry.format(formatter))
                        .append("\n\n");
            }
        }

        if (!found) {
            result.append("No vehicles are currently parked.");
        }

        return result.toString();
    }

    public String prepareExit(String number, String mobile) {
        ParkingSlot slot = findParkedSlot(number, mobile);

        if (slot == null) {
            return "Vehicle not found. Check both details.";
        }

        String key = slot.getVehicle().getVehicleNumber();

        if (pendingRecords.containsKey(key)) {
            return createReceipt(pendingRecords.get(key))
                    + "\n\nPayment is still pending.";
        }

        LocalDateTime entry = entryTimes.get(key);

        if (entry == null) {
            return "Entry time is missing. Please check the database.";
        }

        LocalDateTime exit = LocalDateTime.now();

        long minutes = Math.max(
                1, Duration.between(entry, exit).toMinutes());

        Vehicle vehicle = slot.getVehicle();

        ParkingFeeCalculator.FeeBreakdown breakdown =
                ParkingFeeCalculator.calculate(vehicle, entry, minutes);

        ParkingRecord record = new ParkingRecord(
                vehicle.getVehicleNumber(),
                vehicle.getMobileNumber(),
                slot.getSlotType().equals("C") ? "Car" : "Bike",
                slot.getSlotLabel(),
                entry,
                exit,
                minutes,
                breakdown.billableHours,
                breakdown.baseFee,
                breakdown.overtimeHours,
                breakdown.overtimeCharge,
                breakdown.totalFee
        );

        pendingPayments.put(key, record.fee);
        pendingRecords.put(key, record);

        return createReceipt(record) + "\n\nPayment is pending.";
    }

    private String createReceipt(ParkingRecord record) {
        return "========== PARKING RECEIPT ==========\n\n"
                + "Vehicle Number: " + record.vehicleNumber + "\n"
                + "Vehicle Type: " + record.vehicleType + "\n"
                + "Parking Slot: " + record.slotLabel + "\n"
                + "Entry Time: " + record.entry.format(formatter) + "\n"
                + "Exit Time: " + record.exit.format(formatter) + "\n"
                + "Duration: " + record.minutes + " minute(s)\n"
                + "Billable Hours: " + record.hours + "\n\n"
                + "Base Parking Fee: Rs. "
                + String.format("%.2f", record.baseFee) + "\n"
                + "Overtime Hours: " + record.overtimeHours + "\n"
                + "Overtime Penalty: Rs. "
                + String.format("%.2f", record.overtimeCharge) + "\n"
                + "-------------------------------------\n"
                + "TOTAL FEE: Rs. "
                + String.format("%.2f", record.fee) + "\n"
                + "=====================================";
    }

    public String confirmPayment(String number, String mobile) {
        ParkingSlot slot = findParkedSlot(number, mobile);

        if (slot == null) {
            return "Vehicle not found, or its slot has already been released.";
        }

        String key = slot.getVehicle().getVehicleNumber();

        if (!pendingPayments.containsKey(key)
                || !pendingRecords.containsKey(key)) {
            return "Please calculate the exit fee first.";
        }

        ParkingRecord record = pendingRecords.get(key);
        LocalDateTime paymentTime = LocalDateTime.now();

        try {
            DatabaseManager.saveParkingHistory(
                    record.vehicleNumber,
                    record.mobileNumber,
                    record.vehicleType,
                    record.slotLabel,
                    record.entry,
                    record.exit,
                    record.minutes,
                    record.hours,
                    record.baseFee,
                    record.overtimeHours,
                    record.overtimeCharge,
                    record.fee,
                    paymentTime
            );

            DatabaseManager.removeActiveVehicle(key);

        } catch (SQLException e) {
            return "Database error: payment was not completed.\n"
                    + e.getMessage()
                    + "\nPayment status: PENDING";
        }

        record.paid = true;
        record.paymentTime = paymentTime;

        history.add(record);
        pendingPayments.remove(key);
        pendingRecords.remove(key);
        entryTimes.remove(key);
        slot.removeVehicle();

        return "========== PARKING RECEIPT ==========\n\n"
                + "Vehicle Number: " + record.vehicleNumber + "\n"
                + "Vehicle Type: " + record.vehicleType + "\n"
                + "Parking Slot: " + record.slotLabel + "\n"
                + "Entry Time: " + record.entry.format(formatter) + "\n"
                + "Exit Time: " + record.exit.format(formatter) + "\n"
                + "Duration: " + record.minutes + " minute(s)\n"
                + "Billable Hours: " + record.hours + "\n\n"
                + "Base Parking Fee: Rs. "
                + String.format("%.2f", record.baseFee) + "\n"
                + "Overtime Hours: " + record.overtimeHours + "\n"
                + "Overtime Penalty: Rs. "
                + String.format("%.2f", record.overtimeCharge) + "\n"
                + "-------------------------------------\n"
                + "TOTAL FEE: Rs. "
                + String.format("%.2f", record.fee) + "\n"
                + "PAYMENT STATUS: PAID\n"
                + "Payment Time: " + paymentTime.format(formatter) + "\n"
                + "=====================================\n"
                + "Thank you for parking with us!";
    }

    public int getAvailableSlotCount(String type) {
        int count = 0;

        for (ParkingSlot slot : slots) {
            if (slot.getSlotType().equalsIgnoreCase(type)
                    && !slot.isOccupied()) {
                count++;
            }
        }

        return count;
    }

    public int getOccupiedSlotCount(String type) {
        int count = 0;

        for (ParkingSlot slot : slots) {
            if (slot.getSlotType().equalsIgnoreCase(type)
                    && slot.isOccupied()) {
                count++;
            }
        }

        return count;
    }

    public int getTotalSlotCount(String type) {
        if (type.equalsIgnoreCase("C")) {
            return 5;
        }

        if (type.equalsIgnoreCase("B")) {
            return 10;
        }

        return 0;
    }

    public ArrayList<ParkingSlot> getSlots() {
        return slots;
    }

    public String getHistoryReport() {
        StringBuilder result = new StringBuilder();
        result.append("PARKING HISTORY & REVENUE\n\n");

        if (history.isEmpty()) {
            result.append("No completed paid parking sessions yet.\n");
        } else {
            for (ParkingRecord record : history) {
                result.append("Vehicle: ")
                        .append(record.vehicleNumber).append("\n");
                result.append("Type: ")
                        .append(record.vehicleType).append("\n");
                result.append("Slot: ")
                        .append(record.slotLabel).append("\n");
                result.append("Entry: ")
                        .append(record.entry.format(formatter)).append("\n");
                result.append("Exit: ")
                        .append(record.exit.format(formatter)).append("\n");
                result.append("Duration: ")
                        .append(record.minutes).append(" minutes\n");
                result.append("Base Fee: Rs. ")
                        .append(String.format("%.2f", record.baseFee))
                        .append("\n");
                result.append("Overtime Hours: ")
                        .append(record.overtimeHours).append("\n");
                result.append("Overtime Penalty: Rs. ")
                        .append(String.format("%.2f", record.overtimeCharge))
                        .append("\n");
                result.append("Fee Paid: Rs. ")
                        .append(String.format("%.2f", record.fee)).append("\n");
                result.append("Payment Status: PAID\n\n");
            }
        }

        result.append("Paid Sessions: ").append(history.size()).append("\n");
        result.append("Total Revenue: Rs. ")
                .append(String.format("%.2f", getTotalRevenue()));

        return result.toString();
    }

    public double getTotalRevenue() {
        double total = 0;

        for (ParkingRecord record : history) {
            if (record.paid) {
                total += record.fee;
            }
        }

        return total;
    }

    public static class ParkingRecord {
        public String vehicleNumber;
        public String mobileNumber;
        public String vehicleType;
        public String slotLabel;
        public LocalDateTime entry;
        public LocalDateTime exit;
        public long minutes;
        public int hours;
        public double baseFee;
        public int overtimeHours;
        public double overtimeCharge;
        public double fee;
        public boolean paid;
        public LocalDateTime paymentTime;

        public ParkingRecord(
                String vehicleNumber,
                String mobileNumber,
                String vehicleType,
                String slotLabel,
                LocalDateTime entry,
                LocalDateTime exit,
                long minutes,
                int hours,
                double baseFee,
                int overtimeHours,
                double overtimeCharge,
                double fee) {

            this.vehicleNumber = vehicleNumber;
            this.mobileNumber = mobileNumber;
            this.vehicleType = vehicleType;
            this.slotLabel = slotLabel;
            this.entry = entry;
            this.exit = exit;
            this.minutes = minutes;
            this.hours = hours;
            this.baseFee = baseFee;
            this.overtimeHours = overtimeHours;
            this.overtimeCharge = overtimeCharge;
            this.fee = fee;
            this.paid = false;
        }
    }
}
