package com.seneca.server;
import com.seneca.accounts.Account;
import com.seneca.accounts.GIC;
import com.seneca.accounts.Taxable;
import com.seneca.business.Bank;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class HandleClient extends Thread {

    private Socket m_connection;
    private int m_id;
    private DataOutputStream dataToClient;
    private DataInputStream dataFromClient;
    private Bank m_bank;
    private boolean keepRunning;


    public int getClientID(){return m_id;}

    public HandleClient(int id, Socket s, Bank bank) throws IOException {
        m_connection= s;
        m_id=id;
        dataToClient = new DataOutputStream(m_connection.getOutputStream() );

        dataFromClient = new DataInputStream(m_connection.getInputStream() );
        m_bank=bank;
        keepRunning =true;
    }

    private void displayAccounts() throws IOException {
        String option = "";
        while (!option.equals("x")){
            System.out.println("Waiting for client response");
            option = dataFromClient.readUTF();

            switch (option){
                //Search by account name
                case "a":
                case "A":
                    System.out.println("Please enter the name to search by: ");
                    String name = dataFromClient.readUTF();
                    System.out.println("SEarch for account with name: "+ name);
                    dataToClient.writeUTF(Arrays.toString(m_bank.searchByAccountName(name)));
                    break;
                 //search by final balance
                case "b":
                case "B":
                    System.out.println("Please enter the balance to search by: ");
                    String balanceS = dataFromClient.readUTF();
                    double balance = Double.parseDouble(balanceS);
                    System.out.println(balance);
                    dataToClient.writeUTF(Arrays.toString(m_bank.searchByBalance(balance)));
                    break;
                 //get all accounts in bank
                case "c":
                case "C":
                   dataToClient.writeUTF(Arrays.toString(m_bank.getAllAccounts()));
                    break;

                //search for a specific accoint
                case "d":
                case "D":
                    System.out.println("Please enter the account number:");
                    String acc_num = dataFromClient.readUTF();
                    Account a= m_bank.searchByAccountNumber(acc_num);
                    String result= a!=null? a.toString():"Account not found";
                    dataToClient.writeUTF(result);
                    break;

                 //return to the main menu
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

    private void openAcc(ObjectInputStream objFromClient) throws IOException, ClassNotFoundException {
        System.out.println("Open an account");
        //read account
        Account a = (Account) objFromClient.readObject();
        String result = "unknown";
        if (m_bank.addAccount(a)) {
            result="Account successfully included";
        } else {
            result= "Unable to add Account";
        }
        System.out.println(result);
        dataToClient.writeUTF(result);
        System.out.println(m_bank);

    }

    private void closeAcc() throws IOException {
        String result="";
        System.out.println("Close an account");
        //receive account num
        String acc_num = dataFromClient.readUTF();
        Account del = m_bank.removeAccount(acc_num);
        if(del != null){
            result =  "Account successfully deleted";
        }else{
            result="Account not found";
        }
        System.out.println(result);
        dataToClient.writeUTF(result);
        System.out.println(m_bank);
    }

    private void depositMoney(ObjectOutputStream objToClient) throws IOException{
        System.out.println("Deposit money");
        String acc_num="";
        //get accounnt num from client
        acc_num = dataFromClient.readUTF();
        Account depositAcc = m_bank.searchByAccountNumber(acc_num);
        objToClient.writeObject(depositAcc);

        if(depositAcc!=null){
            double depositAmount=0;
            depositAmount = dataFromClient.readDouble();
            System.out.println(depositAmount);
            depositAcc.deposit(depositAmount);
            dataToClient.writeDouble(depositAcc.getAccountBalance());
            System.out.println("Final Balance: "+ depositAcc.getAccountBalance());

        }
    }

    private void clientMenuHandle() throws IOException, ClassNotFoundException {
        ObjectOutputStream objToClient = new ObjectOutputStream(m_connection.getOutputStream());
        ObjectInputStream objFromClient = new ObjectInputStream(m_connection.getInputStream());
        int choice = 0;

        while (choice != 7){
            System.out.println("getting client choice");
            String result="";
            choice = dataFromClient.readInt();
            switch (choice) {
                case 1: //Open an account
                    openAcc(objFromClient);
                    break;

                case 2: //Close an account
                    closeAcc();
                    break;

                case 3: //Deposit Money

                    depositMoney(objToClient);


                    break;
                case 4:
                    System.out.println("Withdraw money");
                    //get account num from cliet
                    acc_num = dataFromClient.readUTF();
                    Account withdraw_Acc = m_bank.searchByAccountNumber(acc_num);

                    if(withdraw_Acc!=null){
                        System.out.println("Initial amount"+withdraw_Acc.getAccountBalance());

                        dataToClient.writeBoolean(true);
                        dataToClient.writeDouble(withdraw_Acc.getAccountBalance());
                        double draw_amount = dataFromClient.readDouble();
                        boolean res = withdraw_Acc.withdraw(draw_amount);
                        dataToClient.writeBoolean(res);
                        System.out.println("Final Amount"+withdraw_Acc.getAccountBalance());

                        dataToClient.writeDouble(withdraw_Acc.getAccountBalance());

                    }else{
                        dataToClient.writeBoolean(false);
                    }

                    break;
                case 5:
                    System.out.println("display Account");
                    displayAccounts();
                    break;
                case 6:
                    System.out.println("displayTax");
                    String name = dataFromClient.readUTF();
                    StringBuilder results= new StringBuilder();

                    int count = 1;

                    for(Account acc: m_bank.getAllAccounts()){
                        if(acc instanceof GIC && acc.getFullName().equals(name)){
                            if(count == 1){
                                results.append("Tax rate: " + (Taxable.tax_rate * 100) + "%\n");
                                results.append("Name: ").append(acc.getLastName()).append(", ").append(acc.getFirstName()).append("\n");
                            }
                            results.append("[").append(count++).append("]");
                            results.append(((GIC) acc).getTax());
                        }
                    }

                    dataToClient.writeUTF(String.valueOf(results));



                    break;
                case 7:
                    System.out.println("Exit");
                    StopRunning();
                    break;
                default:
                    System.out.println("Invalid choice");
                    //get input from client again
                    choice = 0;
            }

        }
    }

    private void StopRunning(){
        keepRunning = false;
    }
    @Override
    public void run() {
        while (keepRunning){
            try{

                System.out.println(m_bank.getBankName() + "\n for client: "+ m_id);
                //Send BankName
                dataToClient.writeUTF(m_bank.getBankName());
                clientMenuHandle();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                StopRunning();
            }
        }

        System.out.println("Ending thread for client: "+ m_id);



    }
}
