# Valle-Go

## 1. Descripción general

**Valle-Go** es una aplicación móvil nativa para Android orientada a facilitar la compra y venta de productos dentro de un campus universitario.

La plataforma conectará a tres tipos principales de usuarios:

- **Estudiante o comprador**
- **Emprendedor o vendedor**
- **Administrador**

La aplicación funcionará inicialmente dentro de un solo campus universitario, aunque su arquitectura deberá quedar preparada para permitir en el futuro múltiples campus, sedes o universidades.

Valle-Go no funcionará inicialmente como un servicio de delivery tradicional. La entrega se realizará mediante **puntos de encuentro definidos dentro del campus**.

El pago será **contra entrega**. La aplicación no procesará dinero directamente. El comprador realizará el pago al momento del encuentro mediante efectivo, Yape, Plin, transferencia u otro medio aceptado, y posteriormente el emprendedor confirmará desde la aplicación que recibió el pago.

---

# 2. Objetivo principal

El objetivo de Valle-Go es permitir que los estudiantes puedan descubrir y comprar productos ofrecidos por emprendimientos de la comunidad universitaria de forma rápida, ordenada y segura.

El flujo general será:

```text
Comprador
   ↓
Busca productos
   ↓
Agrega productos de uno o varios emprendimientos
   ↓
Confirma su carrito
   ↓
Selecciona punto y horario de encuentro
   ↓
El sistema genera una orden
   ↓
La orden se divide en subpedidos por emprendimiento
   ↓
Cada emprendedor acepta o rechaza su subpedido
   ↓
Los vendedores preparan los productos
   ↓
Comprador y vendedores se encuentran
   ↓
El comprador paga directamente
   ↓
Cada vendedor confirma su pago y entrega
   ↓
La orden queda completada cuando todos los subpedidos finalizan
```

---

# 3. Roles del sistema

## 3.1. Estudiante o comprador

El estudiante podrá:

- Registrarse.
- Iniciar sesión.
- Recuperar contraseña.
- Gestionar su perfil.
- Buscar emprendimientos.
- Buscar productos.
- Explorar categorías.
- Visualizar promociones.
- Consultar disponibilidad.
- Agregar productos al carrito.
- Agregar productos de diferentes emprendimientos.
- Modificar cantidades.
- Eliminar productos del carrito.
- Visualizar el subtotal por emprendimiento.
- Visualizar el total general.
- Seleccionar un punto de encuentro.
- Seleccionar horario de encuentro.
- Confirmar una compra.
- Consultar el estado general de su orden.
- Consultar el estado individual de cada subpedido.
- Cancelar pedidos cuando las reglas de negocio lo permitan.
- Consultar historial.
- Repetir compras anteriores.
- Guardar productos favoritos.
- Guardar emprendimientos favoritos.
- Calificar productos.
- Calificar emprendimientos.
- Reportar problemas.
- Recibir notificaciones.

---

# 4. Carrito con múltiples emprendimientos

Valle-Go permitirá que un comprador agregue productos pertenecientes a diferentes emprendimientos dentro de un mismo carrito.

Ejemplo:

```text
CARRITO VALLE-GO

Papu Burger
--------------------------------
2 Hamburguesas       S/ 20.00
1 Gaseosa            S/  3.00
Subtotal             S/ 23.00

Dulce Valle
--------------------------------
2 Brownies           S/ 10.00
Subtotal             S/ 10.00

Coffee Campus
--------------------------------
1 Café               S/  6.00
Subtotal             S/  6.00

TOTAL GENERAL        S/ 39.00
```

Para el comprador existe una sola experiencia de compra.

Sin embargo, internamente el sistema divide la operación.

```text
ORDEN #500

├── Subpedido #500-1
│   Emprendimiento: Papu Burger
│   Total: S/23
│
├── Subpedido #500-2
│   Emprendimiento: Dulce Valle
│   Total: S/10
│
└── Subpedido #500-3
    Emprendimiento: Coffee Campus
    Total: S/6
```

Esto permite que cada emprendedor pueda administrar solamente los productos que le corresponden.

---

# 5. Orden principal y subpedidos

## Orden

Representa toda la compra realizada por el estudiante.

Ejemplo:

```text
Orden #500

Comprador:
Juan Pérez

Punto:
Biblioteca

Hora:
1:00 PM

Total:
S/39

Cantidad de emprendimientos:
3
```

## Subpedido

Cada emprendimiento involucrado genera su propio subpedido.

Ejemplo:

```text
Subpedido #500-2

Orden:
#500

Emprendimiento:
Dulce Valle

Productos:
2 Brownies

Subtotal:
S/10

Estado:
EN PREPARACIÓN
```

Cada vendedor podrá aceptar, rechazar, preparar y completar únicamente su subpedido.

---

# 6. Aceptación independiente

Una de las ventajas de este modelo es que los vendedores no dependen entre sí.

Por ejemplo:

```text
Orden #500

Papu Burger
ACEPTADO

Dulce Valle
ACEPTADO

Coffee Campus
RECHAZADO
```

Esto no significa necesariamente que toda la orden sea cancelada.

El comprador podrá continuar con los productos aceptados.

El sistema deberá recalcular:

```text
Total original:
S/39

Coffee Campus rechazó:
S/6

Nuevo total:
S/33
```

El comprador recibirá una notificación informándole qué emprendimiento rechazó su pedido.

---

# 7. Punto de encuentro

Los puntos de encuentro serán registrados previamente por el administrador.

Ejemplos:

```text
Biblioteca - entrada principal

Pabellón A

Pabellón B

Patio central

Cafetería

Facultad de Ingeniería
```

Esto evita utilizar direcciones abiertas o ambiguas.

El comprador seleccionará un punto desde una lista.

```text
Punto de encuentro

[ Biblioteca ▼ ]
```

Inicialmente, todos los subpedidos de una misma orden utilizarán el mismo punto de encuentro.

Esto simplifica la coordinación del comprador.

---

# 8. Horario de encuentro

El comprador deberá seleccionar un horario.

Los horarios disponibles podrán trabajar mediante intervalos.

Por ejemplo:

```text
12:00 PM
12:30 PM
1:00 PM
1:30 PM
2:00 PM
```

Para una primera versión se recomienda utilizar intervalos de **30 minutos**.

Idealmente, el sistema deberá comprobar que los emprendimientos seleccionados se encuentren disponibles en ese horario.

Si alguno no puede atender ese horario, el sistema podrá:

- informar al comprador antes de confirmar;
- solicitar seleccionar otro horario;
- o permitir que el vendedor proponga un nuevo horario.

---

# 9. Rol emprendedor

El emprendedor podrá:

- Solicitar autorización para vender.
- Crear un emprendimiento.
- Configurar nombre comercial.
- Agregar logo.
- Agregar descripción.
- Definir horarios.
- Gestionar disponibilidad.
- Crear productos.
- Editar productos.
- Desactivar productos.
- Gestionar stock.
- Configurar precios.
- Crear promociones.
- Recibir subpedidos.
- Aceptar subpedidos.
- Rechazar subpedidos.
- Cambiar el estado de preparación.
- Consultar lugar de encuentro.
- Consultar hora de encuentro.
- Confirmar recepción del pago.
- Confirmar entrega.
- Consultar historial.
- Consultar ventas.
- Consultar productos vendidos.
- Consultar estadísticas.
- Visualizar calificaciones.
- Gestionar incidencias relacionadas con sus pedidos.

---

# 10. Estado del emprendimiento

Cada emprendimiento podrá tener estados como:

```text
ABIERTO

CERRADO

PAUSADO

SATURADO
```

## ABIERTO

Puede recibir pedidos.

## CERRADO

No acepta pedidos en ese momento.

## PAUSADO

El emprendedor ha detenido temporalmente sus ventas.

## SATURADO

El emprendimiento está trabajando, pero tiene una gran cantidad de pedidos.

Esto podrá mostrarse al comprador antes de agregar productos.

---

# 11. Rol administrador

El administrador será responsable del ecosistema Valle-Go.

Podrá:

## Usuarios

- Consultar usuarios.
- Suspender usuarios.
- Bloquear usuarios.
- Reactivar cuentas.
- Revisar reportes.
- Consultar historial.

## Emprendedores

Los usuarios deberán solicitar autorización.

Flujo:

```text
Usuario solicita ser emprendedor
       ↓
Administrador revisa
       ↓
APROBADO / RECHAZADO
```

Estados posibles:

```text
PENDIENTE

APROBADO

RECHAZADO

SUSPENDIDO
```

## Productos

El administrador podrá:

- Gestionar categorías.
- Revisar productos.
- Ocultar publicaciones.
- Revisar productos reportados.
- Suspender publicaciones.

## Pedidos

Podrá consultar:

- órdenes;
- subpedidos;
- pedidos pendientes;
- aceptados;
- completados;
- cancelados;
- rechazados;
- pedidos con incidencias.

## Puntos de encuentro

Podrá:

- crear puntos;
- editar puntos;
- desactivarlos;
- establecer horarios permitidos.

---

# 12. Lógica del pago

Valle-Go utilizará **pago contra entrega**.

La aplicación no procesará dinero.

La lógica será:

```text
Subpedido aceptado
      ↓
Producto preparado
      ↓
Comprador y vendedor se encuentran
      ↓
Comprador realiza el pago
      ↓
Vendedor verifica el pago
      ↓
Vendedor confirma desde Valle-Go
      ↓
Pago confirmado
      ↓
Entrega confirmada
      ↓
Subpedido completado
```

---

# 13. Métodos de pago

Aunque la aplicación no procese pagos, podrá registrar el método utilizado.

Ejemplos:

```text
EFECTIVO

YAPE

PLIN

TRANSFERENCIA

OTRO
```

Ejemplo de registro:

```text
Subpedido:
#500-2

Monto:
S/10

Método:
Yape

Estado:
CONFIRMADO

Confirmado por:
Emprendedor

Hora:
1:04 PM
```

---

# 14. Confirmación del pago

El comprador no será quien confirme definitivamente que el pago fue recibido.

La confirmación deberá realizarla el vendedor.

Ejemplo:

```text
Total:
S/10

[ CONFIRMAR PAGO Y ENTREGA ]
```

Al presionar:

```text
Pago:
CONFIRMADO

Subpedido:
COMPLETADO
```

Esto evita que un comprador marque unilateralmente un pago como realizado cuando el vendedor todavía no lo recibió.

---

# 15. Pago independiente por emprendimiento

Debido a que una orden puede contener varios emprendimientos, el pago se administra por subpedido.

Ejemplo:

```text
Orden #500
Total general: S/33
```

En el punto de encuentro:

```text
Papu Burger
S/23
Pago confirmado ✓

Dulce Valle
S/10
Pago confirmado ✓
```

Cada emprendedor confirma solamente el monto correspondiente a sus productos.

La orden general se considera completamente finalizada cuando todos sus subpedidos aceptados se encuentren completados.

---

# 16. Estados del subpedido

Los estados iniciales pueden ser:

```text
PENDIENTE
   ↓
ACEPTADO
   ↓
EN_PREPARACION
   ↓
LISTO
   ↓
ESPERANDO_ENTREGA
   ↓
PAGO_CONFIRMADO
   ↓
COMPLETADO
```

También pueden existir:

```text
RECHAZADO

CANCELADO

NO_ENTREGADO
```

---

# 17. Estado general de la orden

Como una orden puede tener diferentes subpedidos, el estado general se calculará utilizando sus estados internos.

Ejemplo:

```text
Orden #500

Papu Burger
COMPLETADO

Dulce Valle
EN PREPARACIÓN

Coffee Campus
RECHAZADO
```

La orden podría mostrarse como:

```text
EN PROCESO
```

Cuando todos los subpedidos aceptados estén completados:

```text
COMPLETADA
```

Otros estados generales podrían ser:

```text
PENDIENTE

PARCIALMENTE_ACEPTADA

EN_PROCESO

COMPLETADA

CANCELADA
```

---

# 18. Cancelaciones

Las cancelaciones deberán manejarse por subpedido.

Ejemplo:

```text
Orden #500

Papu Burger
CANCELADO

Dulce Valle
ACEPTADO
```

Cancelar un subpedido no necesariamente cancela toda la orden.

Reglas iniciales:

```text
PENDIENTE
Cancelación libre

ACEPTADO
Cancelación permitida bajo determinadas condiciones

EN_PREPARACION
Cancelación restringida

LISTO
No permitir cancelación automática

ESPERANDO_ENTREGA
No permitir cancelación automática
```

En fases avanzadas podrá utilizarse un sistema de incidencias.

---

# 19. Si el comprador no aparece

Cuando llegue la hora acordada y el comprador no se presente, el vendedor podrá marcar:

```text
[ COMPRADOR NO SE PRESENTÓ ]
```

El subpedido pasará a:

```text
NO_ENTREGADO
```

El sistema guardará:

- usuario;
- emprendimiento;
- fecha;
- hora;
- punto;
- monto;
- motivo;
- evidencia opcional;
- estado.

Estos datos podrán utilizarse posteriormente para detectar abuso del sistema.

---

# 20. Si un vendedor no aparece

También deberá existir el caso contrario.

El comprador podrá reportar:

```text
[ VENDEDOR NO SE PRESENTÓ ]
```

El sistema podrá generar una incidencia.

Esto permitirá mantener la plataforma equilibrada para ambos roles.

---

# 21. Sistema de reputación

El sistema podrá registrar estadísticas.

## Comprador

```text
Pedidos realizados

Pedidos completados

Cancelaciones

Pedidos no recogidos
```

## Emprendedor

```text
Pedidos recibidos

Pedidos aceptados

Pedidos rechazados

Pedidos completados

Entregas fallidas

Calificación promedio
```

Inicialmente estas métricas pueden utilizarse internamente.

---

# 22. Gestión de stock

Ejemplo:

```text
Brownie

Stock:
20
```

Si un comprador solicita:

```text
Cantidad:
3
```

El sistema deberá reservar las unidades.

```text
Stock total:
20

Stock reservado:
3

Stock disponible:
17
```

Si el vendedor rechaza:

```text
Stock disponible:
20
```

Esto evita vender el mismo producto más veces de las disponibles.

---

# 23. Concurrencia de stock

Puede ocurrir:

```text
Stock disponible:
1
```

Juan intenta comprar una unidad.

Pedro intenta comprar la misma unidad al mismo tiempo.

El backend deberá garantizar que solamente uno pueda reservarla.

Esta lógica debe resolverse en el servidor y en la base de datos, no solamente desde Android.

---

# 24. Notificaciones

El comprador podrá recibir:

```text
Tu pedido en Papu Burger fue aceptado.

Tu pedido en Dulce Valle está siendo preparado.

Coffee Campus rechazó tu pedido.

Tu pedido está listo.

Recuerda tu encuentro a la 1:00 PM.

Pago confirmado.

Compra completada.
```

El emprendedor podrá recibir:

```text
Nuevo pedido recibido.

Pedido #500-2

2 Brownies

Total:
S/10

Lugar:
Biblioteca

Hora:
1:00 PM
```

En una fase posterior se podrá implementar Firebase Cloud Messaging.

---

# 25. Pantalla principal del comprador

Ejemplo:

```text
VALLE-GO

¿Qué estás buscando?

[ Buscar productos... ]

Categorías

Comida
Postres
Bebidas
Ropa
Manualidades
Académico
Otros

Emprendimientos destacados

Promociones

Productos nuevos
```

---

# 26. Pantalla de producto

Ejemplo:

```text
Brownie clásico

Dulce Valle

★★★★★ 4.8

Precio:
S/5

Stock:
12

Descripción:
Brownie artesanal de chocolate.

Cantidad:

[-] 2 [+]

[ AGREGAR AL CARRITO ]
```

---

# 27. Pantalla del carrito

Ejemplo:

```text
MI CARRITO

Papu Burger

2 Hamburguesas
S/20

1 Gaseosa
S/3

Subtotal:
S/23


Dulce Valle

2 Brownies
S/10

Subtotal:
S/10


TOTAL GENERAL:
S/33

[ CONTINUAR ]
```

---

# 28. Checkout

Ejemplo:

```text
RESUMEN DE COMPRA

Papu Burger
S/23

Dulce Valle
S/10

TOTAL:
S/33
```

Luego:

```text
Punto de encuentro

[ Biblioteca ▼ ]

Hora

[ 1:00 PM ▼ ]

Método:

Pago contra entrega

[ CONFIRMAR ORDEN ]
```

---

# 29. Pantalla del vendedor

Ejemplo:

```text
NUEVO PEDIDO

Orden:
#500

Subpedido:
#500-2

Comprador:
Juan Pérez

2 Brownies

Subtotal:
S/10

Lugar:
Biblioteca

Hora:
1:00 PM

[ RECHAZAR ]

[ ACEPTAR ]
```

---

# 30. Pantalla de entrega

Ejemplo:

```text
Pedido #500-2

Lugar:
Biblioteca

Hora:
1:00 PM

Total:
S/10

Estado:
Esperando pago
```

Después:

```text
Método recibido

○ Efectivo
○ Yape
○ Plin
○ Transferencia
○ Otro

[ CONFIRMAR PAGO Y ENTREGA ]
```

Finalmente:

```text
Pedido completado ✓
```

---

# 31. Entidades principales

Antes de crear las tablas, el sistema deberá trabajar conceptualmente con entidades como:

```text
Usuario

Rol

Campus

Emprendimiento

Producto

Categoría

Carrito

DetalleCarrito

Orden

Subpedido

DetalleSubpedido

PuntoEncuentro

Pago

Notificación

Promoción

Calificación

Reporte

Incidencia
```

---

# 32. Modelo conceptual inicial

```text
USUARIO
   │
   ├── COMPRADOR
   │
   └── EMPRENDEDOR
            │
            ▼
      EMPRENDIMIENTO
            │
            ▼
         PRODUCTO
            │
            ▼
         CARRITO
            │
            ▼
          ORDEN
            │
      ┌─────┼─────┐
      ▼     ▼     ▼
SUBPEDIDO SUBPEDIDO SUBPEDIDO
   │         │         │
   ▼         ▼         ▼
EMPREND.  EMPREND.  EMPREND.
   │
   ├── DETALLE
   ├── PAGO
   └── ENTREGA
```

---

# 33. Arquitectura general

```text
                VALLE-GO

               Android App
                    │
                 HTTPS
                    │
                    ▼
                 API REST
                    │
        ┌───────────┼────────────┐
        │           │            │
        ▼           ▼            ▼
   Autenticación   Pedidos   Notificaciones
        │           │
        └──────┬────┘
               ▼
          PostgreSQL
               │
               ▼
              ETL
               │
               ▼
          Data Mart
               │
               ▼
           Dashboard
```

---

# 34. Arquitectura Android

Tecnologías recomendadas:

```text
Kotlin

Jetpack Compose

MVVM

Clean Architecture

Coroutines

Flow

Retrofit o Ktor Client

Room

Hilt
```

Estructura conceptual:

```text
app
│
├── presentation
│   ├── auth
│   ├── home
│   ├── products
│   ├── cart
│   ├── checkout
│   ├── orders
│   ├── entrepreneur
│   └── admin
│
├── domain
│   ├── model
│   ├── repository
│   └── usecase
│
├── data
│   ├── remote
│   ├── local
│   ├── repository
│   └── mapper
│
└── core
    ├── network
    ├── security
    ├── notifications
    └── utils
```

---

# 35. Backend

Posibles alternativas:

```text
Android Kotlin
      ↓
Spring Boot
      ↓
PostgreSQL
```

o:

```text
Android Kotlin
      ↓
Node.js + TypeScript
      ↓
PostgreSQL
```

Una opción muy completa para aprendizaje sería:

```text
Kotlin + Jetpack Compose
Spring Boot
PostgreSQL
```

---

# 36. API REST

Conceptualmente podrían existir módulos como:

```text
/auth

/users

/campuses

/entrepreneurs

/businesses

/products

/categories

/cart

/orders

/suborders

/payments

/meeting-points

/notifications

/reviews

/reports
```

La definición exacta de endpoints deberá realizarse después de cerrar los requisitos funcionales y reglas de negocio.

---

# 37. Diagramas necesarios

Para documentar Valle-Go se recomienda desarrollar los siguientes diagramas.

## Diagrama de contexto

Mostrará:

```text
Comprador
    │
    ▼
 VALLE-GO
    ▲
    │
Emprendedor

Administrador
    │
    ▼
 VALLE-GO
```

## Diagrama de casos de uso

Se realizará uno general y, si es necesario, uno por rol.

## Diagrama de actividades

Ejemplos:

- realizar compra;
- aceptar pedido;
- preparar pedido;
- cancelar;
- completar entrega;
- registrar pago.

## Diagrama de secuencia

Ejemplos:

- creación de orden;
- generación de subpedidos;
- aceptación del vendedor;
- confirmación de pago.

## Diagrama de estados

Especialmente importante para:

- orden;
- subpedido;
- pago;
- emprendimiento.

## Diagrama entidad-relación

Representará las relaciones de datos.

## Diagrama de componentes

Representará:

```text
Android
API
Base de datos
Notificaciones
ETL
Dashboard
```

---

# 38. Diagrama de secuencia simplificado de compra

```text
Comprador
    │
    │ Confirmar carrito
    ▼
Android
    │
    │ POST /orders
    ▼
Backend
    │
    │ Crear orden
    ▼
Base de datos
    │
    │ Crear subpedidos
    ▼
Backend
    │
    ├──────── Notificar vendedor A
    ├──────── Notificar vendedor B
    └──────── Notificar vendedor C
```

Cada vendedor responderá independientemente.

---

# 39. Diagrama de estados del subpedido

```text
PENDIENTE
    │
    ├──────────► RECHAZADO
    │
    ▼
ACEPTADO
    │
    ▼
EN_PREPARACION
    │
    ▼
LISTO
    │
    ▼
ESPERANDO_ENTREGA
    │
    ▼
PAGO_CONFIRMADO
    │
    ▼
COMPLETADO
```

Estados alternativos:

```text
CANCELADO

NO_ENTREGADO
```

---

# 40. ETL en Valle-Go

ETL no será necesario para ejecutar la compra.

Será utilizado para análisis.

Significa:

```text
Extract
Transform
Load
```

Flujo:

```text
PostgreSQL
    │
    │ Extract
    ▼
Pedidos, usuarios,
productos y ventas
    │
    │ Transform
    ▼
Limpiar
Agrupar
Calcular
    │
    │ Load
    ▼
Data Mart
    │
    ▼
Dashboard
```

---

# 41. Información que puede analizar el ETL

## Ventas registradas

```text
Ventas por día

Ventas por semana

Ventas por mes
```

## Horarios

```text
11:00 AM → 10 pedidos
12:00 PM → 35 pedidos
1:00 PM  → 56 pedidos
2:00 PM  → 30 pedidos
```

## Puntos de encuentro

```text
Biblioteca        120 entregas

Patio central      95

Pabellón A         60

Cafetería          40
```

## Productos

```text
Brownie          350 ventas

Hamburguesa      300 ventas

Gaseosa          280 ventas
```

## Emprendimientos

Se podrá analizar:

- volumen de ventas;
- pedidos aceptados;
- pedidos rechazados;
- ticket promedio;
- productos más vendidos;
- horarios de mayor demanda.

---

# 42. Dashboard del administrador

Ejemplo:

```text
VALLE-GO ADMIN

Usuarios:
1,280

Emprendedores:
42

Órdenes hoy:
315

Subpedidos:
520

Completados:
470

Cancelados:
20

No entregados:
12

Ventas registradas:
S/ 4,850
```

El concepto será **ventas registradas**, ya que Valle-Go no procesa directamente el dinero.

---

# 43. Dashboard del emprendedor

El vendedor podría visualizar:

```text
Ventas de hoy

Pedidos pendientes

Pedidos completados

Ingresos registrados

Producto más vendido

Calificación

Pedidos por horario
```

---

# 44. MVP de Valle-Go

La primera versión funcional debería incluir:

## Autenticación

- registro;
- login;
- roles.

## Comprador

- visualizar productos;
- carrito multiemprendimiento;
- checkout;
- punto;
- horario;
- crear orden;
- visualizar orden;
- visualizar subpedidos.

## Emprendedor

- productos;
- stock;
- pedidos;
- aceptar;
- rechazar;
- preparar;
- marcar listo;
- confirmar pago y entrega.

## Administrador

- usuarios;
- emprendedores;
- productos;
- categorías;
- puntos de encuentro;
- pedidos.

---

# 45. Valle-Go V2

Agregar:

```text
Notificaciones push

Favoritos

Promociones

Calificaciones

Reclamos

Incidencias

Inventario avanzado

Horarios avanzados

Disponibilidad del vendedor
```

---

# 46. Valle-Go V3

Agregar:

```text
QR de entrega

ETL

Dashboard

Analítica

Estadísticas avanzadas
```

El QR podría funcionar así:

```text
Comprador llega
       ↓
Muestra QR
       ↓
Vendedor escanea
       ↓
Sistema identifica subpedido
       ↓
Comprador paga
       ↓
Vendedor confirma
```

---

# 47. Valle-Go V4

Preparar el sistema para:

```text
Universidad
    │
    ├── Campus A
    ├── Campus B
    └── Campus C
```

Por eso, aunque inicialmente exista un solo campus, es recomendable modelar desde el inicio la entidad:

```text
CAMPUS
```

Y relacionarla con:

```text
usuarios

emprendimientos

puntos de encuentro

órdenes
```

---

# 48. Reglas de negocio iniciales

1. Un usuario debe autenticarse para comprar o vender.
2. Todo usuario tendrá uno o más roles autorizados.
3. Un emprendimiento debe ser aprobado por el administrador.
4. Solo emprendimientos aprobados pueden publicar productos.
5. Un producto debe pertenecer a un emprendimiento.
6. El comprador puede agregar productos de diferentes emprendimientos.
7. El carrito debe agrupar los productos por emprendimiento.
8. Al confirmar el carrito se crea una orden general.
9. La orden se divide automáticamente en subpedidos.
10. Cada emprendimiento recibe solamente su subpedido.
11. Cada vendedor acepta o rechaza independientemente.
12. El rechazo de un subpedido no necesariamente cancela toda la orden.
13. El sistema recalcula el total cuando un subpedido es rechazado o cancelado.
14. Cada subpedido posee su propio estado.
15. La orden posee un estado general calculado.
16. Los productos deben tener stock disponible.
17. El stock solicitado debe reservarse.
18. El stock debe liberarse si el pedido es rechazado o cancelado.
19. El comprador debe seleccionar punto de encuentro.
20. El comprador debe seleccionar un horario.
21. Los puntos deben ser administrados por Valle-Go.
22. El pago se realiza contra entrega.
23. Valle-Go no procesa el dinero.
24. El pago se gestiona independientemente por subpedido.
25. El vendedor confirma la recepción del dinero.
26. Un subpedido no puede completarse sin confirmación de pago.
27. El comprador podrá reportar que un vendedor no se presentó.
28. El vendedor podrá reportar que el comprador no se presentó.
29. Los cambios importantes deberán generar notificaciones.
30. Las operaciones importantes deberán quedar registradas para auditoría.

---

# 49. Orden recomendado para desarrollar el proyecto

Antes de programar, se recomienda trabajar en el siguiente orden:

```text
01. Descripción

02. Problemática

03. Objetivos

04. Alcance

05. Actores

06. Requisitos funcionales

07. Requisitos no funcionales

08. Reglas de negocio

09. Casos de uso

10. Diagrama de contexto

11. Casos de uso UML

12. Diagramas de actividades

13. Diagramas de secuencia

14. Diagramas de estados

15. Modelo conceptual

16. Diagrama entidad-relación

17. Diseño de base de datos

18. Arquitectura

19. API REST

20. Arquitectura Android

21. Seguridad

22. Notificaciones

23. ETL

24. Dashboard

25. Pruebas

26. Roadmap
```

---

# 50. Idea central definitiva de Valle-Go

La característica diferencial del sistema será:

> Un estudiante podrá realizar una compra con productos de múltiples emprendimientos desde un mismo carrito y coordinar la entrega dentro del campus mediante un punto y horario de encuentro. Valle-Go organizará internamente la compra en subpedidos independientes para que cada emprendedor pueda aceptar, preparar, cobrar y confirmar únicamente los productos que le corresponden.

Esto permite ofrecer al comprador una experiencia sencilla sin perder el control individual de cada vendedor.

La arquitectura queda además preparada para agregar posteriormente:

- múltiples campus;
- notificaciones push;
- QR de entrega;
- promociones;
- reputación;
- recomendaciones;
- analítica;
- ETL;
- dashboards;
- predicción de demanda;
- programas de puntos;
- herramientas de administración avanzada.
