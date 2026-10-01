# IT Asset Management Application

An Android-based IT Asset Management application developed using Kotlin and Firebase to improve the registration, allocation, transfer, maintenance, monitoring, and reporting of organisational IT assets.

## 📱 About the Project

The IT Asset Management Application provides a centralised digital solution for managing IT assets throughout their lifecycle.

The application was developed to address challenges associated with manual IT asset management processes, including the use of spreadsheets, documents, and asset registers.

The system aims to improve:

- Asset accountability
- Asset tracking
- Allocation management
- Transfer management
- Maintenance tracking
- Data accuracy
- Reporting
- Operational efficiency

## 🎯 Objectives

The main objectives of the application are to:

- Digitise IT asset registration and management.
- Track assets throughout their lifecycle.
- Manage asset allocation to users and departments.
- Record asset transfers.
- Manage maintenance activities.
- Provide centralised asset information.
- Generate asset reports.
- Improve accountability and visibility of IT assets.
- Provide role-based access to system functionality.

## ✨ Key Features

### 🔐 User Authentication
- User registration and login
- Firebase Authentication
- User profiles
- Role-based access

### 💻 Asset Management
- Add and register IT assets
- Asset number and serial number tracking
- Asset type and condition
- Location and department information
- Assigned user management
- Purchase date tracking

### 👤 Asset Allocation
- Allocate assets to users
- Record allocation information
- Allocation date
- User and department information
- Digital signature capture

### 🔄 Asset Transfer
- Transfer assets between users or departments
- Record previous and new allocation information
- Track transfer history
- Digital signatures

### 🔧 Maintenance
- Record maintenance activities
- Track asset maintenance information
- Monitor maintenance history
- Generate maintenance reports

### 📊 Reports
- Asset reports
- Allocation reports
- Transfer reports
- Maintenance reports
- Exportable reports

### 📈 Dashboard
- Total asset count
- Reports
- Transfers
- Allocations
- Quick access to major system functions
- Firebase-powered real-time information

## 🛠️ Technologies Used

| Technology | Purpose |
|---|---|
| Kotlin | Application development |
| Android Studio | Development environment |
| Android SDK | Android application platform |
| Firebase Authentication | User authentication |
| Firebase Realtime Database | Real-time asset data |
| Firebase Firestore | Application data |
| Firebase App Distribution | Application testing and distribution |
| Git | Version control |
| GitHub | Source code management |

## 🏗️ Application Architecture

The application follows an Android-based architecture where the mobile application communicates with Firebase services.

```text
                    IT Asset Management App
                              |
          +-------------------+-------------------+
          |                   |                   |
          ↓                   ↓                   ↓
     Authentication      Asset Management     Reports
          |                   |                   |
          ↓                   ↓                   ↓
   Firebase Auth       Realtime Database      Firestore
          |                   |
          +-------------------+
                    |
                    ↓
             Firebase Services
