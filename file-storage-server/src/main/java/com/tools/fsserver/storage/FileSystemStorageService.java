package com.tools.fsserver.storage;

import com.tools.fsserver.exception.FileNameNotPresentOnServerException;
import com.tools.fsserver.exception.FileNamePresentOnServerException;
import io.quarkus.runtime.Startup;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Startup
@ApplicationScoped
public class FileSystemStorageService implements IStorageService {

  private static final Logger LOG = Logger.getLogger(FileSystemStorageService.class);
  private final Path permanentStoragePath;

  /**
   * @param permanentStoragePath - Relative path to the folder containing all uploaded files. It is
   *     read from the application.properties file, particularly from the fsserver.uploadedFilesPath
   *     property. At server startup this folder is created if not already existing
   * @throws IOException - if any I/O issues when checking existence of storage path or when
   *     creating it
   */
  @Inject
  public FileSystemStorageService(
      @ConfigProperty(name = "fsserver.uploadedFilesPath") String permanentStoragePath)
      throws IOException {
    Path pathToStorage = Paths.get(permanentStoragePath);
    this.permanentStoragePath =
        Files.notExists(pathToStorage) ? Files.createDirectories(pathToStorage) : pathToStorage;
    LOG.info("FSServer permanent storage path is at " + this.permanentStoragePath.toAbsolutePath());
  }

  /**
   * @return Returns a set of file names from the storage path
   * @throws IOException If any I/O issue occurs
   */
  public Set<String> listStoredFiles() throws IOException {
    try (Stream<Path> stream = Files.list(this.permanentStoragePath)) {
      Set<String> uploadedFileNames =
          stream
              .filter(file -> !Files.isDirectory(file))
              .map(java.nio.file.Path::getFileName)
              .map(java.nio.file.Path::toString)
              .collect(Collectors.toSet());
      LOG.debug("Returning list of uploaded files: " + uploadedFileNames);
      return uploadedFileNames;
    }
  }

  /**
   * @param fileName - the final name of the uploaded file
   * @param uploadSourcePath - the full path to the temp location where the multipart file has been
   *     uploaded. It is usually uploaded under a quarkus-generated filename in the temp folder, so
   *     we will need to copy it to the FSServer's main storage path under the provided fileName
   * @throws FileNamePresentOnServerException - thrown if this file has already been uploaded
   * @throws IOException - thrown if any I/O issue occurs
   */
  public void storeFile(String fileName, Path uploadSourcePath)
      throws FileNamePresentOnServerException, IOException {
    java.nio.file.Path destinationPath = Paths.get(this.permanentStoragePath.toString(), fileName);
    LOG.debug(
        "Copying file from source path "
            + uploadSourcePath.toAbsolutePath()
            + " to destination path "
            + destinationPath.toAbsolutePath());
    Path uploadedFinalPath;
    try {
      uploadedFinalPath = Files.copy(uploadSourcePath, destinationPath);
    } catch (FileAlreadyExistsException faex) {
      String errMsg = "There already exists a file called " + destinationPath.getFileName();
      LOG.error(errMsg);
      throw new FileNamePresentOnServerException(errMsg);
    }
    LOG.debug("Uploaded file at path " + uploadedFinalPath.toAbsolutePath());
  }

  /**
   * @param fileNameToDelete - the name of the previously-uploaded file which we want to delete.
   *     This parameter should not be a path, just a file name
   * @throws FileNameNotPresentOnServerException - thrown if we want to delete a file which does not
   *     exist on the server
   * @throws IOException - thrown if any I/O issue occurs
   */
  public void deleteFile(String fileNameToDelete)
      throws FileNameNotPresentOnServerException, IOException {
    java.nio.file.Path pathToFile =
        Paths.get(this.permanentStoragePath.toString(), fileNameToDelete);
    LOG.debug("Attempting to delete uploaded file at location " + pathToFile);
    boolean deleted = Files.deleteIfExists(pathToFile);
    if (deleted) {
      LOG.debug("Successfully deleted file at location " + pathToFile);
    } else {
      String errMsg = "There is no already uploaded file called " + fileNameToDelete;
      LOG.error(errMsg);
      throw new FileNameNotPresentOnServerException(errMsg);
    }
  }
}
