# Smart Parking Management System

A Java-based Smart Parking Management System built using Object-Oriented Programming (OOP), Java Swing, and MySQL. This project manages parking slots, vehicle entries and exits, parking fees, and payment records.

## Features

- 🚗 Vehicle management for cars and bikes
- 🅿️ Parking slot management
- 💰 Dynamic parking fee calculation based on vehicle type and time of day
- ⏱️ Parking duration calculation with rounded-up billable hours
- ⚠️ Overtime penalty for parking beyond the specified time limit
- 🧾 Detailed exit receipt with fee breakdown and payment status
- 💳 Payment processing and payment-time recording
- 📊 Parking history and fee records
- 🖥️ Java Swing graphical user interface

## Dynamic Parking Fee Rates

| Time Period | Car (per hour) | Bike (per hour) |
|---|---:|---:|
| Morning (6 AM–12 PM) | ₹40 | ₹15 |
| Noon (12 PM–4 PM) | ₹50 | ₹20 |
| Evening (4 PM–8 PM) | ₹60 | ₹25 |
| Night (8 PM–6 AM) | ₹30 | ₹10 |

### Overtime Charges

The system applies a one-time overtime penalty when actual parking duration exceeds 6 hours.

- **Car:** ₹100 penalty
- **Bike:** ₹50 penalty

Billable hours are rounded up, with a minimum charge of one hour.

## Technologies Used

- Java
- Object-Oriented Programming (OOP)
- Java Swing
- MySQL
- JDBC
- Apache Ant / NetBeans project structure

## Requirements

- Java Development Kit (JDK)
- NetBeans IDE or another compatible Java IDE
- MySQL Server
- MySQL JDBC driver

## Setup

1. Clone or download this repository.
2. Open the project in NetBeans IDE.
3. Create a MySQL database named `smart_parking`.
4. Import or create the required database tables using your database backup/schema.
5. Configure the MySQL connection and JDBC driver for your environment.
6. Set the database password using the `SMART_PARKING_DB_PASSWORD` environment variable.
7. Build and run the project from NetBeans.

## Database

The application uses MySQL to store parking slot information, active vehicle records, and parking history, including fee breakdown and payment details.

Make sure your database schema matches the tables and columns used by the application before running it.

## Project Structure

- `src/oop/project/` — Java source files
- `nbproject/` — NetBeans project configuration
- `build.xml` — Ant build configuration
- `.gitignore` — Excludes generated and local files from version control

## Purpose

This project demonstrates Java OOP concepts, database connectivity, desktop GUI development, and dynamic parking fee calculation.

## Author

Developed as a Smart Parking Management System project and extended with dynamic time-based parking fees and overtime penalties for a hackathon.
