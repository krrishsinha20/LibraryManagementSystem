import java.sql.*;
import java.util.Scanner;
import java.util.InputMismatchException;
import org.postgresql.util.PSQLException;

class DatabaseConnector {
private static final String URL = "jdbc:postgresql://localhost:5432/Library Management System";
private static final String USER = "postgres";
private static final String PASS = "merapsql";
public static Connection getConnection() throws SQLException {

try {

return DriverManager.getConnection(URL, USER, PASS);
        
} 
catch (SQLException e) {

System.err.println("Failed to connect to database: " + e.getMessage());

throw e;

}

}

}


class BookNotAvailableException extends Exception {
    
public BookNotAvailableException(String message) {
        
super(message);

}

}

class UserNotFoundException extends Exception {

public UserNotFoundException(String message) {

super(message);

}

}

class BookNotFoundException extends Exception {

public BookNotFoundException(String message) {

super(message);
    
}

}

abstract class User {

protected int userId;
protected String name;
protected String userType;

public User(String name, String userType) {

this.name = name;
this.userType = userType;

}

public abstract void displayUserInfo();
    
public int getUserId() {
    
return userId;

}
    
public void setUserId(int userId) {

this.userId = userId;

}

}

class Student extends User {

public Student(String name) {

super(name, "Student");

}
@Override

public void displayUserInfo() {
        
System.out.println("Student ID: " + userId + ", Name: " + name);
    
}

}

class Book {

private int bookId;
private String title;
private String author;
private boolean isAvailable;

public Book(String title, String author) {

this.title = title;
this.author = author;
this.isAvailable = true;
    
}

public int getBookId() {
return bookId;
}

public void setBookId(int bookId) {
this.bookId = bookId;
}

public String getTitle() {
return title;
}

public String getAuthor() {
return author;
}

public boolean isAvailable() {
return isAvailable;
}

public void setAvailable(boolean status) {
this.isAvailable = status;
}

}

class Library {

private Connection conn;

public Library() throws SQLException {
    
this.conn = DatabaseConnector.getConnection();

initializeDatabase();

}

private void initializeDatabase() throws SQLException {

String createUsersTable = "CREATE TABLE IF NOT EXISTS Users (" +
                "user_id SERIAL PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "user_type VARCHAR(50) NOT NULL)";
        
 String createBooksTable = "CREATE TABLE IF NOT EXISTS Books (" +
                "book_id SERIAL PRIMARY KEY, " +
                "title VARCHAR(200) NOT NULL, " +
                "author VARCHAR(100) NOT NULL, " +
                "is_available BOOLEAN DEFAULT TRUE)";
        
String createIssuedBooksTable = "CREATE TABLE IF NOT EXISTS IssuedBooks (" +
                "issue_id SERIAL PRIMARY KEY, " +
                "user_id INTEGER REFERENCES Users(user_id) ON DELETE CASCADE, " +
                "book_id INTEGER REFERENCES Books(book_id) ON DELETE CASCADE, " +
                "issue_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "return_date TIMESTAMP)";
        
try (Statement stmt = conn.createStatement()) {
stmt.execute(createUsersTable);
stmt.execute(createBooksTable);
stmt.execute(createIssuedBooksTable);

}
}

public void addBook(Book book) throws SQLException {

String sql = "INSERT INTO Books (title, author, is_available) VALUES (?, ?, ?) RETURNING book_id";

try (PreparedStatement stmt = conn.prepareStatement(sql)) {
    
            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setBoolean(3, book.isAvailable());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                book.setBookId(rs.getInt(1));
}
            
            System.out.println("Book Added: " + book.getTitle() + " (ID: " + book.getBookId() + ")");
        } catch (SQLException e) {
            System.err.println("Error adding book: " + e.getMessage());
            throw e;
        }
    }

    public User registerUser(User user) throws SQLException {
        String sql = "INSERT INTO Users (name, user_type) VALUES (?, ?) RETURNING user_id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.name);
            stmt.setString(2, user.userType);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                user.setUserId(rs.getInt(1));
            }
            
            System.out.println("User Registered: " + user.name + " (ID: " + user.getUserId() + ")");
            return user;
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
            throw e;
        }
    }

    public void issueBook(int userId, int bookId) throws SQLException, BookNotAvailableException, UserNotFoundException, BookNotFoundException {
        try {
            conn.setAutoCommit(false);

            
            try (PreparedStatement userCheck = conn.prepareStatement(
                    "SELECT 1 FROM Users WHERE user_id = ? FOR SHARE")) {
                userCheck.setInt(1, userId);
                ResultSet rs = userCheck.executeQuery();
                if (!rs.next()) {
                    throw new UserNotFoundException("User with ID " + userId + " not found");
                }
            }

            
            try (PreparedStatement bookCheck = conn.prepareStatement(
                    "SELECT is_available FROM Books WHERE book_id = ? FOR UPDATE")) {
                bookCheck.setInt(1, bookId);
                ResultSet rs = bookCheck.executeQuery();
                
                if (!rs.next()) {
                    throw new BookNotFoundException("Book with ID " + bookId + " not found");
                }
                if (!rs.getBoolean("is_available")) {
                    throw new BookNotAvailableException("Book is not available for issue!");
                }
            }

            
            try (PreparedStatement issueStmt = conn.prepareStatement(
                    "INSERT INTO IssuedBooks (user_id, book_id) VALUES (?, ?)")) {
                issueStmt.setInt(1, userId);
                issueStmt.setInt(2, bookId);
                issueStmt.executeUpdate();
            }

           
            try (PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE Books SET is_available = false WHERE book_id = ?")) {
                updateStmt.setInt(1, bookId);
                updateStmt.executeUpdate();
            }

            conn.commit();
            System.out.println("Book issued successfully!");

        } catch (PSQLException e) {
            handlePSQLException(e, userId, bookId);
            conn.rollback();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private void handlePSQLException(PSQLException e, int userId, int bookId) 
        throws UserNotFoundException, BookNotFoundException {
        if (e.getMessage().contains("user_id_fkey")) {
            throw new UserNotFoundException("User with ID " + userId + " not found");
        } else if (e.getMessage().contains("book_id_fkey")) {
            throw new BookNotFoundException("Book with ID " + bookId + " not found");
        }
    }

    public void returnBook(int bookId) throws SQLException, BookNotFoundException {
        try {
            conn.setAutoCommit(false);

        
            try (PreparedStatement bookCheck = conn.prepareStatement(
                    "SELECT 1 FROM Books WHERE book_id = ? FOR UPDATE")) {
                bookCheck.setInt(1, bookId);
                ResultSet rs = bookCheck.executeQuery();
                if (!rs.next()) {
                    throw new BookNotFoundException("Book with ID " + bookId + " not found");
                }
            }

           
            try (PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE Books SET is_available = true WHERE book_id = ?")) {
                updateStmt.setInt(1, bookId);
                int affected = updateStmt.executeUpdate();
                if (affected == 0) {
                    throw new BookNotFoundException("Book with ID " + bookId + " not found");
                }
            }

            
            try (PreparedStatement returnStmt = conn.prepareStatement(
                    "UPDATE IssuedBooks SET return_date = NOW() WHERE book_id = ? AND return_date IS NULL")) {
                returnStmt.setInt(1, bookId);
                returnStmt.executeUpdate();
            }

            conn.commit();
            System.out.println("Book returned successfully!");

        } catch (PSQLException e) {
            conn.rollback();
            throw new SQLException("Database error: " + e.getMessage());
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public void displayAllBooks() throws SQLException {
        String sql = "SELECT book_id, title, author, is_available FROM Books ORDER BY title";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n=== Book List ===");
            System.out.printf("%-10s %-40s %-30s %-10s%n", "Book ID", "Title", "Author", "Available");
            while (rs.next()) {
                System.out.printf("%-10d %-40s %-30s %-10s%n",
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getBoolean("is_available") ? "Yes" : "No");
            }
        }
    }

    public void displayAllUsers() throws SQLException {
        String sql = "SELECT user_id, name, user_type FROM Users ORDER BY name";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n=== User List ===");
            System.out.printf("%-10s %-30s %-15s%n", "User ID", "Name", "User Type");
            while (rs.next()) {
                System.out.printf("%-10d %-30s %-15s%n",
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("user_type"));
            }
        }
    }

    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}

public class LibraryManagementSystem {
    public static void main(String[] args) {
        Library library = null;
        try {
            library = new Library();
            Scanner sc = new Scanner(System.in);

            System.out.println("=== Welcome to Library Management System ===");
            while (true) {
                try {
                    System.out.println("1.Register User");
                    System.out.println("2.Add Book");
                    System.out.println("3.Issue Book");
                    System.out.println("4.Return Book");
                    System.out.println ("5.list Books");
                    System.out.println("6.list Users");
                    System.out.println("7.Exit");

                    System.out.println("Enter choice: ");
                    
                    int choice = getValidInt(sc);
                    sc.nextLine();  

                    switch (choice) {
                        case 1:
                            handleRegisterUser(sc, library);
                            break;
                        case 2:
                            handleAddBook(sc, library);
                            break;
                        case 3:
                            handleIssueBook(sc, library);
                            break;
                        case 4:
                            handleReturnBook(sc, library);
                            break;
                        case 5:
                            library.displayAllBooks();
                            break;
                        case 6:
                            library.displayAllUsers();
                            break;
                        case 7:
                            System.out.println("Exiting...");
                            return;
                        default:
                            System.out.println("Invalid choice!");
                    }
                } catch (SQLException e) {
                    System.err.println("Database Error: " + e.getMessage());
                } catch (BookNotAvailableException | UserNotFoundException | BookNotFoundException e) {
                    System.err.println("Operation Failed: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("System initialization failed: " + e.getMessage());
        } finally {
            if (library != null) library.close();
        }
    }

    private static int getValidInt(Scanner sc) {
        while (true) {
            try {
                return sc.nextInt();
            } catch (InputMismatchException e) {
                System.out.print("Invalid input. Enter a number: ");
                sc.nextLine();  
            }
        }
    }

    private static void handleRegisterUser(Scanner sc, Library lib) throws SQLException {
        System.out.print("Enter user name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty!");
            return;
        }
        lib.registerUser(new Student(name));
    }

    private static void handleAddBook(Scanner sc, Library lib) throws SQLException {
        System.out.print("Enter book title: ");
        String title = sc.nextLine().trim();
        if (title.isEmpty()) {
            System.out.println("Title cannot be empty!");
            return;
        }
        
        System.out.print("Enter author: ");
        String author = sc.nextLine().trim();
        if (author.isEmpty()) {
            System.out.println("Author cannot be empty!");
            return;
        }
        
        lib.addBook(new Book(title, author));
    }

    private static void handleIssueBook(Scanner sc, Library lib) throws SQLException, BookNotAvailableException, UserNotFoundException, BookNotFoundException {
        System.out.print("Enter user ID: ");
        int userId = getValidInt(sc);
        
        System.out.print("Enter book ID: ");
        int bookId = getValidInt(sc);
        
        lib.issueBook(userId, bookId);
    }

    private static void handleReturnBook(Scanner sc, Library lib) throws SQLException, BookNotFoundException {
        System.out.print("Enter book ID to return: ");
        int bookId = getValidInt(sc);
        
        lib.returnBook(bookId);
    }
}
