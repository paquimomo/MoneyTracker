MoneyTracker
Una aplicación móvil Android para el seguimiento y gestión de gastos personales, desarrollada con Kotlin y Firebase.


📱 Características
🔐 Autenticación

Registro e inicio de sesión con Firebase Authentication
Persistencia de sesión con SharedPreferences
Recuperación de contraseña
Auto-login al abrir la aplicación

💸 Gestión de Gastos

Agregar gastos con descripción, monto, categoría, fecha y hora
Captura de fotos opcional para cada gasto
Ubicación GPS automática
Editar y eliminar gastos
Filtrar por categoría
Exportar datos

📊 Categorías Personalizadas

7 categorías predeterminadas
Crear categorías personalizadas
Selector de colores (12 opciones)
Límites mensuales configurables
Progreso visual con barras de color
Alertas al superar el 80% del límite

🗺️ Mapa de Gastos

Visualización de todos los gastos en Google Maps
Marcadores con colores según monto:

🟢 Verde: < $500
🟠 Naranja: $500 - $1000
🔴 Rojo: > $1000


Ubicación actual del usuario
Encontrar gasto más cercano
Trazado de ruta con distancia en km

🔔 Notificaciones y Alarmas

Notificación al guardar un gasto
Alerta al exceder límite de categoría
Recordatorio diario configurable
Resumen semanal automático

📱 Sensores

Cámara: Captura de fotos de gastos
Acelerómetro: Agita el dispositivo para ver un gasto aleatorio

📈 Dashboard

Total de gastos registrados
Monto total acumulado
Fotos guardadas
Categoría más utilizada

🛠️ Tecnologías
Lenguaje y Plataforma

Kotlin - Lenguaje de programación principal
Android SDK - Plataforma móvil
Material Design 3 - Diseño de interfaz

Backend

Firebase Authentication - Autenticación de usuarios
Cloud Firestore - Base de datos NoSQL en tiempo real
Firebase Storage - Almacenamiento de imágenes (opcional)

APIs y Servicios

Google Maps SDK - Mapas y localización
Fused Location Provider - Obtención de ubicación GPS
AlarmManager - Programación de alarmas
NotificationManager - Sistema de notificaciones


👨‍💻 Autor
Francisco Morales

GitHub: @paquimomo
Email: pacofut.moralesg@gmail.com
