// AppointmentSystem.java
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

class AppointmentSystem {
    private static final String FILENAME = "appointments.txt";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    // Utilities
    static class Utilities {
        static boolean isValidEmail(String email) {
            if (email == null) return false;
            int at = email.indexOf('@');
            int dot = email.lastIndexOf('.');
            return (at > 0 && dot > at+1 && dot < email.length()-1);
        }

        static boolean isValidDate(String dateStr) {
            try {
                LocalDate.parse(dateStr, DATE_FORMAT);
                return true;
            } catch (DateTimeParseException ex) {
                return false;
            }
        }

        static boolean isWeekday(String dateStr) {
            try {
                LocalDate d = LocalDate.parse(dateStr, DATE_FORMAT);
                DayOfWeek dow = d.getDayOfWeek();
                return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
            } catch (DateTimeParseException ex) {
                return false;
            }
        }

        static boolean isValidTime(String timeStr) {
            try {
                LocalTime t = LocalTime.parse(timeStr, TIME_FORMAT);
                // allowed hours: 09:00 .. 15:59 (i.e. hour >=9 && hour <16)
                int hour = t.getHour();
                int minute = t.getMinute();
                return (hour >= 9 && hour < 16 && minute >= 0 && minute < 60);
            } catch (DateTimeParseException ex) {
                return false;
            }
        }
    }

    // User class
    static class User {
        private String name;
        private String email;
        private String phone;

        User() {}

        User(String name, String email, String phone) {
            this.name = name;
            this.email = email;
            this.phone = phone;
        }

        String getEmail() { return email; }

        void inputDetails(Scanner sc) {
            System.out.print("Enter your name: ");
            name = sc.nextLine().trim();

            System.out.print("Enter your email: ");
            email = sc.nextLine().trim();
            if (!Utilities.isValidEmail(email)) {
                throw new IllegalArgumentException("Invalid email format.");
            }

            System.out.print("Enter your phone number: ");
            phone = sc.nextLine().trim();
        }

        void display() {
            System.out.printf("Name: %s, Email: %s, Phone: %s%n", name, email, phone);
        }
    }

    // Appointment class
    static class Appointment {
        private int appointmentId;
        private String userEmail;
        private String date; // yyyy-MM-dd
        private String time; // HH:mm
        private boolean isBooked;

        Appointment() {
            appointmentId = 0;
            isBooked = false;
        }

        Appointment(int id, String email, String date, String time) {
            this.appointmentId = id;
            this.userEmail = email;
            this.date = date;
            this.time = time;
            this.isBooked = true;
        }

        int getId() { return appointmentId; }
        String getUserEmail() { return userEmail; }
        String getDate() { return date; }
        String getTime() { return time; }
        boolean booked() { return isBooked; }
        void cancel() { isBooked = false; }

        void display() {
            if (isBooked) {
                System.out.printf("Appointment ID: %d, User Email: %s, Date: %s, Time: %s%n",
                        appointmentId, userEmail, date, time);
            }
        }

        String serialize() {
            // CSV: id,email,date,time
            return String.format("%d,%s,%s,%s", appointmentId, userEmail, date, time);
        }

        static Appointment deserialize(String line) {
            if (line == null || line.trim().isEmpty()) return null;
            String[] parts = line.split(",", -1);
            if (parts.length < 4) return null;
            try {
                int id = Integer.parseInt(parts[0].trim());
                String email = parts[1].trim();
                String date = parts[2].trim();
                String time = parts[3].trim();
                return new Appointment(id, email, date, time);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }

    // AppointmentManager
    static class AppointmentManager {
        private final List<User> users = new ArrayList<>();
        private final List<Appointment> appointments = new ArrayList<>();
        private int nextAppointmentId = 1;

        void registerUser(Scanner sc) {
            try {
                User u = new User();
                u.inputDetails(sc);
                users.add(u);
                System.out.println("User registered successfully!");
            } catch (IllegalArgumentException ex) {
                System.err.println(ex.getMessage());
            } catch (Exception ex) {
                System.err.println("An unexpected error occurred while registering user.");
            }
        }

        void scheduleAppointment(Scanner sc) {
            System.out.print("Enter your email: ");
            String email = sc.nextLine().trim();

            Optional<User> user = users.stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(email))
                    .findFirst();

            if (!user.isPresent()) {
                System.out.println("User not found. Please register first.");
                return;
            }

            System.out.print("Enter appointment date (YYYY-MM-DD): ");
            String date = sc.nextLine().trim();
            if (!Utilities.isValidDate(date) || !Utilities.isWeekday(date)) {
                System.out.println("Invalid date. Appointments can only be scheduled on weekdays and in proper format.");
                return;
            }

            System.out.print("Enter appointment time (HH:MM): ");
            String time = sc.nextLine().trim();
            if (!Utilities.isValidTime(time)) {
                System.out.println("Invalid time. Appointments can only be scheduled between 9 AM and 4 PM (up to 15:59).");
                return;
            }

            Appointment appt = new Appointment(nextAppointmentId++, email, date, time);
            appointments.add(appt);
            System.out.println("Appointment scheduled successfully!");
        }

        void viewAppointments() {
            List<Appointment> booked = appointments.stream()
                    .filter(Appointment::booked)
                    .collect(Collectors.toList());
            if (booked.isEmpty()) {
                System.out.println("No appointments found.");
                return;
            }
            for (Appointment a : booked) a.display();
        }

        void cancelAppointment(Scanner sc) {
            System.out.print("Enter appointment ID to cancel: ");
            String line = sc.nextLine().trim();
            int id;
            try {
                id = Integer.parseInt(line);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid ID input.");
                return;
            }

            for (Appointment a : appointments) {
                if (a.getId() == id && a.booked()) {
                    a.cancel();
                    System.out.println("Appointment ID " + id + " has been canceled.");
                    return;
                }
            }
            System.out.println("Appointment ID " + id + " not found or already canceled.");
        }

        void saveAppointments() {
            // Only save booked appointments
            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(FILENAME))) {
                for (Appointment a : appointments) {
                    if (a.booked()) {
                        bw.write(a.serialize());
                        bw.newLine();
                    }
                }
                System.out.println("Appointments saved to " + FILENAME + ".");
            } catch (IOException ex) {
                System.err.println("Error opening file for writing: " + ex.getMessage());
            }
        }

        void loadAppointments() {
            Path p = Paths.get(FILENAME);
            if (!Files.exists(p)) {
                System.out.println("No saved appointments found.");
                return;
            }
            try (BufferedReader br = Files.newBufferedReader(p)) {
                String line;
                int maxId = 0;
                while ((line = br.readLine()) != null) {
                    Appointment a = Appointment.deserialize(line);
                    if (a != null) {
                        appointments.add(a);
                        if (a.getId() > maxId) maxId = a.getId();
                    }
                }
                nextAppointmentId = maxId + 1;
                System.out.println("Appointments loaded from " + FILENAME + ".");
            } catch (IOException ex) {
                System.err.println("Error reading appointments file: " + ex.getMessage());
            }
        }
    }

    // Main
    public static void main(String[] args) {
        AppointmentManager manager = new AppointmentManager();
        manager.loadAppointments();

        Scanner sc = new Scanner(System.in);
        int choice = -1;
        do {
            System.out.println("\n--- Appointment System ---");
            System.out.println("1. Register User");
            System.out.println("2. Schedule Appointment");
            System.out.println("3. View Appointments");
            System.out.println("4. Cancel Appointment");
            System.out.println("5. Save Appointments");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");
            String input = sc.nextLine().trim();
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid choice. Please enter a number.");
                continue;
            }

            switch (choice) {
                case 1:
                    manager.registerUser(sc);
                    break;
                case 2:
                    manager.scheduleAppointment(sc);
                    break;
                case 3:
                    manager.viewAppointments();
                    break;
                case 4:
                    manager.cancelAppointment(sc);
                    break;
                case 5:
                    manager.saveAppointments();
                    break;
                case 0:
                    manager.saveAppointments();
                    System.out.println("Exiting the system.");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 0);

        sc.close();
    }
}
