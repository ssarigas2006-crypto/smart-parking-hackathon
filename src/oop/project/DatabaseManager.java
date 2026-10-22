
package oop.project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class DatabaseManager {

    private static final String URL =
            "jdbc:mysql://localhost:3306/smart_parking";
    private static final String USER = "root";

    private static final String PASSWORD =
            System.getenv("SMART_PARKING_DB_PASSWORD");

    public static Connection getConnection() throws SQLException {
        if (PASSWORD == null || PASSWORD.isEmpty()) {
            throw new SQLException(
                    "SMART_PARKING_DB_PASSWORD environment variable is not set.");
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initializeDatabase() throws SQLException {
        try (Connection con = getConnection();
             Statement st = con.createStatement()) {

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS active_vehicles (" +
                "vehicle_number VARCHAR(20) PRIMARY KEY," +
                "mobile_number VARCHAR(15) NOT NULL," +
                "vehicle_type VARCHAR(10) NOT NULL," +
                "slot_number VARCHAR(10) NOT NULL UNIQUE," +
                "entry_time DATETIME NOT NULL)"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS parking_history (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "vehicle_number VARCHAR(20) NOT NULL," +
                "mobile_number VARCHAR(15) NOT NULL," +
                "vehicle_type VARCHAR(10) NOT NULL," +
                "slot_number VARCHAR(10) NOT NULL," +
                "entry_time DATETIME NOT NULL," +
                "exit_time DATETIME NOT NULL," +
                "parking_minutes BIGINT," +
                "parking_hours INT," +
                "fee DECIMAL(10,2) NOT NULL DEFAULT 0.00," +
                "payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'," +
                "payment_time DATETIME," +
                "base_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00," +
                "overtime_hours INT NOT NULL DEFAULT 0," +
                "overtime_charge DECIMAL(10,2) NOT NULL DEFAULT 0.00)"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS parking_slots (" +
                "slot_number VARCHAR(10) PRIMARY KEY," +
                "slot_type VARCHAR(10) NOT NULL," +
                "occupied BOOLEAN NOT NULL DEFAULT FALSE)"
            );

            for (int i = 1; i <= 5; i++) {
                st.executeUpdate(
                    "INSERT IGNORE INTO parking_slots " +
                    "(slot_number, slot_type, occupied) VALUES " +
                    "('C" + i + "', 'C', FALSE)"
                );
            }

            for (int i = 1; i <= 10; i++) {
                st.executeUpdate(
                    "INSERT IGNORE INTO parking_slots " +
                    "(slot_number, slot_type, occupied) VALUES " +
                    "('B" + i + "', 'B', FALSE)"
                );
            }

            // Synchronize database slot status with active vehicles.
            st.executeUpdate(
                "UPDATE parking_slots SET occupied = FALSE"
            );

            st.executeUpdate(
                "UPDATE parking_slots s " +
                "JOIN active_vehicles a " +
                "ON s.slot_number = a.slot_number " +
                "SET s.occupied = TRUE"
            );
        }
    }

    public static void saveActiveVehicle(
            String number, String mobile, String type,
            String slot, LocalDateTime entry) throws SQLException {

        try (Connection con = getConnection()) {
            con.setAutoCommit(false);

            try {
                String sql =
                    "INSERT INTO active_vehicles " +
                    "(vehicle_number, mobile_number, vehicle_type, " +
                    "slot_number, entry_time) VALUES (?, ?, ?, ?, ?)";

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, number);
                    ps.setString(2, mobile);
                    ps.setString(3, type);
                    ps.setString(4, slot);
                    ps.setTimestamp(5, Timestamp.valueOf(entry));
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE parking_slots SET occupied = TRUE " +
                        "WHERE slot_number = ? AND occupied = FALSE")) {
                    ps.setString(1, slot);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Slot " + slot +
                                " is already occupied or does not exist.");
                    }
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public static void removeActiveVehicle(String number)
            throws SQLException {

        try (Connection con = getConnection()) {
            con.setAutoCommit(false);

            try {
                String slot = null;

                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT slot_number FROM active_vehicles " +
                        "WHERE vehicle_number = ? FOR UPDATE")) {
                    ps.setString(1, number);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            slot = rs.getString("slot_number");
                        }
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM active_vehicles " +
                        "WHERE vehicle_number = ?")) {
                    ps.setString(1, number);
                    ps.executeUpdate();
                }

                if (slot != null) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE parking_slots SET occupied = FALSE " +
                            "WHERE slot_number = ?")) {
                        ps.setString(1, slot);
                        ps.executeUpdate();
                    }
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    // Saves the full dynamic fee breakdown after payment confirmation.
    public static void saveParkingHistory(
            String number,
            String mobile,
            String type,
            String slot,
            LocalDateTime entry,
            LocalDateTime exit,
            long minutes,
            int hours,
            double baseFee,
            int overtimeHours,
            double overtimeCharge,
            double totalFee,
            LocalDateTime paymentTime) throws SQLException {

        String sql =
            "INSERT INTO parking_history " +
            "(vehicle_number, mobile_number, vehicle_type, slot_number, " +
            "entry_time, exit_time, parking_minutes, parking_hours, " +
            "base_fee, overtime_hours, overtime_charge, fee, " +
            "payment_status, payment_time) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PAID', ?)";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, number);
            ps.setString(2, mobile);
            ps.setString(3, type);
            ps.setString(4, slot);
            ps.setTimestamp(5, Timestamp.valueOf(entry));
            ps.setTimestamp(6, Timestamp.valueOf(exit));
            ps.setLong(7, minutes);
            ps.setInt(8, hours);
            ps.setDouble(9, baseFee);
            ps.setInt(10, overtimeHours);
            ps.setDouble(11, overtimeCharge);
            ps.setDouble(12, totalFee);
            ps.setTimestamp(13, Timestamp.valueOf(paymentTime));

            ps.executeUpdate();
        }
    }

    // Backward-compatible overload for any older calls.
    public static void saveParkingHistory(
            String number, String mobile, String type, String slot,
            LocalDateTime entry, LocalDateTime exit,
            long minutes, int hours, double fee,
            LocalDateTime paymentTime) throws SQLException {

        saveParkingHistory(
                number, mobile, type, slot, entry, exit,
                minutes, hours, fee, 0, 0.0, fee, paymentTime
        );
    }

    public static ResultSet getParkingHistory(Connection con)
            throws SQLException {

        String sql =
                "SELECT * FROM parking_history " +
                "WHERE payment_status = 'PAID' ORDER BY id";

        PreparedStatement ps = con.prepareStatement(sql);
        return ps.executeQuery();
    }

    public static double getTotalRevenueFromDatabase()
            throws SQLException {

        String sql =
                "SELECT COALESCE(SUM(fee), 0) AS total " +
                "FROM parking_history WHERE payment_status = 'PAID'";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getDouble("total");
            }
        }

        return 0.0;
    }

    public static void main(String[] args) {
        try {
            initializeDatabase();

            System.out.println("MySQL connected successfully!");
            System.out.println("Parking database initialized!");
            System.out.printf(
                "Total revenue: Rs. %.2f%n",
                getTotalRevenueFromDatabase()
            );

        } catch (SQLException e) {
            System.out.println("Database operation failed!");
            System.out.println(e.getMessage());
        }
    }
}
