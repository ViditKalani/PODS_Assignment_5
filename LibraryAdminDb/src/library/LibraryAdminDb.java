package library;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Scanner;

public class LibraryAdminDb {

	private final static String URL = "jdbc:oracle:thin:@dataserv.mscs.mu.edu:1521:orcl";
	private static Connection conn = null;
	private static Statement stmt = null;
	private static ResultSet rst = null;
	private static Statement stmt1 = null;

	public static void main(String args[]) throws Exception, IOException, SQLException {

		String user = null, pwd = null;

		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
		} catch (ClassNotFoundException e) {
			System.out.println("Could not load the driver" + e.getMessage());
		}

		Scanner scanner;
		scanner = new Scanner(System.in);
		scanner.useDelimiter(System.getProperty("line.separator"));

		System.out.println("Please Enter database user name");
		user = scanner.nextLine();

		System.out.println("Please Enter database password");
		// pwd = PasswordField.readPassword("Please Enter database password :");
		pwd = scanner.nextLine();

		// Get a connection
		conn = DriverManager.getConnection(URL, user, pwd);

		Statement stmt1 = conn.createStatement();

		System.out.println(
				"\n \n -----------******* Welcome to the Library Database Management System (Admin) *******-----------");

		boolean done = false;
		do {
			printMenu();
			System.out.print("Type in your option: ");
			System.out.flush();

			scanner.useDelimiter(System.getProperty("line.separator"));

			String ch = scanner.nextLine();
			System.out.println();
			switch (ch.charAt(0)) {
			case '1':
				newBorrower(conn);
				break;
			case '2':
				CheckOut(conn);
				break;
			case '3':
				CheckIn(conn);
				break;
			case '4':
				done = true;
				break;
			default:
				System.out.println(" Not a valid option ");
			} // switch
		} while (!done);

		rst.close();
		stmt.close();
		stmt1.close();
		conn.close();

	}

	private static void printMenu() {
		System.out.println("\n -------------- \n MENU \n -------------- \n" + "\n\n"
				+ " ----------------------------------------------------------------------------------------------------- ");
		System.out.println("\n(1) Enter a new Borrower ");
		System.out.println("\n (2) Check-out a Book for a Borrower ");
		System.out.println("\n (3) Check-in a Book for a Borrower ");
		System.out.println("\n (4) Exit \n");
		System.out.println(
				" ----------------------------------------------------------------------------------------------------- \n");
	}

	private static String CheckDB(String Query, Connection conn) throws SQLException, IOException {
		stmt1 = conn.createStatement();

		try {
			rst = stmt.executeQuery(Query);
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Error with getting Check Query");
		}

		while (rst.next()) {
			String BT = rst.getString(1);

			return rst.getString(1);

		}
		System.out.println("Not valid ID");
		return "0";

	}

	private static void newBorrower(Connection conn) throws SQLException, IOException {
		String sqlString = null, userName = null, address = null, phone = null;

		Scanner scanner2 = new Scanner(System.in);
		scanner2.useDelimiter(System.getProperty("line.separator"));

		System.out.println("Please Enter Borrower Name  (firstname lastname) : ");
		userName = scanner2.nextLine();

		System.out.println("Please Enter Borrower Address : ");
		address = scanner2.nextLine();

		System.out.println("Please Enter Phone number (xxx-xxx-xxxx) : ");
		phone = scanner2.nextLine();

		String query = "Insert Into Borrower Values (borrower_seq.nextval, ?, ?, ?, ?) ";

		PreparedStatement Insertquery = conn.prepareStatement(query);

		Insertquery.setString(1, userName);
		Insertquery.setString(2, address);
		Insertquery.setString(3, phone);
		Insertquery.setString(4, null);

		int insertsum = 0;
		try {
			insertsum = Insertquery.executeUpdate();
		} catch (SQLException e) {
			e.getMessage();
			System.out.println("Please try and insert the data into again.");
		}

		if (insertsum != 1) {
			System.out.println("Error with insert statment in Borrower.");

		}

		stmt = conn.createStatement();

		String getAccountName = "SELECT borrower_seq.CURRVAL FROM dual";

		try {
			rst = stmt.executeQuery(getAccountName);

			while (rst.next()) {
				System.out.println(
						"\n Borrower record inserted successfully. The borrower's card no is :" + rst.getString(1));

			}

		} catch (SQLException e) {
			e.getMessage();
			System.out.println("\n Borrower record failed to insert new record. Please try again later.");
			stmt.close();
			// conn.close();
			rst.close();
		}

		stmt.close();
		rst.close();

		System.out.println("\n Query executed. Connection closed");

	}

	private static void CheckIn(Connection conn) throws SQLException, IOException, ParseException {
		printBorrowers(conn);
		int IsCheckedOut = 100;
		LinkedList<String> param = null;
		do {
			param = getParameters(conn);

			IsCheckedOut = CheckBookStatus(conn, param.get(0), param.get(1), param.get(2));

			if (IsCheckedOut == -1) {
				System.err.println("The user did not check out that book please Enter a valid bookId or CardNo");
			} else if (IsCheckedOut == 1) {
				System.err.println("The user has already turned in that book please choose a different bookId");
			}
		} while (IsCheckedOut != 0);
		System.out.println("Part 1 is done............");

		Date DateIn = getCurrentDate();
		UpdateDateIn(conn, param.get(0), param.get(1), param.get(2), DateIn);

		String dueDateQuery = "Select DueDate,DateIn from Book_Loans where CardNo = " + param.get(2) + " and BookId = "
				+ param.get(0) + "and BranchId = " + param.get(1);

		Statement stmt2 = conn.createStatement();

		try {
			rst = stmt2.executeQuery(dueDateQuery);
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Error with getting Check Query");
		}
		Date DD = null;
		Date DI = null;
		while (rst.next()) {
			DD = rst.getDate(1);
			DI = rst.getDate(2);

		}

		java.util.Date javaDate = new java.util.Date(DD.getTime());
		java.util.Date javaDateIn = new java.util.Date(DI.getTime());

		long diffInMillies = Math.abs(javaDate.getTime() - javaDateIn.getTime());
		int days = (int) ((((diffInMillies / 1000) / 60) / 60) / 24);
		System.out.println(diffInMillies);

		double lateFee = 0;
		if (days < 0) {
			lateFee = .3 * Math.abs(days);
			System.out.println("Successfully checked in the book -" + param.get(3) + "(" + param.get(0) + ") for"
					+ param.get(4) + "(" + param.get(2) + "). The book was turned in after the due date. "
					+ "The Late fee is $" + lateFee);
		} else {
			System.out.println("Successfully checked in the book -" + param.get(3) + "(" + param.get(0) + ") for"
					+ param.get(4) + "(" + param.get(2)
					+ "). The book has been returned by the due date. There is no late charge ");
		}

	}

	private static void CheckOut(Connection conn) throws SQLException, IOException, ParseException {
		Date DueDate = null;
		stmt = conn.createStatement();
		String getBorrowers = "Select Name, CardNo From Borrower";

		try {
			rst = stmt.executeQuery(getBorrowers);

			while (rst.next()) {
				System.out.println(rst.getString(1) + "(" + rst.getString(2) + ")");

			}

		} catch (SQLException e) {
			e.getMessage();
			System.err.println("Error with getting borrowers");
		}

		LinkedList<String> param = getParameters(conn);

		String query = "Insert Into Book_Loans Values( ?, ?, ?, ?, add_months(?,1), ?, ?) ";

		java.util.Date date = (getCurrentDate());

		PreparedStatement Insertquery = conn.prepareStatement(query);

		Insertquery.setString(1, param.get(0));
		Insertquery.setString(2, param.get(1));
		Insertquery.setString(3, param.get(2));
		System.out.println((java.sql.Date) date);
		Insertquery.setDate(4, (java.sql.Date) date);
		Insertquery.setDate(5, (java.sql.Date) date);
		Insertquery.setString(6, null);
		Insertquery.setString(7, null);
		int insertsum = 0;
		try {
			insertsum = Insertquery.executeUpdate();
		} catch (SQLException e) {
			if (e.getMessage().contains("unique constraint")) {
				System.out.println(e.getMessage());
				System.out.println(
						"This person has already checked out this book please make sure you Entered in the data correctly.");
			} else {
				System.out.println(e.getMessage());
				System.out.println("Please make sure that you have Entered all of data that was asked.");
			}

		}

		if (insertsum != 1) {
			System.out.println("Error with insert statment. Please Try again! ");

			return;
		}

		String dueDateQuery = "Select DueDate from Book_Loans where CardNo = " + param.get(2) + " and BookId = "
				+ param.get(0);

		Statement stmt2 = conn.createStatement();

		try {
			rst = stmt2.executeQuery(dueDateQuery);

			Date DD = null;
			while (rst.next()) {
				DD = rst.getDate(1);

			}

			java.util.Date javaDate = new java.util.Date(DD.getTime());
			SimpleDateFormat simpleDateformat = new SimpleDateFormat("MM");
			String Month = simpleDateformat.format(javaDate);

			simpleDateformat = new SimpleDateFormat("dd");
			String Day = simpleDateformat.format(javaDate);

			simpleDateformat = new SimpleDateFormat("YYYY");
			String Year = simpleDateformat.format(javaDate);

			System.out.println("Successfully checked out the book -" + param.get(3) + "(" + param.get(0) + ") for"
					+ param.get(4) + "(" + param.get(2) + "). The book is due by :" + Month + ", " + Day + " " + Year);

			stmt.close();

			rst.close();
		} catch (SQLException e) {
			e.getMessage();
			System.err.println("Error with getting Check Query please try again later");
		}

		stmt.close();

		rst.close();
	}

	private static void UpdateDateIn(Connection conn, String BookId, String BranchId, String CardNo, Date DateIn)
			throws SQLException, IOException, ParseException {

		String query = "Update Book_Loans Set DateIn = ?  Where BookId=? and BranchId = ? and CardNo = ?";

		PreparedStatement Insertquery = conn.prepareStatement(query);
		Insertquery.setDate(1, (java.sql.Date) DateIn);
		Insertquery.setString(2, BookId);
		Insertquery.setString(3, BranchId);
		Insertquery.setString(4, CardNo);

		int insertsum = 0;

		try {
			insertsum = Insertquery.executeUpdate();
			if (insertsum != 1) {
				System.out.println("Error with Update statment");
				conn.close();
			}
		} catch (SQLException e) {
			e.getMessage();
			System.out.println("There was an error with checking in the persons book.");
		}

	}

	private static LinkedList<String> getParameters(Connection conn) throws SQLException, IOException, ParseException {
		LinkedList<String> parameters = new LinkedList<String>();
		String sqlString = null, CardNo = null, BookId = null, BranchId = null, BookTitle = null, BorrowerName = null;

		Scanner scanner2 = new Scanner(System.in);
		scanner2.useDelimiter(System.getProperty("line.separator"));
		boolean Run = true;

		do {
			System.out.println("Please Enter the borrower card no: ");
			CardNo = scanner2.nextLine();

			BorrowerName = CheckDB("Select Name from Borrower where CardNo = " + CardNo, conn);

			if (BorrowerName == "0") {
				System.err.print("Please Enter a valid CardNo.");
			} else {
				Run = false;
			}

		} while (Run);

		Run = true;
		do {
			System.out.println("Please Enter the book id: ");
			BookId = scanner2.nextLine();

			BookTitle = CheckDB("Select Title From Book where BookId = " + BookId, conn);

			if (BookTitle == "0") {
				System.err.print("Please Enter a valid BookId");
			} else {
				Run = false;
			}

		} while (Run);

		Run = true;

		String BranchIdString = null;
		do {
			System.out.println("Please Enter the branch id: ");
			BranchId = scanner2.nextLine();

			BranchIdString = CheckDB("Select BranchId From Library_Branch where BranchId = " + BranchId, conn);

			if (BranchIdString != "0") {
				System.err.print("Please Enter a valid BranchId");
				Run = false;
			}

		} while (Run);

		parameters.add(BookId);
		parameters.add(BranchId);
		parameters.add(CardNo);
		parameters.add(BookTitle);
		parameters.add(BorrowerName);

		return parameters;

	}

	private static int CheckBookStatus(Connection conn, String BookId, String BranchId, String CardNo)
			throws SQLException, IOException, ParseException {
		stmt = conn.createStatement();
		String getBorrower = "Select CardNo From Book_Loans where CardNo = " + CardNo + " and BookId = " + BookId
				+ " and BranchId = " + BranchId;

		boolean checkingIn = false;
		try {
			rst = stmt.executeQuery(getBorrower);
		} catch (SQLException e) {
			e.getMessage();
			System.err.println("Error with getting borrower with Check In");
		}

		while (rst.next()) {
			checkingIn = true;

		}
		if (checkingIn) {
			stmt = conn.createStatement();
			getBorrower = "Select CardNo From Book_Loans where CardNo = " + CardNo + " and BookId = " + BookId
					+ " and BranchId = " + BranchId + " and DateIn IS NULL";

			checkingIn = false;
			try {
				rst = stmt.executeQuery(getBorrower);
			} catch (SQLException e) {
				e.printStackTrace();
				System.err.println("Error with getting borrower with Check In DueDate");
			}

			while (rst.next()) {
				checkingIn = true;

			}

			if (checkingIn) {
				stmt.close();
				return 0;
			} else {
				stmt.close();
				return 1;
			}

		} else {
			stmt.close();
			return -1;
		}

	}

	private static void printBorrowers(Connection conn) throws SQLException, IOException, ParseException {
		stmt = conn.createStatement();
		String getBorrowers = "Select Name, CardNo From Borrower";

		try {
			rst = stmt.executeQuery(getBorrowers);
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Error with getting borrowers");
		}

		while (rst.next()) {
			System.out.println(rst.getString(1) + "(" + rst.getString(2) + ")");

		}

	}

	private static String readEntry(String prompt) {
		try {
			StringBuffer buffer = new StringBuffer();
			System.out.print(prompt);
			System.out.flush();
			int c = System.in.read();
			while (c != '\n' && c != -1) {
				buffer.append((char) c);
				c = System.in.read();
			}
			return buffer.toString().trim();
		} catch (IOException e) {
			return "";
		}
	}

	private static java.sql.Date getCurrentDate() {
		java.util.Date today = new java.util.Date();
		return new java.sql.Date(today.getTime());
	}

}
