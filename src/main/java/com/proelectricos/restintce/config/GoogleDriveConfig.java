package com.proelectricos.restintce.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

@Configuration
public class GoogleDriveConfig {

    @Value("${google.drive.credentials-path}")
    private String credentialsPath;

    @Value("${google.drive.application-name}")
    private String applicationName;

    @Bean
    public Drive driveClient() throws IOException {
        Path path = Paths.get(credentialsPath);

        GoogleCredentials credentials;
        try (InputStream is = Files.newInputStream(path)) {
            credentials = GoogleCredentials.fromStream(is)
                    .createScoped(Collections.singletonList(DriveScopes.DRIVE));
        }

        return new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(applicationName)
                .build();
    }
}
