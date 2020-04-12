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

    private void openAcc(ObjectInputStream objFromClient) throws IOException, ClassNotFoundException {
        //read account sent by client
        Account a = (Account) objFromClient.readObject();

        String result = "";

        if (m_bank.addAccount(a)) {
            result=m_id+ ". Account successfully included";
        } else {
            result= m_id+". Unable to add Account";
        }

        System.out.println(result);
        dataToClient.writeUTF(result);
        System.out.println(m_bank);

    }

    private void closeAcc() throws IOException {
        String result="";
        //receive account num
        String acc_num = dataFromClient.readUTF();
        //Attempt to delete the account from the bank
        Account del = m_bank.removeAccount(acc_num);

        if(del != null){
            result=m_id+ ". Account successfully included";
        }else{
            result= m_id+". Unable to add Account";
        }

        System.out.println(result);
        dataToClient.writeUTF(result);
        System.out.println(m_bank);
    }

    private void depositMoney(ObjectOutputStream objToClient) throws IOException{
        String acc_num="";
        //get account num from client
        acc_num = dataFromClient.readUTF();

        Account depositAcc = m_bank.searchByAccountNumber(acc_num);
        //Send
        objToClient.writeObject(depositAcc);

        if(depositAcc!=null){
            //get the deposit amount from client
            double depositAmount=0;
            depositAmount = dataFromClient.readDouble();
            System.out.println(m_id+". Has chosen to deposit: "+depositAmount+" into account#: "+depositAcc.getAccountNumber());
            depositAcc.deposit(depositAmount);
            dataToClient.writeDouble(depositAcc.getAccountBalance());
            System.out.println(m_id+". Deposit Final Balance: "+ depositAcc.getAccountBalance());

        }
    }

    private void withdrawMoney() throws IOException{
        String acc_num="";
        //get account num from cliet
        acc_num = dataFromClient.readUTF();
        Account withdraw_Acc = m_bank.searchByAccountNumber(acc_num);

        if(withdraw_Acc!=null){
            dataToClient.writeBoolean(true);
            dataToClient.writeDouble(withdraw_Acc.getAccountBalance());
            double draw_amount = dataFromClient.readDouble();
            boolean res = withdraw_Acc.withdraw(draw_amount);
            dataToClient.writeBoolean(res);
            System.out.println(m_id+ ". Final Amount"+withdraw_Acc.getAccountBalance());

            dataToClient.writeDouble(withdraw_Acc.getAccountBalance());

        }else{
            dataToClient.writeBoolean(false);
        }
    }

    private void displayAccounts() throws IOException {
        String option = "";
        while (!option.equals("x")){
            System.out.println(m_id +". Waiting for displayAccounts choice");
            option = dataFromClient.readUTF();

            switch (option){
                //Search by account name
                case "a":
                case "A":
                    String name = dataFromClient.readUTF();
                    System.out.println(m_id +". Search for account with name: "+ name);
                    dataToClient.writeUTF(Arrays.toString(m_bank.searchByAccountName(name)));
                    break;
                //search by final balance
                case "b":
                case "B":
                    String balanceS = dataFromClient.readUTF();
                    double balance = Double.parseDouble(balanceS);
                    System.out.println(balance);
                    System.out.println(m_id +". Search for account with balance: "+ balance);
                    dataToClient.writeUTF(Arrays.toString(m_bank.searchByBalance(balance)));
                    break;
                //get all accounts in bank
                case "c":
                case "C":
                    dataToClient.writeUTF(Arrays.toString(m_bank.getAllAccounts()));
                    System.out.println(m_id +". Search for all accounts");
                    break;

                //search for a specific accoint
                case "d":
                case "D":
                    String acc_num = dataFromClient.readUTF();
                    Account a= m_bank.searchByAccountNumber(acc_num);
                    System.out.println(m_id +". Search for account with number: "+acc_num);
                    String result= a!=null? a.toString():"Account not found";
                    dataToClient.writeUTF(result);
                    break;

                //return to the main menu
                case "x":
                case "X":
                    System.out.println(m_id +". Operation cancelled, returning to main menu");
                    break;
                default:
                    System.out.println(m_id +".Invalid response, enter a valid response (a-d) or \"x\" to exit");
                    break;
            }
        }

    }

    private void displayTax() throws IOException{
        String name = dataFromClient.readUTF();
        StringBuilder results= new StringBuilder();

        int count = 1;

        for(Account acc: m_bank.getAllAccounts()){
            if(acc instanceof GIC && acc.getFullName().equals(name)){
                if(count == 1){
                    results.append("Tax rate: " + (Taxable.tax_rate * 100) + "%\n");
                    results.append("Name: ").append(acc.getLastName()).append(", ").append(acc.getFirstName()).append("\n");
                }
                results.append("[").append(count++).append("]\n");
                results.append(((GIC) acc).getTax());
            }
            count=0;
        }

        dataToClient.writeUTF(String.valueOf(results));


    }


    private void clientMenuHandle() throws IOException, ClassNotFoundException {
        ObjectOutputStream objToClient = new ObjectOutputStream(m_connection.getOutputStream());
        ObjectInputStream objFromClient = new ObjectInputStream(m_connection.getInputStream());
        int choice = 0;

        while (choice != 7){
            System.out.println(m_id+". Waiting for Client Menu Choice");
            String result="";
            choice = dataFromClient.readInt();
            switch (choice) {
                case 1: //Open an account
                    System.out.println(m_id+". Client has chosen to open an account");
                    openAcc(objFromClient);
                    break;
                case 2: //Close an account
                    System.out.println(m_id+". Client has chosen to close an account");
                    closeAcc();
                    break;
                case 3: //Deposit Money
                    System.out.println(m_id+". Client has chosen to deposit money into an account");

                    depositMoney(objToClient);
                    break;
                case 4:
                    System.out.println(m_id+". Client has chosen to withdraw money from an account");

                    withdrawMoney();
                    break;
                case 5:
                    System.out.println(m_id+". Client has chosen to display accounts");
                    displayAccounts();
                    break;
                case 6:
                    System.out.println(m_id+". Client has chosen to display tax statements");
                    displayTax();
                    break;
                case 7:
                    System.out.println(m_id+". Client has chosen to Exit");
                    StopRunning();
                    break;
                default:
                    System.out.println(m_id+". Client has inputted an invalid menu choice");
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

                System.out.println("Running HandleClient for client: "+ m_id);
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
