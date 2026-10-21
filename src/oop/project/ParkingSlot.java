
package oop.project;

public class ParkingSlot {

    private int slotNumber;
    private String slotType;
    private boolean occupied;
    private Vehicle vehicle;

    public ParkingSlot(int slotNumber, String slotType) {
        this.slotNumber = slotNumber;
        this.slotType = slotType;
        this.occupied = false;
        this.vehicle = null;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public String getSlotType() {
        return slotType;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public String getSlotLabel() {
        return slotType + slotNumber;
    }

    public void parkVehicle(Vehicle vehicle) {
        if (occupied) {
            throw new IllegalStateException("This slot is already occupied.");
        }

        this.vehicle = vehicle;
        this.occupied = true;
    }

    public void removeVehicle() {
        this.vehicle = null;
        this.occupied = false;
    }
}