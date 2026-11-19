# Aplicación Java Spring Boot

Aplicación web REST desarrollada con Java 17 y Spring Boot 3.2.0. El proyecto sirve como aplicación de prueba para los flujos de construcción de imágenes Docker, publicación en Docker Hub y despliegue en Azure Container Apps desde el repositorio de infraestructura.

Además de los endpoints de saludo, el código incluye pruebas de lectura de variables de entorno, consulta de Azure SQL Database y entrega de una imagen JPEG desde el sistema de archivos del contenedor.

## 1. Arquitectura y flujo de entrega

Este repositorio contiene el código fuente de la aplicación. La infraestructura y el pipeline Jenkins que construye y publica la imagen están definidos en [projectdevopsterraform](https://github.com/Javierfloresr31/projectdevopsterraform).

```text
Repositorio Java (main)
        |
        | checkout del código
        v
Jenkins (en VM Ubuntu / Docker)
        |
        | docker build
        v
Imagen Docker
javierfloresr31/myapp:<SHA>
        |
        | docker push
        v
Docker Hub
        |
        | Terraform consulta la etiqueta más reciente
        v
Azure Container Apps
        |
        | inicia el contenedor
        v
Aplicación Spring Boot :8080
        |
        +---------------------+---------------------+
        |                     |                     |
        v                     v                     v
   Variables de          Azure SQL             Archivo JPEG
   entorno               Database              /mnt/asus.jpg
```

### Flujo actual entre repositorios

1. El código Java se mantiene en la rama `main` de este repositorio.
2. El Jenkinsfile del repositorio de infraestructura clona este repositorio.
3. Jenkins obtiene el SHA del commit, construye la imagen con la etiqueta de ese SHA y la publica en Docker Hub bajo `javierfloresr31/myapp`.
4. Después de publicar la imagen, se ejecuta Terraform de forma independiente desde el repositorio de infraestructura.
5. La configuración Terraform consulta Docker Hub y selecciona la etiqueta con la fecha de actualización más reciente para configurar Azure Container Apps.

**Límite del pipeline actual:** Jenkins construye y publica la imagen, pero no ejecuta `terraform apply` ni despliega directamente en Azure. La actualización de Container Apps requiere el paso Terraform independiente.

## 2. Tecnologías

| Tecnología | Versión / uso |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.0 |
| Maven | 3.9 (imagen de build Docker) |
| Spring Boot Web | Controladores REST |
| Microsoft SQL Server JDBC | Conectividad con Azure SQL |
| Docker | Construcción y ejecución en contenedores |
| Eclipse Temurin | JDK/JRE 17 en las imágenes Docker |
| Azure SQL Database | Base de datos consultada por el endpoint `/basedatos` |
| Azure Container Apps | Entorno de ejecución previsto para la imagen |

## 3. Endpoints REST

La clase `HolaMundoController` define los siguientes endpoints:

| Método | Ruta | Comportamiento |
|---|---|---|
| GET | `/` | Devuelve el texto de prueba `¡Hola haciendo prueba de Jenkins`. |
| GET | `/saludo` | Devuelve un saludo que incorpora el valor de la variable de entorno `usuariodev`. |
| GET | `/secreto` | Devuelve el valor de la variable de entorno `secretodev`; si no está definida, informa que falta. |
| GET | `/imagen` | Lee y devuelve el archivo JPEG ubicado en `/mnt/asus.jpg`. Devuelve HTTP 404 si el archivo no existe. |
| GET | `/basedatos` | Se conecta a Azure SQL y devuelve las filas de `dbo.Users`. |

La aplicación escucha en el puerto **8080** dentro del contenedor.

### Ejemplos locales

```bash
curl http://localhost:8080/
curl http://localhost:8080/saludo
curl http://localhost:8080/secreto
curl http://localhost:8080/basedatos
curl -i http://localhost:8080/imagen
```

El endpoint `/secreto` devuelve el valor recibido en la variable de entorno. No debe utilizarse para exponer secretos reales en un entorno accesible públicamente. Para una aplicación productiva, los secretos no deben devolverse en una respuesta HTTP.

## 4. Configuración de la aplicación

El archivo `src/main/resources/application.properties` establece:

- Puerto HTTP: `8080`.
- Nombre Spring Boot: `mi-app-java`.
- Servidor Azure SQL: `sql-terraformcloud-dev-centralus-001.database.windows.net`.
- Base de datos: `sqldb-terraformcloud-dev-centralus-001`.

El usuario y la contraseña SQL se leen desde variables de entorno:

| Variable | Uso |
|---|---|
| `USER` | Usuario de conexión a Azure SQL. |
| `PASSWORD` | Contraseña del usuario SQL. |
| `usuariodev` | Valor mostrado por el endpoint `/saludo`. |
| `secretodev` | Valor mostrado por el endpoint `/secreto`. |

El endpoint `/basedatos` construye una conexión JDBC cifrada hacia Azure SQL (puerto 1433) y ejecuta:

```sql
SELECT * FROM dbo.Users;
```

La respuesta se construye como una lista de filas cuyos valores se obtienen dinámicamente a partir de los metadatos del ResultSet. Si las variables `USER` o `PASSWORD` no están definidas, el endpoint devuelve un mensaje indicando cuál falta.

**Configuración externa:** este repositorio contiene el servidor y la base de datos SQL de destino en `application.properties`, mientras que las credenciales se esperan desde el entorno de ejecución. La configuración actual de Terraform para Container Apps debe completarse para inyectar estas variables y referencias a secretos de forma segura. No se deben incluir credenciales reales en el código ni en el repositorio.

## 5. Almacenamiento de imágenes

El endpoint `GET /imagen` busca el archivo en la ruta absoluta:

```text
/mnt/asus.jpg
```

El archivo no está incluido en el árbol actual del repositorio ni se copia en el Dockerfile. Por tanto, el endpoint responderá con HTTP 404 mientras el archivo no exista en esa ruta dentro del contenedor.

Para habilitar el almacenamiento persistente en Azure Container Apps se necesita montar un volumen en una ruta accesible por la aplicación (por ejemplo, `/mnt`) y asegurar que el archivo `asus.jpg` esté disponible en el recurso compartido. En el repositorio de infraestructura ya se declaran una Storage Account y un Azure Files Share, pero la integración/montaje en Container Apps todavía debe configurarse. La persistencia no queda habilitada únicamente por crear esos recursos.

## 6. Estructura del repositorio

```text
projectdevopsjava/
├── .dockerignore
├── .gitignore
├── .gitlab-ci.yml
├── Dockerfile
├── README.md
├── comandos.md
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/example/
        │       ├── MiAppApplication.java
        │       └── controller/
        │           └── HolaMundoController.java
        └── resources/
            └── application.properties
```

| Archivo | Responsabilidad |
|---|---|
| `pom.xml` | Dependencias Maven, versión Java y plugin de Spring Boot. |
| `Dockerfile` | Construcción multi-stage de la aplicación y creación de la imagen runtime. |
| `MiAppApplication.java` | Punto de entrada de Spring Boot. |
| `HolaMundoController.java` | Endpoints REST de saludo, variables de entorno, imagen y consulta SQL. |
| `application.properties` | Puerto, nombre de aplicación y datos no secretos del servidor/base SQL. |
| `.gitlab-ci.yml` | Pipeline alternativo de GitLab CI para build y ejecución local de contenedor. |
| `comandos.md` | Comandos manuales para build, etiquetado, publicación y operación Docker. |
| `.dockerignore` | Archivos y directorios excluidos del contexto Docker. |
| `.gitignore` | Exclusiones de Maven, IDE, logs y archivos temporales. |

## 7. Construcción y ejecución local

### Requisitos previos

- JDK 17.
- Maven 3.x si se desea compilar fuera de Docker.
- Docker Engine o Docker Desktop, para la ejecución containerizada.

### Ejecutar con Maven

Desde la raíz del proyecto:

```bash
mvn clean package -DskipTests
java -jar target/mi-app-java-1.0.0.jar
```

La aplicación estará disponible en:

```text
http://localhost:8080
```

### Ejecutar con Docker

El Dockerfile utiliza una construcción multi-stage:

1. Etapa de build basada en `maven:3.9-eclipse-temurin-17`.
2. Descarga de dependencias con `mvn dependency:go-offline`.
3. Compilación del proyecto mediante `mvn clean package -DskipTests`.
4. Imagen final basada en `eclipse-temurin:17-jre`, que contiene únicamente el JAR ejecutable.

Construir la imagen:

```bash
docker build -t mi-app-java:latest .
```

Ejecutar el contenedor:

```bash
docker run -d \
  --name mi-app \
  -p 8080:8080 \
  mi-app-java:latest
```

Verificar:

```bash
curl http://localhost:8080/
docker logs -f mi-app
```

Detener y eliminar:

```bash
docker stop mi-app
docker rm mi-app
```

### Variables de entorno para pruebas

Ejemplo de ejecución con variables de entorno (utiliza valores de prueba, nunca credenciales reales):

```bash
docker run -d \
  --name mi-app \
  -p 8080:8080 \
  -e usuariodev=usuario-prueba \
  -e secretodev=valor-no-productivo \
  -e USER=usuario_sql \
  -e PASSWORD='contraseña-de-prueba' \
  -v "$(pwd)/uploads:/mnt" \
  mi-app-java:latest
```

El montaje local `uploads:/mnt` es únicamente para pruebas en Docker. En Azure Container Apps se requiere configurar el volumen correspondiente en el entorno de Azure.

## 8. Publicación manual en Docker Hub

El repositorio incluye `comandos.md` con ejemplos de comandos Docker. Un flujo básico de publicación es:

```bash
docker build -t mi-app-java:latest .
docker login
docker tag mi-app-java:latest javierfloresr31/myapp:latest
docker push javierfloresr31/myapp:latest
```

En el flujo automatizado de Jenkins, la etiqueta utilizada es el SHA del commit y el repositorio de destino es `javierfloresr31/myapp`. Para reproducir exactamente ese flujo manualmente:

```bash
COMMIT=$(git rev-parse HEAD)
docker build -t "javierfloresr31/myapp:$COMMIT" .
docker push "javierfloresr31/myapp:$COMMIT"
```

Para publicar en Docker Hub, autentícate mediante `docker login` o utiliza credenciales gestionadas por el sistema de CI. No guardes tokens en scripts ni en el repositorio.

## 9. GitLab CI

El archivo `.gitlab-ci.yml` define dos etapas:

1. `build_image`: construye la imagen local `mi-app-java:latest`.
2. `deploy_container`: detiene y elimina el contenedor previo `mi-app` y levanta uno nuevo con el mapeo `8081:8080`.

Este pipeline necesita un GitLab Runner con la etiqueta `test` y acceso al daemon Docker. No publica la imagen en Docker Hub y el despliegue se limita al host del Runner. Es un flujo alternativo del repositorio, distinto del Jenkins utilizado en el flujo Azure descrito en la sección 1.

## 10. Consideraciones técnicas y pendientes

- **Pruebas:** el Dockerfile compila con `-DskipTests`; no se observa una suite de pruebas automatizadas en el árbol actual.
- **Seguridad de secretos:** `/secreto` devuelve directamente el valor de `secretodev`. No debe exponerse en un ambiente público ni utilizarse para consultar secretos productivos.
- **Conexión SQL:** el endpoint usa JDBC directamente mediante `DriverManager`. Deben configurarse `USER` y `PASSWORD` en el entorno de ejecución y confirmar que el firewall/red del servidor SQL permite la conexión desde el servicio.
- **Permisos SQL:** la cuenta de aplicación debe tener únicamente los permisos que necesita para consultar `dbo.Users`.
- **Imagen:** el archivo `/mnt/asus.jpg` debe proveerse mediante un volumen o mecanismo de almacenamiento; no se empaqueta dentro de la imagen actual.
- **Persistencia Azure:** Azure Files está declarado en el repositorio Terraform, pero el montaje sobre Container Apps está pendiente.
- **Configuración Azure:** las referencias a secretos y las variables de entorno de la aplicación deben configurarse en el recurso Container App. La definición actual no las declara aún.
- **Errores SQL:** el controlador devuelve el mensaje de excepción SQL en la respuesta HTTP. Para un entorno productivo se debe registrar el detalle en logs internos y devolver un mensaje genérico al cliente.
- **Etiqueta Docker:** para reproducibilidad se recomienda desplegar por SHA de commit o digest, evitando depender únicamente de `latest`.

## 11. Repositorio de infraestructura

La infraestructura y el flujo de build/push utilizado por Jenkins se documentan aquí:

[projectdevopsterraform](https://github.com/Javierfloresr31/projectdevopsterraform)

Ese repositorio contiene la configuración Terraform de Azure, Azure Container Apps, Azure SQL, Key Vault, Managed Identity, Storage Account y Azure Files, así como el Jenkinsfile que clona este proyecto y publica la imagen.

---

**Proyecto de práctica:** aplicación Java Spring Boot utilizada para validar construcción de contenedores, variables de entorno, conexión a Azure SQL, entrega de archivos y despliegue en Azure Container Apps.
