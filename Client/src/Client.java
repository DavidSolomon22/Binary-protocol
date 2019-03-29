import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client implements Runnable{

    private Socket clientSocket;
    private DataOutputStream out;
    private DataInputStream in;
    private int sessionID;
    private static boolean condition = true;
    private static boolean begin = true;
    private boolean in_game = false;

    private Client(String IP, int port){

        try {
            System.out.println("Waiting for connection...");

            clientSocket = new Socket(IP, port);
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // method that generates a packet in client - binary coding
    private byte[] generatePacket(int operation, int answer, int id, int number){

        //                 Construction of the packet
        //
        // 	        OOOOO AAA	  O - operation field, A - answer field
        // byte 1: |00000 000|
        //
        //          A III NNNN	  I - session ID field, N - number field
        // byte 2: |0 000 0000|
        //
        // 	        NNNN EEEE	  E - empty field
        // byte 3: |0000 0000|

        byte[] byteArray = new byte[4]; // check 3

        byteArray[0] = (byte) ((operation & 0b00011111) << 3);
        byteArray[0] = (byte) (byteArray[0] | (byte) ((answer & 0b00001110) >> 1));

        byteArray[1] = (byte) ((answer & 0b00000001) << 7);
        byteArray[1] = (byte) (byteArray[1] | (byte) ((id & 0b00000111) << 4));
        byteArray[1] = (byte) (byteArray[1] | (byte) ((number & 0b11110000) >> 4));

        byteArray[2] = (byte) ((number & 0b00001111) << 4);

        return byteArray;
    }

    // method that sends packet form client to server
    private void sendPacket(int operation, int answer, int id, int number){
        try {
            out.write(generatePacket(operation, answer, id, number),0,4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // method that decodes packet received from server - binary decoding
    private void decodePacket(byte[] byteArray){

        int operation, answer, id, number, attempt;

        operation = (byteArray[0] & 0b11111000) >> 3;
        answer = ((byteArray[0] & 0b00000111) << 1) | ((byteArray[1] & 0b10000000) >> 7);
        id = (byteArray[1] & 0b01110000) >> 4;
        number = ((byteArray[1] & 0b00001111) << 4) | ((byteArray[2] & 0b11110000) >> 4);
        attempt = ((byteArray[2] & 0b00001111) << 4) | ((byteArray[3] & 0b11110000) >> 4);

        if(id == sessionID)
        {
            execute(operation, answer, number, attempt);
        }
        else
        {
            if(operation == 0 && answer == 0)
            {
                sessionID = id;
            }
            else
            {
                System.out.println("Statement received form the server is incorrect.");
            }
        }
    }

    // method that executes proper instructions on the base of fields contained in received packets
    private void execute(int operation, int answer, int number, int attempt){
        // switch - reaction for the operation field
        // if - reaction for the answer field

        switch (operation){
            case 2:
                if(answer == 0){
                    System.out.println("Start of the game!");
                    System.out.print("\nType in odd number: ");
                    in_game = true;
                }
                else if(answer == 1){
                    System.out.println(attempt + " attempts left.");
                }
                break;
            case 3:
                if(in_game){
                    if(answer == 1){
                        System.out.println("Number is too small.");
                        System.out.println(attempt + " attempts left.");
                    }
                    if(answer == 2){
                        System.out.println(attempt + " attempts left.");
                    }
                    if(answer == 4){
                        System.out.println("Number is too big.");
                        System.out.println(attempt + " attempts left.");
                    }
                }
                break;
            case 7:
                if(answer == 1){
                    System.out.println("You won!");
                }

                if(answer == 2){
                    System.out.println("0  attempts left.\nYou lost.");
                }
                sendPacket(7,7,sessionID,0);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                in_game = condition = false;
                break;
            default:
                break;
        }
    }

    public static void main(String[] args){

        Client client = new Client("127.0.0.1", 1234);
        if(client.clientSocket != null)
        {
            System.out.println("Connected with server.");
            new Thread(client).start();
        }
        else
        {
            System.out.println("Unable to connect to the server.");
            condition = false;
        }
    }

    public void run(){

        byte[] byteArray = new byte[4];

        boolean t = true;
        boolean r = false;

        int number, length;
        int attempt;

        Scanner scanner = new Scanner(System.in);

        // loop needed to read first odd number
        while(begin)
        {
            try {
                if (in.available() > 0){
                    length = in.read(byteArray);
                    if(length == -1){
                        condition = false;
                    } else{
                        decodePacket(byteArray);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (System.in.available() > 0){
                    attempt = scanner.nextInt();
                    sendPacket(2,2, sessionID, attempt);
                    condition = true;
                    begin = false;

                }
            } catch (Throwable e) {
                System.out.println(e.getMessage());
            }
        }

        // main loop
        while(condition){

            System.out.print("\nType in number: ");

            // here we type number in
            while(t)
            {
                try {
                    if (System.in.available() > 0){
                        number = scanner.nextInt();
                        sendPacket(3,0, sessionID, number);
                        t = false;
                        r = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // here we receive and decode packet
            while(r)
            {
                try {
                    if (in.available() > 0) {
                        length = in.read(byteArray);
                        if (length == -1) {
                            condition = false;
                        } else {
                            decodePacket(byteArray);
                            t = true;
                            r = false;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}