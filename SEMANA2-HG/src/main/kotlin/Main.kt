fun main() {
   /// print("Ingrese el primer número: ")
    val num1 = readLine()!!.toDouble()

    print("Ingrese el segundo número: ")
    val num2 = readLine()!!.toDouble()

    val suma = num1 + num2
    val resta = num1 - num2
    val multiplicacion = num1 * num2

    println("Resultados:")
    println("Suma: $suma")
    println("Resta: $resta")
    println("Multiplicación: $multiplicacion")

    if (num2 != 0.0) {
        val division = num1 / num2
        println("División: $division")
    } else {
        println("División: No se puede dividir entre cero.")
    }
}