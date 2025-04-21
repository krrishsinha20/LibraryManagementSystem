# LibraryManagementSystem
## Team Members
- Krish Sinha – OOPS Implementation
- Miraat Gupta – Database
- Piyush Mengde – Exception handling, UI

📌 Add New Books

👥 Register Users (Students)

🔖 Issue Books to Users (with availability check)

✅ Return Books and update availability

📋 View all Books and Users

🛡️ Exception Handling for:

Book not available

User not found

Book not found

💾 Auto Database Table Creation on startup

💡 Technologies Used
Java (JDK 8+)

JDBC (Java Database Connectivity)

PostgreSQL Database

SQL Queries

Custom Exceptions & Transaction Handling

├── DatabaseConnector.java         # PostgreSQL connection handler
├── BookNotAvailableException.java # Custom Exception: Book not available
├── UserNotFoundException.java     # Custom Exception: User not found
├── BookNotFoundException.java     # Custom Exception: Book not found
├── User.java                      # Abstract base class for Users
├── Student.java                   # Concrete subclass for Student Users
├── Book.java                      # Book class definition
├── Library.java                   # Core logic for Library operations
└── Main.java                      # Main method to run the program (you write this!)
