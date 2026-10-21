
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
        String sql = "SELECT vehicle_number, mobile_number, vehicle_type, " +
                "slot_number, entry_time, exit_time, parking_minutes, " +
                "parking_hours, fee, payment_status, payment_time " +
                "FROM parking_history WHERE payment_status = 'PAID' " +
                "ORDER BY id";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            history.clear();

            while (rs.next()) {
                Timestamp entryTimestamp = rs.getTimestamp("entry_time");
                Timestamp exitTimestamp = rs.getTimestamp("exit_time");
                Timestamp paymentTimestamp = rs.getTimestamp("payment_time");

                if (entryTimestamp == null || exitTimestamp == null) {
                    continue;
                }

                LocalDateTime entry = entryTimestamp.toLocalDateTime();
                LocalDateTime exit = exitTimestamp.toLocalDateTime();

                long minutes = rs.getLong("parking_minutes");
                int hours = rs.getInt("parking_hours");
                double fee = rs.getDouble("fee");

                ParkingRecord record = new ParkingRecord(
                        rs.getString("vehicle_number"),
                        rs.getString("mobile_number"),
                        rs.getString("vehicle_type"),
                        rs.getString("slot_number"),
                        entry,
                        exit,
                        minutes,
                        hours,
                        fee
                );

                record.paid = true;

                if (paymentTimestamp != null) {
                    record.paymentTime = paymentTimestamp.toLocalDateTime();
                }

                history.add(record);
            }
        }
    }

    private void loadActiveVehicles() throws SQLException {
        String sql = "SELECT vehicle_number, mobile_number, vehicle_type, " +
                "slot_number, entry_time FROM active_vehicles";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String number = rs.getString("vehicle_number");
                String mobile = rs.getString("mobile_number");
                String type = rs.getString("vehicle_type");
                String slotLabel = rs.getString("slot_number");
                Timestamp entryTimestamp = rs.getTimestamp("entry_time");

                if (entryTimestamp == null) {
                    continue;
                }

                LocalDateTime entry = entryTimestamp.toLocalDateTime();

                for (ParkingSlot slot : slots) {
                    if (slot.getSlotLabel().equalsIgnoreCase(slotLabel)
                            && !slot.isOccupied()) {

                        Vehicle vehicle;

                        if (type.equalsIgnoreCase("Car")) {
                            vehicle = new Car(number, mobile);
                        } else {
                            vehicle = new Bike(number, mobile);
                        }

                        slot.parkVehicle(vehicle);
                        entryTimes.put(number, entry);
                        break;
                    }
                }
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
                return "This vehicle has an unpaid exit. Complete its payment first.";
            }
        }

        Vehicle vehicle;

        if (type.equalsIgnoreCase("Car")) {
            vehicle = new Car(number, mobile);
            type = "Car";
        } else {
            vehicle = new Bike(number, mobile);
            type = "Bike";
        }

        String requiredType = type.equals("Car") ? "C" : "B";

        for (ParkingSlot slot : slots) {
            if (slot.getSlotType().equals(requiredType)
                    && !slot.isOccupied()) {

                LocalDateTime entryTime = LocalDateTime.now();

                try {
                    DatabaseManager.saveActiveVehicle(
                            number, mobile, type,
                            slot.getSlotLabel(), entryTime
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
            return "Vehicle not found. Check the vehicle number and mobile number.";
        }

        Vehicle vehicle = slot.getVehicle();
        LocalDateTime entry = entryTimes.get(vehicle.getVehicleNumber());

        return "VEHICLE FOUND\n\n"
                + "Vehicle Number: " + vehicle.getVehicleNumber() + "\n"
                + "Mobile Number: " + vehicle.getMobileNumber() + "\n"
                + "Vehicle Type: "
                + (slot.getSlotType().equals("C") ? "Car" : "Bike") + "\n"
                + "Parking Slot: " + slot.getSlotLabel() + "\n"
                + "Entry Time: " + entry.format(formatter);
    }

    public String showParkedVehicles() {
        StringBuilder result = new StringBuilder();
        result.append("CURRENTLY PARKED VEHICLES\n\n");

        boolean found = false;

        for (ParkingSlot slot : slots) {
            Vehicle vehicle = slot.getVehicle();

            if (vehicle != null) {
                found = true;
                result.append("Slot: ").append(slot.getSlotLabel()).append("\n");
                result.append("Vehicle: ").append(vehicle.getVehicleNumber()).append("\n");
                result.append("Type: ")
                        .append(slot.getSlotType().equals("C") ? "Car" : "Bike")
                        .append("\n");
                result.append("Mobile: ").append(vehicle.getMobileNumber()).append("\n");
                result.append("Entry: ")
                        .append(entryTimes.get(vehicle.getVehicleNumber()).format(formatter))
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
        LocalDateTime exit = LocalDateTime.now();

        long minutes = Math.max(
                1, Duration.between(entry, exit).toMinutes());

        int hours = (int) Math.max(1, (minutes + 59) / 60);

        Vehicle vehicle = slot.getVehicle();
        double fee = vehicle.calculateFee(hours);

        ParkingRecord record = new ParkingRecord(
                vehicle.getVehicleNumber(),
                vehicle.getMobileNumber(),
                slot.getSlotType().equals("C") ? "Car" : "Bike",
                slot.getSlotLabel(),
                entry,
                exit,
                minutes,
                hours,
                fee
        );

        pendingPayments.put(key, fee);
        pendingRecords.put(key, record);

        return createReceipt(record) + "\n\nPayment is pending.";
    }

    private String createReceipt(ParkingRecord record) {
        return "PARKING RECEIPT\n\n"
                + "Vehicle Number: " + record.vehicleNumber + "\n"
                + "Vehicle Type: " + record.vehicleType + "\n"
                + "Parking Slot: " + record.slotLabel + "\n"
                + "Entry Time: " + record.entry.format(formatter) + "\n"
                + "Exit Time: " + record.exit.format(formatter) + "\n"
                + "Duration: " + record.minutes + " minute(s)\n"
                + "Billable Hours: " + record.hours + "\n"
                + "Hourly Rate: Rs. "
                + String.format("%.2f", record.fee / record.hours) + "\n"
                + "Total Fee: Rs. "
                + String.format("%.2f", record.fee);
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
                    record.fee,
                    paymentTime
            );

            DatabaseManager.removeActiveVehicle(key);

        } catch (SQLException e) {
            return "Database error: payment was not completed in the system.\n"
                    + e.getMessage();
        }

        record.paid = true;
        record.paymentTime = paymentTime;

        history.add(record);
        pendingPayments.remove(key);
        pendingRecords.remove(key);
        entryTimes.remove(key);
        slot.removeVehicle();

        return "Payment confirmed successfully!\n\n"
                + "Vehicle: " + record.vehicleNumber + "\n"
                + "Slot Released: " + record.slotLabel + "\n"
                + "Amount Collected: Rs. "
                + String.format("%.2f", record.fee);
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
                result.append("Vehicle: ").append(record.vehicleNumber).append("\n");
                result.append("Type: ").append(record.vehicleType).append("\n");
                result.append("Slot: ").append(record.slotLabel).append("\n");
                result.append("Entry: ").append(record.entry.format(formatter)).append("\n");
                result.append("Exit: ").append(record.exit.format(formatter)).append("\n");
                result.append("Duration: ").append(record.minutes).append(" minutes\n");
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
                double fee) {

            this.vehicleNumber = vehicleNumber;
            this.mobileNumber = mobileNumber;
            this.vehicleType = vehicleType;
            this.slotLabel = slotLabel;
            this.entry = entry;
            this.exit = exit;
            this.minutes = minutes;
            this.hours = hours;
            this.fee = fee;
            this.paid = false;
        }
    }
}
