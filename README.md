# Keycloak Email Link Authenticator

This repository contains source code for Keycloak plugin developed in my tutorial - https://dev.to/yakovlev_alexey/how-to-create-a-keycloak-plugin-3acj.

1. To build the plugin run `mvn clean package`.
2. Move or copy newly built plugins into `docker/plugins` folder: `rm docker/plugins/*.jar && mv target/*.jar docker/plugins/`.
3. Run docker-compose: `docker-compose up --build keycloak mailhog`.
4. Visit http://localhost:8024 in your browser to enter Keycloak and configure the plugin. Read about configuration in the article.
5. Visit http://localhost:8024/realms/master/acoount/ to see plugin in action.

You may this plugin in your projects freely. See [MIT license](/LICENSE).
