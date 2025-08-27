package boundary;

import java.util.Scanner;
import utility.InputUtil;

public class ClinicMaintenanceUI {
    private Scanner input = new Scanner(System.in);

    public void printHeader(String headerMsg) {
        System.out.println("\n-----------------------------------------------");
        System.out.println(headerMsg);
        System.out.println("-----------------------------------------------");
    }

    public int getMenuChoice() {
        System.out.println("Please select an option:");
        System.out.println("1. Patient Management");
        System.out.println("2. Doctor Management");
    System.out.println("3. Consultation Management");
    System.out.println("4. Medical Treatment Management");
    System.out.println("5. Pharmacy Management");
    System.out.println("6. Exit");
        System.out.println("-----------------------------------------------");
        return InputUtil.getIntInput(input, "Enter your choice: ");
    }

}
