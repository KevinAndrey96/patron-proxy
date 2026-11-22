package Proxy;

/**
 * Punto de entrada para demostrar la ejecución del Patrón Proxy.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== 1. Acceso a través de Proxy (Lazy Loading & Smart Counting) ===");
        Sujeto objetoA = new Proxy("objetoA");
        Cliente clienteA = new Cliente(objetoA);

        // Antes de invocar métodos, el SujetoReal no ha sido creado en memoria
        objetoA.status();

        // Ejecuta llamadas a traves del Proxy
        clienteA.ejecutar();

        // Verificamos estado y conteo de accesos
        objetoA.status();

        System.out.println("\n=== 2. Acceso directo a SujetoReal (Sin intermediario) ===");
        Sujeto objetoB = new SujetoReal("objetoB");
        Cliente clienteB = new Cliente(objetoB);
        clienteB.ejecutar();
        objetoB.status();
    }
}
