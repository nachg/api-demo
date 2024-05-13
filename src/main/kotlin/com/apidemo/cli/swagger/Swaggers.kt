package com.apidemo.cli.swagger

object Swaggers {
    val realWorld = SwaggerCheck(
        config = SwaggerConfig(
            stand = "localhost",
            name = "RealWorld App",
            url = "http://localhost:8080/api/v3/api-docs",
            replaceConfigs = listOf(
                ReplaceConfig(
                    replaceUrl = "http://localhost:8080/api",
                )
            ),

            //игнорируем другие системы
            ignoreConfig = IgnoreConfig(
                ignorePatters = listOf(
                    "/graphql",
                    "/schema.json",
                )
            )
        )
    )
}