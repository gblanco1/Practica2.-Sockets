import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Servidor {
    // NUEVA FUNCIONALIDAD - FASE 2: Clase para representar un mensaje en la cola
    static class Mensaje {
        int clientId;
        String contenido;
        
        public Mensaje(int clientId, String contenido) {
            this.clientId = clientId;
            this.contenido = contenido;
        }
    }
    
    // NUEVA FUNCIONALIDAD - FASE 2: Clase para representar un cliente activo
    static class ClienteActivo {
        int clientId;
        Socket socket;
        PrintWriter socketOut;
        String clientKeyword;
        String serverKeyword;
        boolean activo;
        
        public ClienteActivo(int clientId, Socket socket, PrintWriter socketOut, String clientKeyword, String serverKeyword) {
            this.clientId = clientId;
            this.socket = socket;
            this.socketOut = socketOut;
            this.clientKeyword = clientKeyword;
            this.serverKeyword = serverKeyword;
            this.activo = true;
        }
    }
    
    // NUEVA FUNCIONALIDAD - FASE 2: Variables globales sincronizadas
    static Queue<Mensaje> colaMensajes = new LinkedBlockingQueue<>();
    static List<ClienteActivo> clientesActivos = Collections.synchronizedList(new ArrayList<>());
    
    // NUEVA FUNCIONALIDAD - FASE 3: Control de cierre del servidor
    static boolean debeTerminar = false;
    static boolean primerClienteConectado = false;
    static int nextClientId = 1;
    
    public static void main(String[] args) {
        // Comprovem que hi ha almenys 3 arguments (port, paraula clau, max clientes)
    	if (args.length < 3) {
            System.out.println("Uso: java Servidor <puerto> <palabra_clave_servidor> <max_clientes>");
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
        
        // Convertim el tercer argument a numero (max clientes)
        int maxClientes;
        try {
            maxClientes = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.out.println("Max clientes invalido: " + args[2]);
            return;
        }

        System.out.println("PORT_SERVIDOR: " + puerto);
        System.out.println("PARAULA_CLAU_SERVIDOR: \"" + serverKeyword + "\"");
        System.out.println("Server chat at port " + puerto);

        // Creem el ServerSocket (el servidor escolta connexions)
        try (ServerSocket servidor = new ServerSocket(puerto)) {
            System.out.println("Inicializing server... OK");
            
            // NUEVA FUNCIONALIDAD - FASE 2: Hilo que maneja el teclado del servidor
            Thread threadTeclado = new Thread(new ServerKeyboardHandler(serverKeyword));
            threadTeclado.start();

            // NUEVA FUNCIONALIDAD - FASE 3: Bucle de aceptación con reutilización de espacios
            while (!debeTerminar) {
                // Verificar si hay espacio para nuevos clientes
                if (clientesActivos.size() < maxClientes) {
                    try {
                        Socket socket = servidor.accept();
                        
                        // Crear un hilo para cada cliente
                        Thread clientThread = new Thread(new ClientHandler(socket, serverKeyword));
                        clientThread.start();
                    } catch (SocketException e) {
                        // El servidor puede estar cerrado, salir del bucle
                        break;
                    }
                } else {
                    // Si alcanzamos el máximo, esperar un poco antes de reintentar
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            System.out.println("Closing server... OK");
            System.out.println("Bye!");
        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // NUEVA FUNCIONALIDAD - FASE 2: Hilo que maneja el teclado del servidor
    static class ServerKeyboardHandler implements Runnable {
        private String serverKeyword;
        
        public ServerKeyboardHandler(String serverKeyword) {
            this.serverKeyword = serverKeyword;
        }
        
        @Override
        public void run() {
            try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
                while (!debeTerminar) {
                    // NUEVA FUNCIONALIDAD - FASE 3: Monitoreo de clientes
                    if (primerClienteConectado && clientesActivos.isEmpty()) {
                        System.out.println("No hay clientes conectados. Terminando servidor...");
                        debeTerminar = true;
                        break;
                    }
                    
                    // Esperamos que haya un mensaje en la cola
                    Mensaje mensaje = colaMensajes.poll();
                    
                    if (mensaje != null) {
                        // Mostrar el mensaje recibido (ya mostrado por ClientHandler)
                        // Leer la respuesta del servidor
                        System.out.print("#Enviar al client " + mensaje.clientId + ": ");
                        String respuesta = console.readLine();
                        
                        if (respuesta != null) {
                            // Buscar el cliente en la lista activa
                            ClienteActivo cliente = null;
                            synchronized (clientesActivos) {
                                for (ClienteActivo c : clientesActivos) {
                                    if (c.clientId == mensaje.clientId) {
                                        cliente = c;
                                        break;
                                    }
                                }
                            }
                            
                            if (cliente != null && cliente.activo) {
                                // Enviar respuesta al cliente
                                cliente.socketOut.println(respuesta);
                                
                                // Comprobar si es palabra clave
                                if (respuesta.equals(serverKeyword)) {
                                    // Cerrar TODOS los chats
                                    System.out.println("Server keyword detected!");
                                    cerrarTodosChats();
                                    debeTerminar = true;
                                    break;
                                } else if (respuesta.equals(cliente.clientKeyword)) {
                                    // Cerrar solo el chat de este cliente
                                    System.out.println("Client " + mensaje.clientId + " keyword detected!");
                                    cliente.activo = false;
                                    cliente.socket.close();
                                    synchronized (clientesActivos) {
                                        clientesActivos.remove(cliente);
                                    }
                                }
                            }
                        }
                    } else {
                        // Si no hay mensajes en la cola, esperar un poco
                        Thread.sleep(100);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error en el teclado del servidor: " + e.getMessage());
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println("Hilo de teclado interrumpido.");
            }
        }
        
        private void cerrarTodosChats() {
            synchronized (clientesActivos) {
                for (ClienteActivo cliente : new ArrayList<>(clientesActivos)) {
                    try {
                        cliente.activo = false;
                        cliente.socket.close();
                    } catch (IOException e) {
                        System.out.println("Error al cerrar cliente " + cliente.clientId);
                    }
                }
                clientesActivos.clear();
            }
        }
    }
    
    // NUEVA FUNCIONALIDAD - FASE 1/2: Clase para manejar cada cliente en su propio hilo
    static class ClientHandler implements Runnable {
        private Socket socket;
        private int clientId;
        private String serverKeyword;
        
        public ClientHandler(Socket socket, String serverKeyword) {
            this.socket = socket;
            this.serverKeyword = serverKeyword;
            // NUEVA FUNCIONALIDAD - FASE 3: Generar ID único para cada cliente
            synchronized (Servidor.class) {
                this.clientId = nextClientId++;
            }
        }
        
        @Override
        public void run() {
            try (BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter socketOut = new PrintWriter(socket.getOutputStream(), true)) {
                
                System.out.println("Connection from client " + clientId + " ... OK");
                
                // NUEVA FUNCIONALIDAD - FASE 3: Marcar que ha llegado el primer cliente
                if (!primerClienteConectado) {
                    primerClienteConectado = true;
                }
                
                // Llegim la paraula clau del client
                String clientKeyword = socketIn.readLine();
                if (clientKeyword == null) {
                    System.out.println("No se recibio la palabra clave del cliente " + clientId);
                    return;
                }
                
                // Enviem la paraula clau del servidor al client
                socketOut.println(serverKeyword);
                
                // NUEVA FUNCIONALIDAD - FASE 2: Agregar cliente a la lista activa
                ClienteActivo clienteActivo = new ClienteActivo(clientId, socket, socketOut, clientKeyword, serverKeyword);
                clientesActivos.add(clienteActivo);
                
                System.out.println("Inicializing chat... OK");
                
                while (clienteActivo.activo && !debeTerminar) {
                    String mensaje = socketIn.readLine();
                    if (mensaje == null) {
                        System.out.println("El cliente " + clientId + " cerro la conexion.");
                        break;
                    }
                    
                    System.out.println("#Rebut del client " + clientId + ": " + mensaje);
                    
                    // Comprobar palabras clave del cliente
                    if (mensaje.equals(clientKeyword)) {
                        System.out.println("Client keyword detected!");
                        break;
                    }
                    
                    // Si es la palabra clave del servidor, solo se agrega a la cola
                    // El servidor decidirá qué hacer (ignorar o cerrar todos)
                    if (mensaje.equals(serverKeyword)) {
                        System.out.println("Server keyword from client " + clientId + " (ignored)");
                    }
                    
                    // NUEVA FUNCIONALIDAD - FASE 2: Agregar mensaje a la cola
                    colaMensajes.add(new Mensaje(clientId, mensaje));
                }
                
                System.out.println("Closing chat... OK");
                clienteActivo.activo = false;
                clientesActivos.remove(clienteActivo);
                socket.close();
                
            } catch (IOException e) {
                System.out.println("Error en el cliente " + clientId + ": " + e.getMessage());
            }
        }
    }
}