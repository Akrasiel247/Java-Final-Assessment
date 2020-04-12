package com.seneca.server;
import com.seneca.accounts.Chequing;
import com.seneca.accounts.GIC;
import com.seneca.business.Bank;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    public static void loadBank(Bank bank) {
        bank.addAccount(new Chequing("John Doe", "1234C", 123.45, 0.25, 3));
        bank.addAccount(new Chequing("Mary Ryan", "5678C", 678.90, 0.12, 3));
        bank.addAccount(new GIC("John Doe", "9999G", 6000, 2, .0150));
        bank.addAccount(new GIC("Mary Ryan", "888G", 15000, 4, .0250));
        bank.addAccount(new GIC("Mary Ryan", "778G", 12222, 4, .0250));

    }

    public static void main (String[] args) throws InterruptedException {
        ServerSocket serverSocket;
        final int Port = 5678;
        final int Max_Clients = 3;

        ArrayList<Thread> clients = new ArrayList<>();
        Bank myBank = new Bank("Final Assessment");
        loadBank(myBank);

        try{
            serverSocket =new ServerSocket(Port);
            System.out.println("Listening on port: "+ Port);

            for(int i = 0 ; i < Max_Clients; ++i){
                Socket socketConnection = serverSocket.accept();
                System.out.println("\nAccepted connection for client #"+ (i+1));

                Thread client = new HandleClient(i+1, socketConnection,myBank);
                clients.add(client);
                client.start();


            }

            for(int j = 0 ; j<Max_Clients; ++j){
                clients.get(j).join();
                System.out.println("This client is closing: "+ clients.get(j).getId());
            }


        } catch (IOException e) {
            e.printStackTrace();
        }


        System.out.println("Closing Server");
    }
}
