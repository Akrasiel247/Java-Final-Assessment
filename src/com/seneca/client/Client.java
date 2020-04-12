package com.seneca.client;
import com.seneca.accounts.Account;
import com.seneca.accounts.Chequing;
import com.seneca.accounts.GIC;
import com.seneca.business.Bank;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Scanner;

public class Client {

    public static Socket clientSocket;
    public static ObjectInputStream objFromServer;
    public static ObjectOutputStream objToServer;

    public static DataOutputStream dataToServer;
    public static DataInputStream  dataFromServer;

    public static void displayMenu(String bankName){
        StringBuffer title = new StringBuffer("\nWelcome to ").append(bankName).append(" Bank!");

        System.out.println(title);
        System.out.println("1. Open an account.");
        System.out.println("2. Close an account.");
        System.out.println("3. Deposit money.");
        System.out.println("4. Withdraw money.");
        System.out.println("5. Display accounts.");
        System.out.println("6. Display a tax statement.");
        System.out.println("7. Exit.");
    }
    public static int menuChoice(){
        Scanner s = new Scanner(System.in);
        System.out.print("Please enter your choice> ");
        return s.nextInt();
    }
    public static Account openAcc() {
        Scanner in = new Scanner(System.in);
        Account newAccount = null;

        boolean valid_args = false;

        while (!valid_args) {
            System.out.print("Please enter the account type(CHQ/GIC)> ");
            String acc_type = in.nextLine();

            System.out.println("Please enter account information in one line.\n");

            if (acc_type.equals("CHQ") || acc_type.equals("chq")) {
                System.out.println(
                        "Format: Name;Account Number; Starting Balance; Service Charge; Max number of Transactions");
                System.out.println("ex. (John Doe; 1234; 567.89; 0.25; 3)");

                String chq_valuesString = in.nextLine();
                String[] chq_args = chq_valuesString.split(";");

                if (chq_args.length != 5) {
                    System.out.println("Invalid input. Please follow the format shown on screen");
                } else {
                    // this is vulnerable to type mismatch
                    newAccount = new Chequing(chq_args[0], chq_args[1].trim(), Double.parseDouble(chq_args[2].trim()),
                            Double.parseDouble(chq_args[3].trim()), Integer.parseInt(chq_args[4].trim()));
                    valid_args = true;

                }

            } else if (acc_type.equals("GIC") || acc_type.equals("gic")) {
                System.out.println(
                        "Format: Name; Account Number; Starting Balance; Period of Investment in year(s); Interest Rate (15.5% would be 15.5)");
                System.out.println("Example: John M. Doe;A1234;1000.00; 1; 15.5");
                System.out.print(">");
                String gic_valuesString = in.nextLine();
                String[] gic_args = gic_valuesString.split(";");

                if (gic_args.length != 5) {
                    System.out.println("Invalid input. Please follow the format shown on screen.");
                } else {
                    newAccount = new GIC(gic_args[0], gic_args[1].trim(), Double.parseDouble(gic_args[2].trim()),
                            Integer.parseInt(gic_args[3].trim()), (Double.parseDouble(gic_args[4].trim()) / 100.00));
                }
                valid_args = true;
            } else {
                System.out.println("Invalid account type");
            }
        }

        return newAccount;

    }
    /**
     *
     * @param
     * @return account number to be deleted
     */
    public static String closeAcc() {
        Scanner in = new Scanner(System.in);

        System.out.println("Please enter the Account Number: ");
        String delAccNum = in.nextLine();
        StringBuffer confirm = new StringBuffer("Confirm delete of account with the number:").append(delAccNum).append(" (Y/N)");
        System.out.println(confirm);
        String res = in.nextLine();
        boolean valid_res = false;
        while (!valid_res) {

            switch (res) {
                case "Y":
                case "y":
                    valid_res = true;
                    break;

                case "N":
                case "n":
                    valid_res = true;
                    delAccNum="";
                    System.out.println("Delete cancelled");
                    break;
                default:
                    System.out.println("Invalid response, please enter \"Y\" or \"N\" ");

            }

        }
        return delAccNum;
    }

    public static void depositMoney() throws IOException, ClassNotFoundException {

        Scanner in = new Scanner(System.in);
        System.out.println("Please enter your account number: ");
        String account_num = in.nextLine();
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        dataToServer.writeUTF(account_num);

        Account depAcc = (Account) objFromServer.readObject();

        if (depAcc != null) {
            double oldBalance = depAcc.getAccountBalance();
            System.out.println("Please enter the amount you would like to deposit:");
            double depositMoney = in.nextDouble();
            dataToServer.writeDouble(depositMoney);
            System.out.println("Deposit Successful");
            double newBalance = dataFromServer.readDouble();
            System.out.println("Old Balance: "+ nf.format(oldBalance));
            System.out.println("New Balance: " + nf.format(newBalance));

        } else {
            System.out.println("Error account not found.");
        }



    }

    public static void withdrawMoney() throws IOException, ClassNotFoundException {
        Scanner in = new Scanner(System.in);
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        System.out.println("Please enter your account number: ");
        String account_num = in.nextLine();

        dataToServer.writeUTF(account_num);
        boolean valid_acc = dataFromServer.readBoolean();

        if (valid_acc) {
            double oldBalance = dataFromServer.readDouble();
            System.out.println("Please enter the amount you would like to withdraw");
            double withdrawAmount = in.nextDouble();
            dataToServer.writeDouble(withdrawAmount);
            boolean res = dataFromServer.readBoolean();
            double newBalance= dataFromServer.readDouble();
            if (res) {
                System.out.println("Withdraw successful");
                System.out.println("Old Balance: "+ nf.format(oldBalance));
                System.out.println("New Balance: " + nf.format(newBalance));


            } else {
                System.out.println("Withdraw failed");
            }

        } else {
            System.out.println("Error account not found.");
        }

    }

    public static void displayAccounts() throws IOException {
        Scanner in = new Scanner(System.in);
        boolean valid = false;
        String option="";
        while (!option.equals("x")) {
            System.out.println("Please choose one of the following options: ");
            System.out.println("a) display all accounts with the same account name");
            System.out.println("b) display all accounts with the same final balance");
            System.out.println("c) display all accounts opened at the bank");
            System.out.println("d) display a specific account");
            System.out.println("x) Return to main menu");
            System.out.print(">");
            option = in.nextLine();
            dataToServer.writeUTF(option);

            String result;
            switch (option) {
                case "a":
                case "A":
                    System.out.println("Please enter the name to search by: ");
                    String nameSearch = in.nextLine();
                    dataToServer.writeUTF(nameSearch);
                    result = dataFromServer.readUTF();
                    System.out.println(result);
                    break;
                case "b":
                case "B":
                    System.out.println("Please enter the balance to search by: ");
                    //this is causing bug, nextDouble() returns a newline - therefore- don't mix nextLine, nextDouble
                    // https://stackoverflow.com/questions/16040601/why-is-nextline-returning-an-empty-string
                    String balance = in.nextLine();
                    dataToServer.writeUTF(balance);
                    result = dataFromServer.readUTF();
                    System.out.println(result);
                    break;
                case "c":
                case "C":
                    result = dataFromServer.readUTF();
                    System.out.println(result);
                    break;

                case "d":
                case "D":
                    System.out.println("Please enter the account number:");
                    String accNum = in.nextLine();
                    dataToServer.writeUTF(accNum);
                    result = dataFromServer.readUTF();
                    System.out.println(result);
                    break;
                case "x":
                case "X":
                    System.out.println("Operation cancelled, returning to main menu");
                    break;
                default:
                    System.out.println("Invalid response, enter a valid response (a-d) or \"x\" to exit");
                    break;
            }
        }

    }

    public static void displayTax() throws IOException {
        Scanner in = new Scanner(System.in);
        System.out.print("Please input the name of the person? ");
        String name = in.nextLine();
        dataToServer.writeUTF(name);
        String results = dataFromServer.readUTF();
        System.out.println(results);


    }
    public static void main(String[] args) {


        try{

            clientSocket = new Socket( InetAddress.getByName( "localhost" ),
                    5678 );

            System.out.println( "Connected to " +
                    clientSocket.getInetAddress().getHostName());

            dataToServer = new DataOutputStream(
                    clientSocket.getOutputStream() );

            dataFromServer= new DataInputStream(
                    clientSocket.getInputStream() );

            System.out.println("I/O streams connected to the socket");

            String bankName=dataFromServer.readUTF();

            int choice = 0;
            objFromServer = new ObjectInputStream(clientSocket.getInputStream());
            objToServer = new ObjectOutputStream(clientSocket.getOutputStream());

            while (choice != 7) {
                displayMenu(bankName);
                choice = menuChoice();

                switch (choice) {

                    case 1: // Open an account - add GIC

                        System.out.println("You have chosen Open Account");
                        dataToServer.writeInt(choice);
                        Account account = openAcc();
                        String result = "";
                        //send account
                        objToServer.writeObject(account);
                        result=dataFromServer.readUTF();
                        System.out.println(result);
                    break;

                    case 2: // Close an account - Complete
                        System.out.println("You have chosen Close Account");
                        String account_num = closeAcc();
                        dataToServer.writeInt(choice);
                        dataToServer.writeUTF(account_num);
                        result=dataFromServer.readUTF();
                        System.out.println(result);

                        break;
                    case 3:// Deposit money-Complete
                        System.out.println("You have chosen deposit");
                        dataToServer.writeInt(choice);
                        depositMoney();


                        break;
                    case 4:// Withdraw money--Complete
                        System.out.println("You have chosen withdraw");
                        dataToServer.writeInt(choice);
                        withdrawMoney();

                        break;
                    case 5:// Display accounts
                        System.out.println("You have chosen display accoutbt");
                        dataToServer.writeInt(choice);
                        displayAccounts();

                        break;
                    case 6:// Display a tax statement
                        System.out.println("You have chosen tax dtatsment");
                        dataToServer.writeInt(choice);
                        displayTax();

                        break;
                    case 7:// Exit
                        dataToServer.writeInt(choice);

                        break;
                    default:
                        System.out.println("invalid choice");
                        choice = menuChoice();

                }

            }
            System.out.println("Thank you for using our app!");





        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();

        }
    }

}