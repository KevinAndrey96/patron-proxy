package Proxy;

/**
 * Interfaz / Clase base abstracta del patrón Proxy.
 * Define las operaciones comunes que tanto el SujetoReal como el Proxy implementan.
 */
public abstract class Sujeto {

    private final String _nombre;

    public Sujeto(String nombre) {
        this._nombre = nombre;
    }

    public String getNombre() {
        return _nombre;
    }

    @Override
    public String toString() {
        return _nombre;
    }

    public abstract void metodo1();
    public abstract void metodo2();

    public void status() {
        System.out.println(this.toString());
    }
}
