


package oop.project;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainFrame extends JFrame {

    private final ParkingManager manager = new ParkingManager();

    private JTextField vehicleField;
    private JTextField mobileField;
    private JComboBox<String> typeBox;
    private JTextArea outputArea;

    private JLabel carStatus;
    private JLabel bikeStatus;
    private JPanel carSlotsPanel;
    private JPanel bikeSlotsPanel;

    public MainFrame() {
        setTitle("Smart Parking Management System");
        setSize(1050, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        createInterface();
        refreshDashboard();
    }

    private void createInterface() {
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(245, 247, 250));
        setContentPane(mainPanel);

        JLabel heading = new JLabel("SMART PARKING MANAGEMENT SYSTEM");
        heading.setFont(new Font("SansSerif", Font.BOLD, 24));
        heading.setForeground(new Color(35, 55, 80));
        mainPanel.add(heading, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(12, 12));
        centerPanel.setOpaque(false);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setPreferredSize(new Dimension(330, 500));
        leftPanel.setOpaque(false);
        centerPanel.add(leftPanel, BorderLayout.WEST);

        leftPanel.add(createInputPanel());
        leftPanel.add(Box.createVerticalStrut(12));
        leftPanel.add(createActionsPanel());
        leftPanel.add(Box.createVerticalStrut(12));
        leftPanel.add(createDashboardPanel());

        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setOpaque(false);
        centerPanel.add(rightPanel, BorderLayout.CENTER);

        JPanel parkingPanel = new JPanel();
        parkingPanel.setLayout(new BoxLayout(parkingPanel, BoxLayout.Y_AXIS));
        parkingPanel.setBackground(Color.WHITE);
        parkingPanel.setBorder(BorderFactory.createTitledBorder(
                "LIVE PARKING LAYOUT"));

        carSlotsPanel = new JPanel(new GridLayout(1, 5, 6, 6));
        bikeSlotsPanel = new JPanel(new GridLayout(2, 5, 6, 6));

        carSlotsPanel.setBackground(Color.WHITE);
        bikeSlotsPanel.setBackground(Color.WHITE);

        parkingPanel.add(new JLabel("  CAR PARKING ZONE"));
        parkingPanel.add(Box.createVerticalStrut(8));
        parkingPanel.add(carSlotsPanel);
        parkingPanel.add(Box.createVerticalStrut(20));
        parkingPanel.add(new JLabel("  BIKE PARKING ZONE"));
        parkingPanel.add(Box.createVerticalStrut(8));
        parkingPanel.add(bikeSlotsPanel);

        rightPanel.add(parkingPanel, BorderLayout.NORTH);

        outputArea = new JTextArea(12, 40);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("SYSTEM OUTPUT"));
        rightPanel.add(scrollPane, BorderLayout.CENTER);

        JButton clearButton = new JButton("Clear Output");
        clearButton.addActionListener(e -> outputArea.setText(""));
        rightPanel.add(clearButton, BorderLayout.SOUTH);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("VEHICLE DETAILS"));

        vehicleField = new JTextField();
        mobileField = new JTextField();
        typeBox = new JComboBox<>(new String[]{"Car", "Bike"});

        panel.add(new JLabel("Vehicle Number"));
        panel.add(vehicleField);
        panel.add(new JLabel("Registered Mobile Number"));
        panel.add(mobileField);
        panel.add(new JLabel("Vehicle Type"));
        panel.add(typeBox);

        return panel;
    }

    private JPanel createActionsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 7, 7));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("ACTIONS"));

        JButton parkButton = new JButton("1. Park Vehicle");
        JButton exitButton = new JButton("2. Exit Vehicle");
        JButton showButton = new JButton("3. Show Vehicles");
        JButton findButton = new JButton("4. Find My Vehicle");
        JButton historyButton = new JButton("History & Revenue");

        parkButton.addActionListener(e -> parkVehicle());
        exitButton.addActionListener(e -> exitVehicle());
        showButton.addActionListener(e ->
                showOutput(manager.showParkedVehicles()));
        findButton.addActionListener(e -> findVehicle());
        historyButton.addActionListener(e ->
                showOutput(manager.getHistoryReport()));

        panel.add(parkButton);
        panel.add(exitButton);
        panel.add(showButton);
        panel.add(findButton);
        panel.add(historyButton);

        return panel;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("LIVE DASHBOARD"));

        carStatus = new JLabel();
        bikeStatus = new JLabel();

        panel.add(carStatus);
        panel.add(bikeStatus);

        return panel;
    }

    private void parkVehicle() {
        String result = manager.parkVehicle(
                vehicleField.getText(),
                mobileField.getText(),
                (String) typeBox.getSelectedItem());

        showOutput(result);
        refreshDashboard();
    }

    private void exitVehicle() {
        String number = vehicleField.getText().trim();
        String mobile = mobileField.getText().trim();

        String receipt = manager.prepareExit(number, mobile);
        showOutput(receipt);

        if (receipt.startsWith("PARKING RECEIPT")) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    receipt + "\n\nHas the customer paid?",
                    "Exit & Payment",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                showOutput(manager.confirmPayment(number, mobile));
                refreshDashboard();
            } else {
                showOutput("Payment not confirmed. The slot remains occupied.");
            }
        }
    }

    private void findVehicle() {
        String result = manager.findVehicle(
                vehicleField.getText(),
                mobileField.getText());

        showOutput(result);
    }

    private void showOutput(String message) {
        outputArea.setText(message);
        outputArea.setCaretPosition(0);
    }

    private void refreshDashboard() {
        carStatus.setText("Cars: "
                + manager.getOccupiedSlotCount("C") + " occupied / "
                + manager.getAvailableSlotCount("C") + " available");

        bikeStatus.setText("Bikes: "
                + manager.getOccupiedSlotCount("B") + " occupied / "
                + manager.getAvailableSlotCount("B") + " available");

        carSlotsPanel.removeAll();
        bikeSlotsPanel.removeAll();

        for (ParkingSlot slot : manager.getSlots()) {
            JPanel slotPanel = new JPanel(new BorderLayout(2, 5));
            slotPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            slotPanel.setPreferredSize(new Dimension(65, 75));

            JLabel name = new JLabel(slot.getSlotLabel(), SwingConstants.CENTER);
            name.setFont(new Font("SansSerif", Font.BOLD, 14));

            JLabel status = new JLabel(
                    slot.isOccupied() ? "OCCUPIED" : "AVAILABLE",
                    SwingConstants.CENTER);
            status.setFont(new Font("SansSerif", Font.PLAIN, 10));

            if (slot.isOccupied()) {
                slotPanel.setBackground(new Color(255, 210, 210));
                name.setForeground(new Color(150, 25, 25));
            } else {
                slotPanel.setBackground(new Color(210, 245, 220));
                name.setForeground(new Color(25, 110, 55));
            }

            slotPanel.add(name, BorderLayout.CENTER);
            slotPanel.add(status, BorderLayout.SOUTH);

            if (slot.getSlotType().equals("C")) {
                carSlotsPanel.add(slotPanel);
            } else {
                bikeSlotsPanel.add(slotPanel);
            }
        }

        carSlotsPanel.revalidate();
        carSlotsPanel.repaint();
        bikeSlotsPanel.revalidate();
        bikeSlotsPanel.repaint();
    }
}



