package Proxy;

/**
 * Proxy estructural que actúa como intermediario para el SujetoReal.
 * Implementa Lazy Loading (instanciación diferida) y Smart Reference (conteo de accesos).
 */
public class Proxy extends Sujeto {

    private SujetoReal _sujetoReal;
    private int _accesosMetodo1;
    private int _accesosMetodo2;

    public Proxy(String nombre) {
        super(nombre);
        this._sujetoReal = null;
        this._accesosMetodo1 = 0;
        this._accesosMetodo2 = 0;
    }

    private SujetoReal obtenerSujetoReal() {
        if (_sujetoReal == null) {
            _sujetoReal = new SujetoReal(super.toString() + " (Real)");
        }
        return _sujetoReal;
    }

    @Override
    public void metodo1() {
        _accesosMetodo1++;
        obtenerSujetoReal().metodo1();
    }

    @Override
    public void metodo2() {
        _accesosMetodo2++;
        obtenerSujetoReal().metodo2();
    }

    @Override
    public String toString() {
        if (_sujetoReal != null) {
            return "Accesos a " + _sujetoReal + ": metodo1=" + _accesosMetodo1 + ", metodo2=" + _accesosMetodo2;
        } else {
            return "Sujeto Real (" + super.toString() + ") no creado aún (Lazy Init).";
        }
    }
}
