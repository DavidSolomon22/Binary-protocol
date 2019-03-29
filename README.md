# Binary-protocol

One server. Two players. All you need to win the game, is to divine number.

Communication between server and client is based on binary coding and TCP.
It also uses multithreading in Java.

That's how construction of the packet looks like:

    byte 1: |00000 000|
             OOOOO AAA	  O - operation field, A - answer field
                    
    byte 2: |0 000 0000|                
             A III NNNN	  I - session ID field, N - number field
       
    byte 3: |0000 0000|
             NNNN TTTT	  T - attempt field
       
    byte 4: |0000 0000|    
             TTTT EEEE	  E - empty field
              
To turn on the game you need run in 3 different windows:
  - Server app (run it as first),
  - Client app
  - Client_2 app
