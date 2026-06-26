fun main() {
    print("Ingrese un número: ")
    val numero = readLine()!!.toInt()

    if (numero % 2 == 0) {
        println("El número $numero es par.")
    } else {
        println("El número $numero es impar.")
    }
}