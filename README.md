# Pan, Puntos y Premios

Aplicación de escritorio desarrollada en Java Swing y Oracle Database para administrar clientes, productos, inventario, pedidos, pagos, canjes y reportes.

## Requisitos

- JDK 21.
- NetBeans con soporte para proyectos Java Ant.
- Oracle Database con el servicio `FREEPDB1` en `127.0.0.1:1521`.
- El esquema de Oracle `PAN_PREMIOS`.
- El controlador JDBC de Oracle `ojdbc17.jar`, configurado en las bibliotecas del proyecto.

Si NetBeans indica que no encuentra `ojdbc17.jar`, hay que volver a agregar esa biblioteca desde la computadora donde se abra el proyecto.

## Preparar la base de datos

1. Conéctate en Oracle al esquema `PAN_PREMIOS`.
2. Para una instalación nueva, ejecuta `base_datos.sql` una sola vez. El script crea las tablas e inserta datos de prueba.
3. `migracion_estado_pedidos.sql` sirve para actualizar una base de datos existente; no es el script de instalación inicial.

## Abrir y ejecutar

1. Abre la carpeta del proyecto como un proyecto existente en NetBeans.
2. Comprueba que use JDK 21 y que la biblioteca `ojdbc17.jar` esté disponible.
3. Ejecuta el proyecto. La clase principal es `panpuntospremios.Principal`.
4. Cuando la aplicación solicite la clave de Oracle, escribe la clave configurada para el esquema `PAN_PREMIOS`.

La clave de Oracle se ingresa al iniciar la aplicación. No la escribas en este archivo.

## Diagramas

La carpeta `documentacion` contiene el diagrama de clases UML y el diagrama entidad-relación.
