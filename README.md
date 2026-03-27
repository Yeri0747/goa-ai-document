## [Máster en Ingeniería Web por la Universidad Politécnica de Madrid (miw-upm)](http://miw.etsisi.upm.es)

## Back-end con Tecnologías de Código Abierto (BETCA).

### Estado del código
[![CI goa-ai-document](https://github.com/miw-upm/goa-document/actions/workflows/ci.yml/badge.svg)](https://github.com/miw-upm/goa-document/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=miw-upm-github_goa-document&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=miw-upm-github_goa-document)
[![AWS broken](https://gestion.ocanabogados.es/api/goa-document/system/version-badge)](https://gestion.ocanabogados.es/api/goa-document/system)


### Tecnologías necesarias

`Java` `Maven` `GitHub` `GitHub Actions` `Spring-Boot` `GitHub Packages` `Docker` `OpenAPI`

### :gear: Instalación del proyecto

1. Clonar el repositorio en tu equipo, **mediante consola**:

```sh
> cd <folder path>
> git clone https://github.com/Yeri0747/goa-ai-document
```

2. Importar el proyecto mediante **IntelliJ IDEA**
    * **Open**, y seleccionar la carpeta del proyecto.

### :gear: Ejecución en local con IntelliJ

* Ejecutar la clase **Application** con IntelliJ

### :gear: Ejecución en local con Docker
* Crear la red, solo una vez:

```sh
> docker network create goa
```

* Ejecutar en el proyecto la siguiente secuencia de comandos de Docker:

```sh
> docker compose up --build -d
```

* Cliente Web: `http://localhost:8091/swagger-ui.html`
