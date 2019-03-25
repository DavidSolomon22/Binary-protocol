import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.*;
import java.io.IOException;

public class Client implements Runnable{

    private Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private Server server;
    private int sessionID;
    private byte[] packet = new byte[4];
    private boolean condition = true;

    Client(ServerSocket serverSocket, int clientID, Server server){
        try {
            clientSocket = serverSocket.accept();

            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            this.server = server;
            sessionID = clientID;

            sendPacket(0,0,0,0);    // initialization packet
            System.out.println("Client with ID " + sessionID + " connected.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // method that generates a packet in server - binary coding
    private byte[] generatePacket(int operation, int answer, int number, int attempt){

        //                 Construction of the packet
        //
        // 	        OOOOO AAA	  O - operation field, A - answer field
        // byte 1: |00000 000|
        //
        //          A III NNNN	  I - session ID field, N - number field
        // byte 2: |0 000 0000|
        //
        // 	        NNNN TTTT	  T - attempt field
        // byte 3: |0000 0000|
        //
        // 	        TTTT EEEE	  E - empty field
        // byte 4: |0000 0000|

        byte[] byteArray = new byte[4];

        byteArray[0] = (byte) ((operation & 0b00011111) << 3);
        byteArray[0] = (byte) (byteArray[0] | (byte) ((answer & 0b00001110) >> 1));

        byteArray[1] = (byte) ((answer & 0b00000001) << 7);
        byteArray[1] = (byte) (byteArray[1] | (byte) ((sessionID & 0b00000111) << 4));
        byteArray[1] = (byte) (byteArray[1] | (byte) ((number & 0b11110000) >> 4));

        byteArray[2] = (byte) ((number & 0b00001111) << 4);
        byteArray[2] = (byte) (byteArray[2] | (byte) ((attempt & 0b11110000) >> 4));

        byteArray[3] = (byte) ((attempt & 0b00001111) << 4);

        return byteArray;
    }

    // method that send packet form server to client
    void sendPacket(int operation, int answer, int number, int attempt){
        try {
            out.write(generatePacket(operation, answer, number, attempt),0,4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // method that ends connection
    private void end(){
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            condition = false;
        }
    }

    // method that execute proper instructions on the base of fields contained in received packets
    private void execute(int operation, int answer, int id, int number)
    {
        if(operation == 2 && answer == 2) // generate number of attempts
        {
            server.numberOf_attempts(id, number,this);
        }
        else if(operation == 3 && answer == 0) // check received packet
        {
            server.check(id, number,this);
        }
        else if(operation == 7 && answer == 7) // end connection
        {
            System.out.println("Client with ID "+ sessionID + " is disconnected.");
            end();
        }
    }

    // method that decode packet received from client - binary decoding
    private void decodePacket(byte[] byteArray){

        int operation, answer, id, number;

        operation = (byteArray[0] & 0b11111000) >> 3;
        answer = ((byteArray[0] & 0b00000111) << 1) | ((byteArray[1] & 0b10000000) >> 7);
        id = (byteArray[1] & 0b01110000) >> 4;
        number = ((byteArray[1] & 0b00001111) << 4) | ((byteArray[2] & 0b11110000) >> 4);

        if(id == sessionID){
            execute(operation, answer, id, number);
        }
        else{
            System.out.println("Statement received form the client with id " + id + ", is incorrect.");
        }
    }

    public void run(){

        int length;

        while(condition){
            try {
                length = in.read(packet);
                if (length == -1){
                    System.out.println("Client with ID " + sessionID + " is disconnected.");
                    condition = false;
                    break;
                } else {
                    decodePacket(packet);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}