package com.proelectricos.restintce.service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;

/**
 * Servicio dedicado a operaciones con Google Drive:
 * creación de carpetas y subida de archivos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDriveService {

    private final Drive driveClient;

    private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
    private static final String EXCEL_MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * Crea una carpeta dentro de una carpeta padre en Google Drive.
     *
     * @param folderName     nombre de la carpeta a crear
     * @param parentFolderId ID de la carpeta padre en Drive
     * @return ID de la carpeta creada
     */
    public String createFolder(String folderName, String parentFolderId) throws IOException {
        try {
            File folderMetadata = new File()
                    .setName(folderName)
                    .setMimeType(FOLDER_MIME_TYPE)
                    .setParents(Collections.singletonList(parentFolderId));

            File created = driveClient.files().create(folderMetadata)
                    .setSupportsAllDrives(true)
                    .setFields("id")
                    .execute();

            log.info("Carpeta creada en Drive: '{}' (ID: {})", folderName, created.getId());
            return created.getId();

        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                log.error("Carpeta padre no encontrada (ID: {}). Verifica que la carpeta exista y esté compartida con la cuenta de servicio.", parentFolderId);
                throw new IOException("La carpeta raíz de Drive (ID: " + parentFolderId + ") no fue encontrada. "
                        + "Comparte la carpeta con la cuenta de servicio en Google Drive.", e);
            }
            throw e;
        }
    }

    /**
     * Sube un archivo Excel a una carpeta específica de Google Drive.
     *
     * @param localFilePath ruta local del archivo a subir
     * @param fileName      nombre con el que se guardará en Drive
     * @param folderId      ID de la carpeta destino en Drive
     * @return webViewLink del archivo subido
     */
    public String uploadFile(String localFilePath, String fileName, String folderId) throws IOException {
        java.io.File localFile = new java.io.File(localFilePath);
        if (!localFile.exists()) {
            throw new IOException("El archivo local no existe: " + localFilePath);
        }

        File fileMetadata = new File()
                .setName(fileName)
                .setParents(Collections.singletonList(folderId));

        FileContent mediaContent = new FileContent(EXCEL_MIME_TYPE, localFile);

        File uploaded = driveClient.files().create(fileMetadata, mediaContent)
                .setSupportsAllDrives(true)
                .setFields("id, webViewLink")
                .execute();

        log.info("Archivo subido a Drive: '{}' -> {}", fileName, uploaded.getWebViewLink());
        return uploaded.getWebViewLink();
    }
}
