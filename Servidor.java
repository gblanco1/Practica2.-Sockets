import java.io.*;
import java.net.*;

public class Servidor {
    public static void main(String[] args) {
        // Comprovem que hi ha almenys 2 arguments (port i paraula clau)
    	if (args.length < 2) {
            System.out.println("Uso: java Servidor <puerto> <palabra_clave_servidor>");
            return;
        }

        int puerto;
        // Convertim el primer argument a numero (port)
        try {
            puerto = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Puerto invalido: " + args[0]);
            return;
        }

        // Guardem la paraula clau del servidor
        String serverKeyword = args[1];

        System.out.println("PORT_SERVIDOR: " + puerto);
        System.out.println("PARAULA_CLAU_SERVIDOR: \"" + serverKeyword + "\"");
        System.out.println("Server chat at port " + puerto);

        // Creem el ServerSocket (el servidor escolta connexions)
        try (ServerSocket servidor = new ServerSocket(puerto)) {
            System.out.println("Inicializing server... OK");
            System.out.println("Servidor esperando conexion...");

            // Accepta una connexio d un client (espera fins que arriba)
            try (Socket socket = servidor.accept();
                // Flux d entrada des del client
             	BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // Flux de sortida cap al client
            	PrintWriter socketOut = new PrintWriter(socket.getOutputStream(), true);
            	// Flux per llegir del teclat
            	BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

                System.out.println("Connection from client... OK");
                // Llegim la paraula clau del client
                String clientKeyword = socketIn.readLine();
                if (clientKeyword == null) {
                    System.out.println("No se recibio la palabra clave del cliente.");
                    return;
                }
             // Enviem la paraula clau del servidor al client
                socketOut.println(serverKeyword);

                System.out.println("Inicializing chat... OK");

                while (true) {
                    String mensaje = socketIn.readLine();
                    if (mensaje == null) {
                        System.out.println("El cliente cerro la conexion.");
                        break;
                    }

                    System.out.println("#Rebut del client: " + mensaje);
                    if (mensaje.equals(serverKeyword) || mensaje.equals(clientKeyword)) {
                        System.out.println("Client keyword detected!");
                        break;
                    }

                    System.out.print("#Enviar al client: ");
                    String respuesta = console.readLine();
                    if (respuesta == null) {
                        System.out.println("No se pudo leer del teclado.");
                        break;
                    }
                    // Si la resposta te la paraula clau, acabem
                    socketOut.println(respuesta);
                    if (respuesta.equals(serverKeyword) || respuesta.equals(clientKeyword)) {
                        System.out.println("Server keyword detected!");
                        break;
                    }
                }

                System.out.println("Closing chat... OK");
            }

            System.out.println("Closing server... OK");
            System.out.println("Bye!");
        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}