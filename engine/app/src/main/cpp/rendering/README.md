# Puente nativo: rendering

Implementación del dominio rendering. Se incluye desde `native_bridge.cpp` para conservar una única sesión privada y el ABI existente. Los archivos `.inc` son fuentes C++ compiladas en esa unidad, no motores duplicados. Las firmas y cuerpos JNI se verifican mediante `scripts/check_modular_boundaries.py` y las pruebas JNI/JVM.
