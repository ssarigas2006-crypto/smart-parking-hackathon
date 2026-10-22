# Smart Parking Management System

A Java-based Smart Parking Management System built with Object-Oriented Programming (OOP), Java Swing, and MySQL.

## Features

- Vehicle management for cars and bikes
- Parking slot management
- Dynamic parking fees based on vehicle type and time of day
- Parking duration calculation with rounded-up billable hours
- Overtime penalties for parking longer than 6 hours
- Detailed exit receipts with fee breakdown and payment status
- Payment records and parking history

## Dynamic Parking Fees

| Time Period | Car per Hour | Bike per Hour |
|---|---:|---:|
| Morning (6 AM–12 PM) | ₹40 | ₹15 |
| Noon (12 PM–4 PM) | ₹50 | ₹20 |
| Evening (4 PM–8 PM) | ₹60 | ₹25 |
| Night (8 PM–6 AM) | ₹30 | ₹10 |

### Overtime Penalty

If parking exceeds 6 hours, a one-time penalty applies:

- Car: ₹100
- Bike: ₹50

Billable parking hours are rounded up, with a minimum charge of one hour.

## Technologies

- Java
- Java Swing
- MySQL
- JDBC
- NetBeans IDE

## Requirements

- Java Development Kit (JDK)
- NetBeans IDE
- MySQL Server
- MySQL JDBC driver

## Setup

1. Clone or download this repository.
2. Open the project in NetBeans IDE.
3. Create the `smart_parking` database in MySQL.
4. Import the required database schema and configure the database connection.
5. Configure the MySQL password using the `SMART_PARKING_DB_PASSWORD` environment variable.
6. Build and run the project.

## Project Structure

- `src/oop/project/` — Java source files
- `nbproject/` — NetBeans project configuration
- `build.xml` — Ant build configuration

## Purpose

This project demonstrates Java OOP, database connectivity, desktop GUI development, and dynamic parking fee calculation.

Extended with dynamic time-based parking fees and overtime penalties for a hackathon.
