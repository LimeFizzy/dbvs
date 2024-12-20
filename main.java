import java.sql.*;
import java.util.Scanner;

public class OrderManagement {

    /********************************************************/
    public static void loadDriver() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException cnfe) {
            System.out.println("Couldn't find driver class!");
            cnfe.printStackTrace();
            System.exit(1);
        }
    }
    /********************************************************/
    public static Connection getConnection() {
        Connection postGresConn = null;
        try {
            postGresConn = DriverManager.getConnection("jdbc:postgresql://pgsql3.mif/studentu", "stud", "stud");
        } catch (SQLException sqle) {
            System.out.println("Couldn't connect to database!");
            sqle.printStackTrace();
            return null;
        }
        System.out.println("Successfully connected to Postgres Database");
        return postGresConn;
    }
    /********************************************************/
    public static void searchCustomerOrders(Connection conn) {
        String query = """
                SELECT order_id, total_price, status, order_date
                FROM lesi9952.customer_orders
                WHERE customer_name = (SELECT name FROM lesi9952.customers WHERE email = ?)
                """;
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.print("Enter customer email: ");
            String email = scanner.nextLine();

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();

                System.out.println("\nCustomer Orders:");
                while (rs.next()) {
                    System.out.printf("Order ID: %d | Total Price: %.2f | Status: %s | Date: %s%n",
                            rs.getInt("order_id"),
                            rs.getBigDecimal("total_price"),
                            rs.getString("status"),
                            rs.getTimestamp("order_date"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving customer orders!");
            e.printStackTrace();
        }
    }
    /********************************************************/
    public static void addOrder(Connection conn) {
        String findCustomerIdQuery = "SELECT customer_id FROM lesi9952.customers WHERE email = ?";
        String insertOrderQuery = """
                INSERT INTO lesi9952.orders (customer_id, total_price, status)
                VALUES (?, ?, 'Pending') RETURNING order_id
                """;
        String insertOrderProductQuery = """
                INSERT INTO lesi9952.order_products (order_id, product_id, quantity)
                VALUES (?, ?, ?)
                """;
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.print("Enter customer email: ");
            String email = scanner.nextLine();

            int customerId;
            try (PreparedStatement stmt = conn.prepareStatement(findCustomerIdQuery)) {
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    customerId = rs.getInt("customer_id");
                } else {
                    System.out.println("Customer not found!");
                    return;
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(insertOrderQuery)) {
                stmt.setInt(1, customerId);
                stmt.setBigDecimal(2, BigDecimal.ZERO); // Placeholder; updated after adding products
                ResultSet rs = stmt.executeQuery();
                rs.next();
                int orderId = rs.getInt("order_id");

                System.out.println("Available Products:");
                try (Statement productStmt = conn.createStatement();
                     ResultSet productRs = productStmt.executeQuery("SELECT product_id, name, price FROM lesi9952.products")) {
                    while (productRs.next()) {
                        System.out.printf("Product ID: %d | Name: %s | Price: %.2f%n",
                                productRs.getInt("product_id"),
                                productRs.getString("name"),
                                productRs.getBigDecimal("price"));
                    }
                }

                BigDecimal totalPrice = BigDecimal.ZERO;
                while (true) {
                    System.out.print("Enter product ID to add (or 0 to finish): ");
                    int productId = scanner.nextInt();
                    if (productId == 0) break;

                    System.out.print("Enter quantity: ");
                    int quantity = scanner.nextInt();

                    try (PreparedStatement productStmt = conn.prepareStatement(insertOrderProductQuery)) {
                        productStmt.setInt(1, orderId);
                        productStmt.setInt(2, productId);
                        productStmt.setInt(3, quantity);
                        productStmt.executeUpdate();
                    }

                    try (PreparedStatement priceStmt = conn.prepareStatement("SELECT price FROM lesi9952.products WHERE product_id = ?")) {
                        priceStmt.setInt(1, productId);
                        ResultSet priceRs = priceStmt.executeQuery();
                        if (priceRs.next()) {
                            totalPrice = totalPrice.add(priceRs.getBigDecimal("price").multiply(BigDecimal.valueOf(quantity)));
                        }
                    }
                }

                try (PreparedStatement updateTotalPriceStmt = conn.prepareStatement("UPDATE lesi9952.orders SET total_price = ? WHERE order_id = ?")) {
                    updateTotalPriceStmt.setBigDecimal(1, totalPrice);
                    updateTotalPriceStmt.setInt(2, orderId);
                    updateTotalPriceStmt.executeUpdate();
                }

                System.out.println("Order created successfully.");
            }
        } catch (SQLException e) {
            System.out.println("Error adding a new order!");
            e.printStackTrace();
        }
    }
    /********************************************************/
    public static void deleteProductFromOrder(Connection conn) {
        String getCustomerOrdersQuery = """
                SELECT o.order_id, o.total_price, o.status, o.order_date
                FROM lesi9952.orders o
                JOIN lesi9952.customers c ON o.customer_id = c.customer_id
                WHERE c.email = ?
                """;
        String getOrderProductsQuery = """
                SELECT op.product_id, p.name, op.quantity
                FROM lesi9952.order_products op
                JOIN lesi9952.products p ON op.product_id = p.product_id
                WHERE op.order_id = ?
                """;
        String deleteProductQuery = "DELETE FROM lesi9952.order_products WHERE order_id = ? AND product_id = ?";
        Scanner scanner = new Scanner(System.in);
    
        try {
            System.out.print("Enter customer email: ");
            String email = scanner.nextLine();
    
            try (PreparedStatement stmt = conn.prepareStatement(getCustomerOrdersQuery)) {
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                
                System.out.println("\nCustomer Orders:");
                boolean ordersExist = false;
                while (rs.next()) {
                    ordersExist = true;
                    System.out.printf("Order ID: %d | Total Price: %.2f | Status: %s | Date: %s%n",
                            rs.getInt("order_id"),
                            rs.getBigDecimal("total_price"),
                            rs.getString("status"),
                            rs.getTimestamp("order_date"));
                }
    
                if (!ordersExist) {
                    System.out.println("No orders found for this email.");
                    return;
                }
    
                System.out.print("\nEnter order ID to delete product from: ");
                int orderId = scanner.nextInt();
    
                System.out.println("\nOrder Products:");
                try (PreparedStatement productStmt = conn.prepareStatement(getOrderProductsQuery)) {
                    productStmt.setInt(1, orderId);
                    ResultSet productRs = productStmt.executeQuery();
                    boolean productsExist = false;
                    while (productRs.next()) {
                        productsExist = true;
                        System.out.printf("Product ID: %d | Name: %s | Quantity: %d%n",
                                productRs.getInt("product_id"),
                                productRs.getString("name"),
                                productRs.getInt("quantity"));
                    }
    
                    if (!productsExist) {
                        System.out.println("No products found in this order.");
                        return;
                    }
    
                    System.out.print("\nEnter product ID to delete: ");
                    int productId = scanner.nextInt();
    
                    try (PreparedStatement stmtDelete = conn.prepareStatement(deleteProductQuery)) {
                        stmtDelete.setInt(1, orderId);
                        stmtDelete.setInt(2, productId);
                        int rowsDeleted = stmtDelete.executeUpdate();
                        if (rowsDeleted > 0) {
                            System.out.println("Product removed from the order.");
                        } else {
                            System.out.println("Failed to remove the product. Check the order and product IDs.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error deleting product from the order!");
            e.printStackTrace();
        }
    }
    /********************************************************/
    public static void updateOrderStatus(Connection conn) {
        String getOrderQuery = """
                SELECT o.order_id, o.status
                FROM lesi9952.orders o
                JOIN lesi9952.customers c ON o.customer_id = c.customer_id
                WHERE c.email = ?
                """;
        String updateStatusQuery = "UPDATE lesi9952.orders SET status = ? WHERE order_id = ?";
        String updateProductQuantityQuery = """
                UPDATE lesi9952.products
                SET quantity = quantity - (
                    SELECT op.quantity
                    FROM lesi9952.order_products op
                    WHERE op.product_id = lesi9952.products.product_id
                    AND op.order_id = ?
                )
                WHERE product_id IN (
                    SELECT product_id
                    FROM lesi9952.order_products
                    WHERE order_id = ?
                )
                """;
    
        Scanner scanner = new Scanner(System.in);
    
        try {
            System.out.print("Enter customer email: ");
            String email = scanner.nextLine();
    
            System.out.println("\nCustomer Orders:");
            try (PreparedStatement stmt = conn.prepareStatement(getOrderQuery)) {
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                boolean ordersExist = false;
                while (rs.next()) {
                    ordersExist = true;
                    System.out.printf("Order ID: %d | Current Status: %s%n",
                            rs.getInt("order_id"),
                            rs.getString("status"));
                }
    
                if (!ordersExist) {
                    System.out.println("No orders found for this email.");
                    return;
                }
            }
    
            System.out.print("\nEnter the order ID to update: ");
            int orderId = scanner.nextInt();
    
            System.out.println("\nAvailable Status Options:");
            System.out.println("1. Pending");
            System.out.println("2. Completed");
            System.out.println("3. Cancelled");
            System.out.print("Select the new status: ");
            int statusChoice = scanner.nextInt();
    
            String newStatus;
            switch (statusChoice) {
                case 1 -> newStatus = "Pending";
                case 2 -> newStatus = "Completed";
                case 3 -> newStatus = "Cancelled";
                default -> {
                    System.out.println("Invalid status choice. Aborting...");
                    return;
                }
            }
    
            conn.setAutoCommit(false);
            try (PreparedStatement updateStatusStmt = conn.prepareStatement(updateStatusQuery);
                 PreparedStatement updateProductQuantityStmt = conn.prepareStatement(updateProductQuantityQuery)) {
    
                // Update order status
                updateStatusStmt.setString(1, newStatus);
                updateStatusStmt.setInt(2, orderId);
                int rowsUpdated = updateStatusStmt.executeUpdate();
    
                if (rowsUpdated > 0) {
                    System.out.println("Order status updated successfully.");
                } else {
                    System.out.println("Failed to update the order status. Rolling back...");
                    conn.rollback();
                    return;
                }
    
                // If status is "Completed", update product quantities
                if ("Completed".equalsIgnoreCase(newStatus)) {
                    updateProductQuantityStmt.setInt(1, orderId);
                    updateProductQuantityStmt.setInt(2, orderId);
    
                    int rowsAffected = updateProductQuantityStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        System.out.println("Product quantities updated successfully.");
                    } else {
                        System.out.println("Failed to update product quantities. Rolling back...");
                        conn.rollback();
                        return;
                    }
                }
    
                conn.commit();
                System.out.println("Transaction committed successfully.");
            } catch (SQLException e) {
                System.out.println("Error occurred during the transaction. Rolling back...");
                conn.rollback();
                e.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println("Error updating the order status!");
            e.printStackTrace();
        }
    }
    /********************************************************/
    public static void main(String[] args) {
        loadDriver();
        try (Connection conn = getConnection();
             Scanner scanner = new Scanner(System.in)) {

            if (conn == null) return;

            while (true) {
                System.out.println("\n=== Order Management System ===");
                System.out.println("1. Search Customer Orders");
                System.out.println("2. Add New Order");
                System.out.println("3. Delete Product from Order");
                System.out.println("4. Update Order Status");
                System.out.println("5. Exit");
                System.out.print("Select an option: ");
                int choice = scanner.nextInt();

                switch (choice) {
                    case 1 -> searchCustomerOrders(conn);
                    case 2 -> addOrder(conn);
                    case 3 -> deleteProductFromOrder(conn);
                    case 4 -> updateOrderStatus(conn);
                    case 5 -> {
                        System.out.println("Exiting. Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid choice. Try again.");
                }
            }
        } catch (Exception e) {
            System.out.println("Unexpected error occurred.");
            e.printStackTrace();
        }
    }
    /********************************************************/
}
