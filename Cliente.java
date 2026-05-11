import java.io.*;
import java.net.*;

public class Cliente {
    public static void main(String[] args) {
        // Comprovem que es passen almenys 2 arguments (port i paraula clau)
    	if (args.length < 2) {
            System.out.println("Uso: java Cliente <puerto> <palabra_clave_cliente>");
            return;
        }

        int puerto;
        // Intentem convertir el primer argument a número (port)
        try {
            puerto = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Puerto invalido: " + args[0]);
            return;
        }

        String clientKeyword = args[1];

        System.out.println("PORT_SERVIDOR: " + puerto);
        System.out.println("PARAULA_CLAU_CLIENT: \"" + clientKeyword + "\"");
        System.out.println("Client chat to port " + puerto);

        // Creem un socket que es connecta al servidor (localhost) i al port indicat
        try (Socket socket = new Socket("127.0.0.1", puerto);
            // Flux d’entrada des del servidor 
        	BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Flux de sortida cap al servidor
        	PrintWriter socketOut = new PrintWriter(socket.getOutputStream(), true);
            // Flux d’entrada des del teclat
        	BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Inicializing client... OK");

            // Enviem la paraula clau del client al servidor
            socketOut.println(clientKeyword);
            // Llegim la paraula clau del servidor
            String serverKeyword = socketIn.readLine();
            if (serverKeyword == null) {
                System.out.println("No se recibio la palabra clave del servidor.");
                return;
            }

            System.out.println("Inicializing chat... OK");

            // Bucle principal del chat
            while (true) {

                // Demanem un missatge a l’usuari
                System.out.print("#Enviar al servidor: ");
                String mensaje = console.readLine();

                // Si no es pot llegir del teclat, sortim
                if (mensaje == null) {
                    System.out.println("No s'ha pogut llegir del teclat.");
                    break;
                }

                // Enviem el missatge al servidor
                socketOut.println(mensaje);

                // Si el missatge és una paraula clau, acabem
                if (mensaje.equals(clientKeyword) || mensaje.equals(serverKeyword)) {
                    System.out.println("Client keyword detected!");
                    break;
                }

                // Esperem resposta del servidor
                String respuesta = socketIn.readLine();

                // Si el servidor tanca la connexió
                if (respuesta == null) {
                    System.out.println("El servidor ha tancat la connexió.");
                    break;
                }

                // Mostrem la resposta del servidor
                System.out.println("#Rebut del servidor: " + respuesta);

                // Si la resposta te la paraula clau, acabem
                if (respuesta.equals(clientKeyword) || respuesta.equals(serverKeyword)) {
                    System.out.println("Server keyword detected!");
                    break;
                }
            }

            //Missatge per tancar
            System.out.println("Closing chat... OK");
            System.out.println("Closing client... OK");
            System.out.println("Bye!");
        } catch (IOException e) {
            System.out.println("Error en el cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
}